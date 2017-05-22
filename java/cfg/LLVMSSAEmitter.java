package cfg;

import ast.*;
import cfg.llvm.*;
import constprop.Bottom;
import constprop.ConstImm;
import constprop.ConstValue;
import constprop.Top;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class LLVMSSAEmitter {
    public static void writeLlvm(Program prog, String filename, boolean sccp, boolean uce) {
        try (PrintWriter pw = new PrintWriter(new File(filename))) {
            writeConstants(pw);
            writeStructs(prog.getTypes(), pw);
            writeGlobals(prog.getDecls(), pw);
            for (Function f : prog.getFuncs()) {
                writeFunction(f,
                              pw,
                              createSymbolTable(prog),
                              createStructTable(prog.getTypes()),
                              createArgTypesByFun(prog.getFuncs()),
                              sccp,
                              uce);
            }
        } catch (FileNotFoundException e) {
            System.err.println(String.format("Could not find/open file %s.", filename));
            System.exit(1);
        }
    }

    private static void writeConstants(PrintWriter pw) {
        pw.println("target triple=\"x86_64\"");
        pw.println("declare i8* @malloc(i64)");
        pw.println("declare void @free(i8*)");
        pw.println("declare i32 @printf(i8*, ...)");
        pw.println("declare i32 @scanf(i8*, ...)");
        pw.println("@.println = private unnamed_addr constant [5 x i8] c\"%ld\\0A\\00\",align 1");
        pw.println("@.print = private unnamed_addr constant [5 x i8] c\"%ld \\00\",align 1");
        pw.println("@.read = private unnamed_addr constant [4 x i8] c\"%ld\\00\",align 1");
        pw.println("@.read_scratch = common global i64 0, align 8\n");
    }

    private static void writeStructs(List<TypeDeclaration> structs, PrintWriter pw) {
        for (TypeDeclaration td : structs) {
            pw.print(String.format("%%struct.%s = type {", td.getName()));
            List<Declaration> fields = td.getFields();
            for (int i = 0; i < td.getFields().size(); ++i) {
                if (fields.get(i).getType() instanceof IntType || fields.get(i).getType() instanceof BoolType) {
                    pw.print("i64");
                } else if (fields.get(i).getType() instanceof StructType) {
                    pw.print(String.format("%%struct.%s*", ((StructType) fields.get(i).getType()).getName()));
                } else {
                    System.err.println(String.format("Unexpected field declaration: %S", fields.get(i).toString()));
                }

                if (i != fields.size() - 1) {
                    pw.print(", ");
                }
            }
            pw.println("}");
        }
        pw.println();
    }

    private static void writeGlobals(List<Declaration> globals, PrintWriter pw) {
        for (Declaration d : globals) {
            if (d.getType() instanceof IntType || d.getType() instanceof BoolType) {
                pw.println(String.format("@%s = common global i64 0, align 8", d.getName()));
            } else if (d.getType() instanceof StructType) {
                pw.println(String.format("@%s = common global %s null, align 8",
                                         d.getName(), d.getType().toLlvmType()));
            } else {
                System.err.println(String.format("Unexpected field declaration: %S", d.toString()));
            }
        }
        pw.println();
    }


    private static void writeFunction(Function f,
                                      PrintWriter pw,
                                      Map<String, Pair<Type, VariableScope>> symbolTable,
                                      Map<String, Map<String, Pair<Integer, Type>>> structTable,
                                      Map<String, List<Type>> argTypesByFun,
                                      boolean sccp,
                                      boolean uce) {
        BasicBlock startBlock = new BasicBlock();
        startBlock.seal();

        // Write function header
        pw.print(String.format("define %s @%s(", f.getRetType().toLlvmType(), f.getName()));
        for (int i = 0; i < f.getParams().size(); ++i) {
            symbolTable.put(f.getParams().get(i).getName(),
                            new Pair<>(f.getParams().get(i).getType(), VariableScope.PARAM));
            pw.print(String.format("%s %%%s",
                                   f.getParams().get(i).getType().toLlvmType(), f.getParams().get(i).getName()));
            startBlock.writeVariable(f.getParams().get(i).getName(),
                                     new LLVMRegister(f.getParams().get(i).getType(),
                                                      String.format("%%%s", f.getParams().get(i).getName())));
            if (i != f.getParams().size() - 1) {
                pw.print(", ");
            }
        }
        pw.println(") {");

        // Write locals
        for (Declaration local : f.getLocals()) {
            symbolTable.put(local.getName(), new Pair<>(local.getType(), VariableScope.LOCAL));
        }

        BasicBlock returnBlock = new BasicBlock();
        BasicBlock returned = writeStatement(f.getBody(), startBlock, returnBlock, symbolTable, structTable, argTypesByFun);
        if (returned != returnBlock) {
            returned.addSuccessor(returnBlock);
            returnBlock.addPredecessor(returned);
            returned.addInstruction(new LLVMJump(returnBlock.getLabel()));
        }
        returnBlock.seal();
        if (!(f.getRetType() instanceof VoidType)) {
            returnBlock.addInstruction(new LLVMReturn(f.getRetType(), returnBlock.readVariabe("_retval", f.getRetType())));
        } else {
            returnBlock.addInstruction(new LLVMReturnVoid());
        }

        if (sccp) {
            do {
                propogateConstants(startBlock, f.getParams());
            } while (fixBranches(startBlock));
        }
        if (uce) {
            do {
            } while (eliminateUselessCode(startBlock, f.getParams()));
        }

        Set<BasicBlock> visited = new HashSet<>();
        Queue<BasicBlock> queue = new ArrayDeque<>();
        visited.add(startBlock);
        queue.add(startBlock);
        while (queue.size() > 0) {
            BasicBlock cur = queue.poll();
            List<BasicBlock> newSuccessors = cur.getSuccessors().stream()
                    .filter(successor -> !visited.contains(successor))
                    .filter(successor -> successor != returnBlock)
                    .collect(Collectors.toList());
            queue.addAll(newSuccessors);
            visited.addAll(newSuccessors);
            cur.writeBlock(pw);
        }

        returnBlock.writeBlock(pw);

        pw.println("}\n");
    }

    private static BasicBlock writeStatement(Statement stmt,
                                             BasicBlock currentBlock,
                                             BasicBlock returnBlock,
                                             Map<String, Pair<Type, VariableScope>> symbolTable,
                                             Map<String, Map<String, Pair<Integer, Type>>> structTable,
                                             Map<String, List<Type>> argTypesByFun) {
        if (stmt instanceof AssignmentStatement) {
            Value source = writeExpr(((AssignmentStatement) stmt).getSource(), currentBlock, symbolTable, structTable, argTypesByFun);
            Lvalue lval = ((AssignmentStatement) stmt).getTarget();
            if (lval instanceof LvalueId) {
                Pair<Type, VariableScope> id = symbolTable.get(((LvalueId) lval).getId());
                if (id.getSecond() == VariableScope.LOCAL) {
                    currentBlock.writeVariable(((LvalueId) lval).getId(), source);
                } else if (id.getSecond() == VariableScope.GLOBAL) {
                    LLVMRegister result =  new LLVMRegister(id.getFirst(), "@" + ((LvalueId) lval).getId());
                    currentBlock.addInstruction(new LLVMStore(id.getFirst(), source, result));
                } else if (id.getSecond() == VariableScope.PARAM) {
                    currentBlock.writeVariable(((LvalueId) lval).getId(), source);
                }
            } else if (lval instanceof LvalueDot) {
                LLVMRegister left = (LLVMRegister) writeExpr(((LvalueDot) lval).getLeft(), currentBlock, symbolTable,
                                                             structTable, argTypesByFun);
                StructType structType = (StructType) left.getType();
                Pair<Integer, Type> field = structTable.get(structType.getName()).get(((LvalueDot) lval).getId());
                LLVMRegister result = new LLVMRegister(field.getSecond());
                currentBlock.addInstruction(new LLVMLoadField(structType, result, left, field.getFirst()));
                currentBlock.addInstruction(new LLVMStore(result.getType(), source, result));
            }
            return currentBlock;
        } else if (stmt instanceof BlockStatement) {
            BasicBlock b = currentBlock;
            for (Statement s : ((BlockStatement) stmt).getStatements()) {
                b = writeStatement(s, b, returnBlock, symbolTable, structTable, argTypesByFun);
                if (b == returnBlock) {
                    return returnBlock;
                }
            }
            return b;
        } else if (stmt instanceof ConditionalStatement) {
            Value guard = writeExpr(((ConditionalStatement) stmt).getGuard(), currentBlock, symbolTable, structTable, argTypesByFun);
            BasicBlock thenBlock = new BasicBlock();
            thenBlock.addPredecessor(currentBlock);
            thenBlock.seal();
            BasicBlock then = writeStatement(((ConditionalStatement) stmt).getThenBlock(),
                                             thenBlock,
                                             returnBlock,
                                             symbolTable,
                                             structTable,
                                             argTypesByFun);
            BasicBlock elseBlock = new BasicBlock();
            elseBlock.addPredecessor(currentBlock);
            elseBlock.seal();
            BasicBlock els = writeStatement(((ConditionalStatement) stmt).getElseBlock(),
                                            elseBlock,
                                            returnBlock,
                                            symbolTable,
                                            structTable,
                                            argTypesByFun);

            currentBlock.addSuccessor(thenBlock);
            currentBlock.addSuccessor(elseBlock);
            currentBlock.addInstruction(new LLVMConditionalBranch(guard, thenBlock.getLabel(), elseBlock.getLabel()));
            if (then == returnBlock && els == returnBlock) {
                return returnBlock;
            } else if (then == returnBlock) {
                return els;
            } else if (els == returnBlock) {
                return then;
            } else {
                //jump then and else to new block
                BasicBlock join = new BasicBlock();
                then.addInstruction(new LLVMJump(join.getLabel()));
                then.addSuccessor(join);
                join.addPredecessor(then);
                els.addInstruction(new LLVMJump(join.getLabel()));
                els.addSuccessor(join);
                join.addPredecessor(els);
                join.seal();
                return join;
            }
        } else if (stmt instanceof DeleteStatement) {
            Value t = writeExpr(((DeleteStatement) stmt).getExpression(), currentBlock, symbolTable, structTable, argTypesByFun);
            LLVMRegister ptr = new LLVMRegister(new PointerType());
            currentBlock.addInstruction(new LLVMBitcast(ptr, t));
            currentBlock.addInstruction(new LLVMFree(ptr));
            return currentBlock;
        } else if (stmt instanceof InvocationStatement) {
            writeExpr(((InvocationStatement) stmt).getExpression(), currentBlock, symbolTable, structTable, argTypesByFun);
            return currentBlock;
        } else if (stmt instanceof PrintLnStatement) {
            Value t = writeExpr(((PrintLnStatement) stmt).getExpression(), currentBlock, symbolTable, structTable, argTypesByFun);
            currentBlock.addInstruction(new LLVMPrint("@.println", t));
            return currentBlock;
        } else if (stmt instanceof PrintStatement) {
            Value t = writeExpr(((PrintStatement) stmt).getExpression(), currentBlock, symbolTable, structTable, argTypesByFun);
            currentBlock.addInstruction(new LLVMPrint("@.print", t));
            return currentBlock;
        } else if (stmt instanceof ReturnStatement) {
            Value val = writeExpr(((ReturnStatement) stmt).getExpression(), currentBlock, symbolTable, structTable, argTypesByFun);
            currentBlock.addSuccessor(returnBlock);
            returnBlock.addPredecessor(currentBlock);
            currentBlock.writeVariable("_retval", val);
            currentBlock.addInstruction(new LLVMJump(returnBlock.getLabel()));
            return returnBlock;
        } else if (stmt instanceof ReturnEmptyStatement) {
            currentBlock.addSuccessor(returnBlock);
            returnBlock.addPredecessor(currentBlock);
            currentBlock.addInstruction(new LLVMJump(returnBlock.getLabel()));
            return returnBlock;
        } else if (stmt instanceof WhileStatement) {
            Value guard = writeExpr(((WhileStatement) stmt).getGuard(), currentBlock, symbolTable, structTable, argTypesByFun);
            BasicBlock body = new BasicBlock();
            BasicBlock join = new BasicBlock();
            currentBlock.addInstruction(new LLVMConditionalBranch(guard, body.getLabel(), join.getLabel()));
            BasicBlock returned = writeStatement(((WhileStatement) stmt).getBody(), body, returnBlock,
                                                 symbolTable, structTable, argTypesByFun);
            if (returned != returnBlock) {
                guard = writeExpr(((WhileStatement) stmt).getGuard(), returned, symbolTable, structTable, argTypesByFun);
                returned.addInstruction(new LLVMConditionalBranch(guard, body.getLabel(), join.getLabel()));
                returned.addSuccessor(body);
                body.addPredecessor(returned);
                returned.addSuccessor(join);
                join.addPredecessor(returned);
            }
            currentBlock.addSuccessor(body);
            body.addPredecessor(currentBlock);
            currentBlock.addSuccessor(join);
            join.addPredecessor(currentBlock);

            body.seal();
            join.seal();

            return join;
        } else {
            throw new RuntimeException("Invalid Statement");
        }
    }

    private static Value writeExpr(Expression expr,
                                   BasicBlock curBlock,
                                   Map<String, Pair<Type, VariableScope>> symbolTable,
                                   Map<String, Map<String, Pair<Integer, Type>>> structTable,
                                   Map<String, List<Type>> argTypesByFun) {
        if (expr instanceof BinaryExpression) {
            Value left = writeExpr(((BinaryExpression) expr).getLeft(), curBlock, symbolTable, structTable, argTypesByFun);
            Value right = writeExpr(((BinaryExpression) expr).getRight(), curBlock, symbolTable, structTable, argTypesByFun);
            LLVMRegister reg;
            switch (((BinaryExpression) expr).getOperator()) {
                case TIMES:
                    reg = new LLVMRegister(new IntType());
                    curBlock.addInstruction(
                            new LLVMArithmetic(LLVMArithmetic.Operator.MUL, new IntType(), reg, left, right));
                    return reg;
                case DIVIDE:
                    reg = new LLVMRegister(new IntType());
                    curBlock.addInstruction(
                            new LLVMArithmetic(LLVMArithmetic.Operator.SDIV, new IntType(), reg, left, right));
                    return reg;
                case PLUS:
                    reg = new LLVMRegister(new IntType());
                    curBlock.addInstruction(
                            new LLVMArithmetic(LLVMArithmetic.Operator.ADD, new IntType(), reg, left, right));
                    return reg;
                case MINUS:
                    reg = new LLVMRegister(new IntType());
                    curBlock.addInstruction(
                            new LLVMArithmetic(LLVMArithmetic.Operator.SUB, new IntType(), reg, left, right));
                    return reg;
                case LT:
                    reg = new LLVMRegister(new BoolType());
                    curBlock.addInstruction(
                            new LLVMComparison(LLVMComparison.Operator.LT, left.getType(), reg, left, right));
                    return reg;
                case GT:
                    reg = new LLVMRegister(new BoolType());
                    curBlock.addInstruction(
                            new LLVMComparison(LLVMComparison.Operator.GT, left.getType(), reg, left, right));
                    return reg;
                case LE:
                    reg = new LLVMRegister(new BoolType());
                    curBlock.addInstruction(
                            new LLVMComparison(LLVMComparison.Operator.LE, left.getType(), reg, left, right));
                    return reg;
                case GE:
                    reg = new LLVMRegister(new BoolType());
                    curBlock.addInstruction(
                            new LLVMComparison(LLVMComparison.Operator.GE, left.getType(), reg, left, right));
                    return reg;
                case EQ:
                    reg = new LLVMRegister(new BoolType());
                    curBlock.addInstruction(
                            new LLVMComparison(LLVMComparison.Operator.EQ, left.getType(), reg, left, right));
                    return reg;
                case NE:
                    reg = new LLVMRegister(new BoolType());
                    curBlock.addInstruction(
                            new LLVMComparison(LLVMComparison.Operator.NE, left.getType(), reg, left, right));
                    return reg;
                case AND:
                    reg = new LLVMRegister(new BoolType());
                    curBlock.addInstruction(new LLVMBoolOp(LLVMBoolOp.Operator.AND, new BoolType(), reg, left, right));
                    return reg;
                case OR:
                    reg = new LLVMRegister(new BoolType());
                    curBlock.addInstruction(new LLVMBoolOp(LLVMBoolOp.Operator.OR, new BoolType(), reg, left, right));
                    return reg;
                default:
                    throw new RuntimeException("Invalid BinaryExpression");
            }
        } else if (expr instanceof DotExpression) {
            LLVMRegister left = (LLVMRegister) writeExpr(((DotExpression) expr).getLeft(), curBlock, symbolTable,
                                                         structTable, argTypesByFun);
            StructType t = (StructType) left.getType();
            Pair<Integer, Type> field = structTable.get(t.getName()).get(((DotExpression) expr).getId());

            // %r1 = getelementptr %<struct>* <left>, i1 0, i32 <index>
            LLVMRegister temp1 = new LLVMRegister(field.getSecond());
            curBlock.addInstruction(new LLVMLoadField(t, temp1, left, field.getFirst()));
            // %r2 = load %<type>* %r1
            LLVMRegister temp2 = new LLVMRegister(field.getSecond());
            curBlock.addInstruction(new LLVMLoad(field.getSecond(), temp2, temp1));

            return temp2;
        } else if (expr instanceof UnaryExpression) {
            Value operand = writeExpr(((UnaryExpression) expr).getOperand(), curBlock, symbolTable, structTable, argTypesByFun);
            if (UnaryExpression.Operator.NOT.equals(((UnaryExpression) expr).getOperator())) {
                if (operand instanceof LLVMImmediate) {
                    return new LLVMImmediate(((LLVMImmediate) operand).getVal() == 1 ? 0 : 1);
                } else {
                    LLVMRegister reg = new LLVMRegister(new BoolType());
                    curBlock.addInstruction(new LLVMBoolOp(LLVMBoolOp.Operator.XOR, new BoolType(), reg, operand,
                                                           new LLVMImmediate(1)));
                    return reg;
                }
            } else if (UnaryExpression.Operator.MINUS.equals(((UnaryExpression) expr).getOperator())) {
                if (operand instanceof LLVMImmediate) {
                    return new LLVMImmediate(-((LLVMImmediate) operand).getVal());
                } else {
                    LLVMRegister reg = new LLVMRegister(new IntType());
                    curBlock.addInstruction(
                            new LLVMArithmetic(LLVMArithmetic.Operator.SUB, new IntType(), reg, new LLVMImmediate(0),
                                               operand));
                    return reg;
                }
            }
        } else if (expr instanceof FalseExpression) {
            return new LLVMImmediate(0);
        } else if (expr instanceof TrueExpression) {
            return new LLVMImmediate(1);
        } else if (expr instanceof IdentifierExpression) {
            Pair<Type, VariableScope> iden = symbolTable.get(((IdentifierExpression) expr).getId());
            if (VariableScope.GLOBAL == iden.getSecond()) {
                LLVMRegister result = new LLVMRegister(iden.getFirst());
                curBlock.addInstruction(
                        new LLVMLoad(iden.getFirst(),
                                     result,
                                     new LLVMRegister(iden.getFirst(), "@" + ((IdentifierExpression) expr).getId())));
                return result;
            } else if (VariableScope.LOCAL == iden.getSecond()) {
                return curBlock.readVariabe(((IdentifierExpression) expr).getId(), iden.getFirst());
            } else if (VariableScope.PARAM == iden.getSecond()) {
                return curBlock.readVariabe(((IdentifierExpression) expr).getId(), iden.getFirst());
            }
        } else if (expr instanceof IntegerExpression) {
            return new LLVMImmediate(Integer.parseInt(((IntegerExpression) expr).getValue()));
        } else if (expr instanceof InvocationExpression) {
            Value[] args = ((InvocationExpression) expr).getArguments().stream()
                    .map(e -> writeExpr(e, curBlock, symbolTable, structTable, argTypesByFun))
                    .toArray(Value[]::new);
            Type retType = symbolTable.get(((InvocationExpression) expr).getName()).getFirst();
            LLVMRegister result = new LLVMRegister(retType);
            curBlock.addInstruction(new LLVMInvocation(argTypesByFun.get(((InvocationExpression) expr).getName()), retType, result, ((InvocationExpression) expr).getName(), args));
            return result;
        } else if (expr instanceof NewExpression) {
            LLVMRegister alloced = new LLVMRegister(new PointerType());
            curBlock.addInstruction(
                    new LLVMMalloc(alloced, 8 * structTable.get(((NewExpression) expr).getId()).keySet().size()));
            LLVMRegister result = new LLVMRegister(new StructType(0, ((NewExpression) expr).getId()));
            curBlock.addInstruction(new LLVMBitcast(result, alloced));
            return result;
        } else if (expr instanceof NullExpression) {
            return LLVMNull.instance();
        } else if (expr instanceof ReadExpression) {
            curBlock.addInstruction(new LLVMRead());
            LLVMRegister result = new LLVMRegister(new IntType());
            curBlock.addInstruction(new LLVMLoad(result.getType(), result, new LLVMRegister(result.getType(), "@.read_scratch")));
            return result;
        }
        throw new RuntimeException("Invalid Expression");
    }

    private static void propogateConstants(BasicBlock startBlock, List<Declaration> params) {
        Map<String, ConstValue> valueByRegister = new HashMap<>();
        Set<LLVMRegister> ssaWorklist = new HashSet<>();
        Map<String, DefUse> defUseMap = generateDefUseMap(startBlock, params, valueByRegister, ssaWorklist);

        while (!ssaWorklist.isEmpty()) {
            LLVMRegister cur = ssaWorklist.toArray(new LLVMRegister[ssaWorklist.size()])[0];
            ssaWorklist.remove(cur);
            DefUse du = defUseMap.get(cur.toString());
            for (LLVMInstruction use : du.getUses()) {
                LLVMRegister reg = use.getDefRegister();
                if (reg != null) {
                    ConstValue m = valueByRegister.get(reg.toString());
                    if (m != null && !(m instanceof Bottom)) {
                        ConstValue t = use.evaluate(valueByRegister);
                        if (!m.eq(t)) {
                            valueByRegister.put(reg.toString(), t);
                            ssaWorklist.add(reg);
                        }
                    }
                }
            }
        }
        for (Map.Entry<String, ConstValue> e : valueByRegister.entrySet()) {
            if (e.getValue() instanceof ConstImm) {
                for (LLVMInstruction use : defUseMap.get(e.getKey()).getUses()) {
                    use.replace(e.getKey(), (ConstImm)e.getValue());
                }
            }
        }
    }

    private static boolean eliminateUselessCode(BasicBlock block, List<Declaration> params) {
        Map<LLVMInstruction, BasicBlock> blockByInstruction = new HashMap<>();
        Map<String, DefUse> defUseMap = new HashMap<>();
        List<LLVMPhi> phis = new ArrayList<>();
        for (Declaration param : params) {
            defUseMap.put("%" + param.getName(),new DefUse(null));
        }
        Queue<BasicBlock> visited = new ArrayDeque<>();
        Queue<BasicBlock> toVisit = new ArrayDeque<>();
        toVisit.add(block);
        while (!toVisit.isEmpty()) {
            BasicBlock cur = toVisit.poll();
            visited.add(cur);
            for (BasicBlock succ : cur.getSuccessors()) {
                if (!visited.contains(succ)) {
                    toVisit.add(succ);
                }
            }

            for (LLVMPhi phi : cur.getPhis()) {
                blockByInstruction.put(phi, cur);
                LLVMRegister def = phi.getDefRegister();
                if (def != null) {
                    defUseMap.put(def.toString(), new DefUse(phi));
                }
                phis.add(phi);
            }
            for (LLVMInstruction inst : cur.getInstructions()) {
                blockByInstruction.put(inst, cur);
                LLVMRegister def = inst.getDefRegister();
                if (def != null) {
                    defUseMap.put(def.toString(), new DefUse(inst));
                }
                for (LLVMRegister use : inst.getUseRegisters()) {
                    if (use.toString().charAt(0) != '@') {
                        defUseMap.get(use.toString()).addUse(inst);
                    }
                }
            }
        }
        for (LLVMPhi phi : phis) {
            for (LLVMRegister use : phi.getUseRegisters()) {
                if (use.toString().charAt(0) != '@') {
                    defUseMap.get(use.toString()).addUse(phi);
                }
            }
        }

        boolean changed = false;
        for (DefUse du : defUseMap.values()) {
            if (du.getUses().size() == 0 && du.getDefinition() != null && !(du.getDefinition() instanceof LLVMInvocation)) {
                changed = true;
                blockByInstruction.get(du.getDefinition()).remove(du.getDefinition());
            }
        }
        return changed;
    }

    private static boolean fixBranches(BasicBlock block) {
        Queue<BasicBlock> visited = new ArrayDeque<>();
        Queue<BasicBlock> toVisit = new ArrayDeque<>();
        toVisit.add(block);
        while (!toVisit.isEmpty()) {
            BasicBlock cur = toVisit.poll();
            visited.add(cur);
            for (BasicBlock succ : cur.getSuccessors()) {
                if (!visited.contains(succ)) {
                    toVisit.add(succ);
                }
            }

            List<LLVMInstruction> insts = cur.getInstructions();
            LLVMInstruction inst = insts.get(insts.size() - 1);
            if (inst instanceof LLVMConditionalBranch && ((LLVMConditionalBranch) inst).isTrivial()) {
                insts.remove(inst);
                Pair<LLVMJump, String> info = ((LLVMConditionalBranch) inst).getNewBranchInstAndBadLabel();
                insts.add(info.getFirst());
                removeBlock(cur, info.getSecond());
                return true;
            }
        }
        return false;
    }

    private static void removeBlock(BasicBlock block, String label) {
        Queue<BasicBlock> visited = new ArrayDeque<>();
        Queue<BasicBlock> toVisit = new ArrayDeque<>();
        toVisit.add(block);
        while (!toVisit.isEmpty()) {
            BasicBlock cur = toVisit.poll();
            visited.add(cur);
            for (BasicBlock succ : cur.getSuccessors()) {
                if (!visited.contains(succ)) {
                    toVisit.add(succ);
                }
            }

            if (cur.getLabel().equals(label)) {
                cur.removePredecessor(block);
                for (LLVMPhi phi : cur.getPhis()) {
                    phi.removeLabel(block.getLabel());
                }
            }
        }
    }

    private static Map<String, DefUse> generateDefUseMap(BasicBlock startBlock,
                                                         List<Declaration> params,
                                                         Map<String, ConstValue> valueByRegister,
                                                         Set<LLVMRegister> ssaWorklist) {
        Map<String, DefUse> defUseMap = new HashMap<>();
        for (Declaration param : params) {
            defUseMap.put("%" + param.getName(),new DefUse(null));
            valueByRegister.put("%" + param.getName(), new Bottom());
            ssaWorklist.add(new LLVMRegister(param.getType(), "%" + param.getName()));
        }
        Queue<BasicBlock> visited = new ArrayDeque<>();
        Queue<BasicBlock> toVisit = new ArrayDeque<>();
        toVisit.add(startBlock);
        List<LLVMPhi> phis = new ArrayList<>();
        while (!toVisit.isEmpty()) {
            BasicBlock cur = toVisit.poll();
            visited.add(cur);
            for (BasicBlock succ : cur.getSuccessors()) {
                if (!visited.contains(succ)) {
                    toVisit.add(succ);
                }
            }

            for (LLVMPhi phi : cur.getPhis()) {
                LLVMRegister def = phi.getDefRegister();
                if (def != null) {
                    defUseMap.put(def.toString(), new DefUse(phi));
                }
                phis.add(phi);
                ConstValue v = phi.initialize(valueByRegister);
                if (v != null && !(v instanceof Top)) {
                    ssaWorklist.add(phi.getDefRegister());
                }
                valueByRegister.put(phi.getDefRegister().toString(), v);
            }
            for (LLVMInstruction inst : cur.getInstructions()) {
                LLVMRegister def = inst.getDefRegister();
                if (def != null) {
                    defUseMap.put(def.toString(), new DefUse(inst));
                }
                for (LLVMRegister use : inst.getUseRegisters()) {
                    if (use.toString().charAt(0) != '@') {
                        defUseMap.get(use.toString()).addUse(inst);
                    }
                }
                ConstValue v = inst.initialize(valueByRegister);
                if (v != null) {
                    if  (!(v instanceof Top)) {
                        ssaWorklist.add(inst.getDefRegister());
                    }
                    valueByRegister.put(inst.getDefRegister().toString(), v);
                }
            }
        }
        for (LLVMPhi phi : phis) {
            for (LLVMRegister use : phi.getUseRegisters()) {
                defUseMap.get(use.toString()).addUse(phi);
            }
        }
        return defUseMap;
    }

    private static Map<String, Pair<Type, VariableScope>> createSymbolTable(Program program) {
        Map<String, Pair<Type, VariableScope>> symbolTable = new HashMap<>();

        for (Declaration global : program.getDecls()) {
            symbolTable.put(global.getName(), new Pair<>(global.getType(), VariableScope.GLOBAL));
        }
        for (Function f : program.getFuncs()) {
            symbolTable.put(f.getName(), new Pair<>(f.getRetType(), VariableScope.GLOBAL));
        }

        return symbolTable;
    }

    private static Map<String, Map<String, Pair<Integer, Type>>> createStructTable(List<TypeDeclaration> structs) {
        Map<String, Map<String, Pair<Integer, Type>>> structTable = new HashMap<>();
        for (TypeDeclaration struct : structs) {
            Map<String, Pair<Integer, Type>> fieldIndexByName = new HashMap<>();
            for (int i = 0; i < struct.getFields().size(); ++i) {
                fieldIndexByName.put(struct.getFields().get(i).getName(),
                                     new Pair<>(i, struct.getFields().get(i).getType()));
            }
            structTable.put(struct.getName(), fieldIndexByName);
        }
        return structTable;
    }

    private static Map<String, List<Type>> createArgTypesByFun(List<Function> funs) {
        Map<String, List<Type>> argTypesByFunName = new HashMap<>();
        for (Function f : funs) {
            argTypesByFunName.put(f.getName(), f.getParams().stream()
                    .map(Declaration::getType)
                    .collect(Collectors.toList()));
        }
        return argTypesByFunName;
    }
}

