package cfg;

import ast.*;
import cfg.arm.*;
import constprop.Bottom;
import constprop.ConstImm;
import constprop.ConstValue;
import constprop.Top;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class ARMEmitter {
    public static void writeLlvm(Program prog, String filename, boolean sccp, boolean uce) {
        try (PrintWriter pw = new PrintWriter(new File(filename))) {
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

    private static void writeGlobals(List<Declaration> globals, PrintWriter pw) {
        pw.println("\t.global __aeabi_idiv");
        pw.println();
        pw.println(".read");
        pw.println("\t.ascii \"%ld\\000\"");
        pw.println("\t.align 2");
        pw.println(".print");
        pw.println("\t.ascii \"%ld \\000\"");
        pw.println("\t.align 2");
        pw.println(".println");
        pw.println("\t.ascii \"%ld\\012\\000\"");
        pw.println("\t.align 2");
        pw.println();
        pw.println("\t.comm read_scratch, 4, 4");
        for (Declaration d : globals) {
            pw.println(String.format("\t.comm %s, 4, 4", d.getName()));
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
        for (int i = 0; i < f.getParams().size(); ++i) {
            symbolTable.put(f.getParams().get(i).getName(),
                            new Pair<>(f.getParams().get(i).getType(), VariableScope.PARAM));
            startBlock.writeVariable(f.getParams().get(i).getName(),
                                     new ARMRegister(f.getParams().get(i).getType(),
                                                      String.format("%%%s", f.getParams().get(i).getName())));
        }

        // Write locals
        for (Declaration local : f.getLocals()) {
            symbolTable.put(local.getName(), new Pair<>(local.getType(), VariableScope.LOCAL));
        }

        BasicBlock returnBlock = new BasicBlock();
        BasicBlock returned = writeStatement(f.getBody(), startBlock, returnBlock, symbolTable, structTable, argTypesByFun,
                                             f.getLocals());
        if (returned != returnBlock) {
            returned.addSuccessor(returnBlock);
            returnBlock.addPredecessor(returned);
            returned.addInstruction(new ARMJump(returnBlock.getLabel()));
        }
        returnBlock.seal();
        if (!(f.getRetType() instanceof VoidType)) {
            returnBlock.addInstruction(new ARMReturn(f.getRetType(),
                                                     returnBlock.readVariable("_retval", f.getRetType()),
                                                     f.getLocals().size()));
        } else {
            returnBlock.addInstruction(new ARMReturnVoid(f.getLocals().size()));
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

        fixPhis(startBlock);

        pw.println("\t.text");
        pw.println("\t.align 2");
        pw.println("\t.global " + f.getName());
        pw.println(f.getName() + ":");
        pw.println("\tpush {fp, lr}");
        pw.println("\tadd fp, sp, #4");
        pw.println("\tsub sp, sp #" + 4 * f.getLocals().size());

        for (int i = 0; i < f.getParams().size(); ++i) {
            if (i < 4) {
                pw.println("\tmov %" + f.getParams().get(i).getName() + ", r" + i);
            } else {
                pw.println("\tldr %" + f.getParams().get(i).getName() + ", [fp, #" + (4 + (i - 4) * 4) + "]");
            }
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
        pw.println();
        pw.println();
    }

    private static BasicBlock writeStatement(Statement stmt,
                                             BasicBlock currentBlock,
                                             BasicBlock returnBlock,
                                             Map<String, Pair<Type, VariableScope>> symbolTable,
                                             Map<String, Map<String, Pair<Integer, Type>>> structTable,
                                             Map<String, List<Type>> argTypesByFun,
                                             List<Declaration> locals) {
        if (stmt instanceof AssignmentStatement) {
            Value source = writeExpr(((AssignmentStatement) stmt).getSource(), currentBlock, symbolTable, structTable, argTypesByFun, locals);
            Lvalue lval = ((AssignmentStatement) stmt).getTarget();
            if (lval instanceof LvalueId) {
                Pair<Type, VariableScope> id = symbolTable.get(((LvalueId) lval).getId());
                if (id.getSecond() == VariableScope.LOCAL) {
                    currentBlock.writeVariable(((LvalueId) lval).getId(), source);
//                    for (int i = 0; i < locals.size(); ++i) {
//                        if (((LvalueId) lval).getId().equals(locals.get(i).getName())) {
//                            currentBlock.addInstruction(
//                                    new ARMStore(id.getFirst(), source, new ARMRegister(new IntType(), "fp, #-" + (4 + i * 4))));
//                        }
//                    }
                } else if (id.getSecond() == VariableScope.GLOBAL) {
                    ARMRegister result = new ARMRegister(id.getFirst());
                    currentBlock.addInstruction(new ARMLoadGlobal(((LvalueId) lval).getId(), result));
                    currentBlock.addInstruction(new ARMStore(id.getFirst(), source, result));
                } else if (id.getSecond() == VariableScope.PARAM) {
                    currentBlock.writeVariable(((LvalueId) lval).getId(), source);
                }
            } else if (lval instanceof LvalueDot) {
                ARMRegister left = (ARMRegister) writeExpr(((LvalueDot) lval).getLeft(), currentBlock, symbolTable,
                                                           structTable, argTypesByFun, locals);
                StructType structType = (StructType) left.getType();
                Pair<Integer, Type> field = structTable.get(structType.getName()).get(((LvalueDot) lval).getId());
                currentBlock.addInstruction(new ARMStoreField(source, left, field.getFirst()));
            }
            return currentBlock;
        } else if (stmt instanceof BlockStatement) {
            BasicBlock b = currentBlock;
            for (Statement s : ((BlockStatement) stmt).getStatements()) {
                b = writeStatement(s, b, returnBlock, symbolTable, structTable, argTypesByFun, locals);
                if (b == returnBlock) {
                    return returnBlock;
                }
            }
            return b;
        } else if (stmt instanceof ConditionalStatement) {
            Value guard = writeExpr(((ConditionalStatement) stmt).getGuard(), currentBlock, symbolTable, structTable, argTypesByFun, locals);
            BasicBlock thenBlock = new BasicBlock();
            thenBlock.addPredecessor(currentBlock);
            thenBlock.seal();
            BasicBlock then = writeStatement(((ConditionalStatement) stmt).getThenBlock(),
                                             thenBlock,
                                             returnBlock,
                                             symbolTable,
                                             structTable,
                                             argTypesByFun,
                                             locals);
            BasicBlock elseBlock = new BasicBlock();
            elseBlock.addPredecessor(currentBlock);
            elseBlock.seal();
            BasicBlock els = writeStatement(((ConditionalStatement) stmt).getElseBlock(),
                                            elseBlock,
                                            returnBlock,
                                            symbolTable,
                                            structTable,
                                            argTypesByFun,
                                            locals);

            currentBlock.addSuccessor(thenBlock);
            currentBlock.addSuccessor(elseBlock);
            currentBlock.addInstruction(new ARMLLVMConditionalBranch(guard, thenBlock.getLabel(), elseBlock.getLabel()));
            if (then == returnBlock && els == returnBlock) {
                return returnBlock;
            } else if (then == returnBlock) {
                return els;
            } else if (els == returnBlock) {
                return then;
            } else {
                //jump then and else to new block
                BasicBlock join = new BasicBlock();
                then.addInstruction(new ARMJump(join.getLabel()));
                then.addSuccessor(join);
                join.addPredecessor(then);
                els.addInstruction(new ARMJump(join.getLabel()));
                els.addSuccessor(join);
                join.addPredecessor(els);
                join.seal();
                return join;
            }
        } else if (stmt instanceof DeleteStatement) {
            Value t = writeExpr(((DeleteStatement) stmt).getExpression(), currentBlock, symbolTable, structTable, argTypesByFun, locals);
            currentBlock.addInstruction(new ARMFree((ARMRegister) t));
            return currentBlock;
        } else if (stmt instanceof InvocationStatement) {
            writeExpr(((InvocationStatement) stmt).getExpression(), currentBlock, symbolTable, structTable, argTypesByFun, locals);
            return currentBlock;
        } else if (stmt instanceof PrintLnStatement) {
            Value t = writeExpr(((PrintLnStatement) stmt).getExpression(), currentBlock, symbolTable, structTable, argTypesByFun, locals);
            currentBlock.addInstruction(new ARMPrint(".println", t));
            return currentBlock;
        } else if (stmt instanceof PrintStatement) {
            Value t = writeExpr(((PrintStatement) stmt).getExpression(), currentBlock, symbolTable, structTable, argTypesByFun, locals);
            currentBlock.addInstruction(new ARMPrint(".print", t));
            return currentBlock;
        } else if (stmt instanceof ReturnStatement) {
            Value val = writeExpr(((ReturnStatement) stmt).getExpression(), currentBlock, symbolTable, structTable, argTypesByFun, locals);
            currentBlock.addSuccessor(returnBlock);
            returnBlock.addPredecessor(currentBlock);
            currentBlock.writeVariable("_retval", val);
            currentBlock.addInstruction(new ARMJump(returnBlock.getLabel()));
            return returnBlock;
        } else if (stmt instanceof ReturnEmptyStatement) {
            currentBlock.addSuccessor(returnBlock);
            returnBlock.addPredecessor(currentBlock);
            currentBlock.addInstruction(new ARMJump(returnBlock.getLabel()));
            return returnBlock;
        } else if (stmt instanceof WhileStatement) {
            Value guard = writeExpr(((WhileStatement) stmt).getGuard(), currentBlock, symbolTable, structTable, argTypesByFun, locals);
            BasicBlock body = new BasicBlock();
            BasicBlock join = new BasicBlock();
            currentBlock.addInstruction(new ARMLLVMConditionalBranch(guard, body.getLabel(), join.getLabel()));
            BasicBlock returned = writeStatement(((WhileStatement) stmt).getBody(), body, returnBlock,
                                                 symbolTable, structTable, argTypesByFun, locals);
            if (returned != returnBlock) {
                guard = writeExpr(((WhileStatement) stmt).getGuard(), returned, symbolTable, structTable, argTypesByFun, locals);
                returned.addInstruction(new ARMLLVMConditionalBranch(guard, body.getLabel(), join.getLabel()));
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
                                   Map<String, List<Type>> argTypesByFun,
                                   List<Declaration> locals) {
        if (expr instanceof BinaryExpression) {
            Value left = writeExpr(((BinaryExpression) expr).getLeft(), curBlock, symbolTable, structTable, argTypesByFun, locals);
            Value right = writeExpr(((BinaryExpression) expr).getRight(), curBlock, symbolTable, structTable, argTypesByFun, locals);
            ARMRegister reg;
            switch (((BinaryExpression) expr).getOperator()) {
                case TIMES:
                    reg = new ARMRegister(new IntType());
                    curBlock.addInstruction(
                            new ARMArithmetic(ARMArithmetic.Operator.MUL, new IntType(), reg, left, right));
                    return reg;
                case DIVIDE:
                    reg = new ARMRegister(new IntType());
                    curBlock.addInstruction(
                            new ARMArithmetic(ARMArithmetic.Operator.SDIV, new IntType(), reg, left, right));
                    return reg;
                case PLUS:
                    reg = new ARMRegister(new IntType());
                    curBlock.addInstruction(
                            new ARMArithmetic(ARMArithmetic.Operator.ADD, new IntType(), reg, left, right));
                    return reg;
                case MINUS:
                    reg = new ARMRegister(new IntType());
                    curBlock.addInstruction(
                            new ARMArithmetic(ARMArithmetic.Operator.SUB, new IntType(), reg, left, right));
                    return reg;
                case LT:
                    reg = new ARMRegister(new BoolType());
                    curBlock.addInstruction(
                            new ARMComparison(ARMComparison.Operator.LT, left.getType(), reg, left, right));
                    return reg;
                case GT:
                    reg = new ARMRegister(new BoolType());
                    curBlock.addInstruction(
                            new ARMComparison(ARMComparison.Operator.GT, left.getType(), reg, left, right));
                    return reg;
                case LE:
                    reg = new ARMRegister(new BoolType());
                    curBlock.addInstruction(
                            new ARMComparison(ARMComparison.Operator.LE, left.getType(), reg, left, right));
                    return reg;
                case GE:
                    reg = new ARMRegister(new BoolType());
                    curBlock.addInstruction(
                            new ARMComparison(ARMComparison.Operator.GE, left.getType(), reg, left, right));
                    return reg;
                case EQ:
                    reg = new ARMRegister(new BoolType());
                    curBlock.addInstruction(
                            new ARMComparison(ARMComparison.Operator.EQ, left.getType(), reg, left, right));
                    return reg;
                case NE:
                    reg = new ARMRegister(new BoolType());
                    curBlock.addInstruction(
                            new ARMComparison(ARMComparison.Operator.NE, left.getType(), reg, left, right));
                    return reg;
                case AND:
                    reg = new ARMRegister(new BoolType());
                    curBlock.addInstruction(new ARMBoolOp(ARMBoolOp.Operator.AND, new BoolType(), reg, left, right));
                    return reg;
                case OR:
                    reg = new ARMRegister(new BoolType());
                    curBlock.addInstruction(new ARMBoolOp(ARMBoolOp.Operator.OR, new BoolType(), reg, left, right));
                    return reg;
                default:
                    throw new RuntimeException("Invalid BinaryExpression");
            }
        } else if (expr instanceof DotExpression) {
            ARMRegister left = (ARMRegister) writeExpr(((DotExpression) expr).getLeft(), curBlock, symbolTable,
                                                       structTable, argTypesByFun, locals);
            StructType t = (StructType) left.getType();
            Pair<Integer, Type> field = structTable.get(t.getName()).get(((DotExpression) expr).getId());

            // %r1 = getelementptr %<struct>* <left>, i1 0, i32 <index>
            ARMRegister temp1 = new ARMRegister(field.getSecond());
            curBlock.addInstruction(new ARMLoadField(t, temp1, left, field.getFirst()));
            // %r2 = load %<type>* %r1
            ARMRegister temp2 = new ARMRegister(field.getSecond());
            curBlock.addInstruction(new ARMLoad(field.getSecond(), temp2, temp1));

            return temp2;
        } else if (expr instanceof UnaryExpression) {
            Value operand = writeExpr(((UnaryExpression) expr).getOperand(), curBlock, symbolTable, structTable, argTypesByFun, locals);
            if (UnaryExpression.Operator.NOT.equals(((UnaryExpression) expr).getOperator())) {
                if (operand instanceof ARMImmediate) {
                    return new ARMImmediate(((ARMImmediate) operand).getVal() == 1 ? 0 : 1);
                } else {
                    ARMRegister reg = new ARMRegister(new BoolType());
                    curBlock.addInstruction(new ARMBoolOp(ARMBoolOp.Operator.XOR, new BoolType(), reg, operand,
                                                          new ARMImmediate(1)));
                    return reg;
                }
            } else if (UnaryExpression.Operator.MINUS.equals(((UnaryExpression) expr).getOperator())) {
                if (operand instanceof ARMImmediate) {
                    return new ARMImmediate(-((ARMImmediate) operand).getVal());
                } else {
                    ARMRegister reg = new ARMRegister(new IntType());
                    curBlock.addInstruction(
                            new ARMArithmetic(ARMArithmetic.Operator.SUB, new IntType(), reg, new ARMImmediate(0),
                                              operand));
                    return reg;
                }
            }
        } else if (expr instanceof FalseExpression) {
            return new ARMImmediate(0);
        } else if (expr instanceof TrueExpression) {
            return new ARMImmediate(1);
        } else if (expr instanceof IdentifierExpression) {
            Pair<Type, VariableScope> iden = symbolTable.get(((IdentifierExpression) expr).getId());
            if (VariableScope.GLOBAL == iden.getSecond()) {
                ARMRegister result = new ARMRegister(iden.getFirst());
                ARMRegister temp = new ARMRegister(iden.getFirst());
                curBlock.addInstruction(new ARMLoadGlobal(((IdentifierExpression) expr).getId(), temp));
                curBlock.addInstruction(new ARMLoad(iden.getFirst(), result, temp));
                return result;
            } else if (VariableScope.LOCAL == iden.getSecond()) {
                return curBlock.readVariable(((IdentifierExpression) expr).getId(), iden.getFirst());
            } else if (VariableScope.PARAM == iden.getSecond()) {
                return curBlock.readVariable(((IdentifierExpression) expr).getId(), iden.getFirst());
            }
        } else if (expr instanceof IntegerExpression) {
            return new ARMImmediate(Integer.parseInt(((IntegerExpression) expr).getValue()));
        } else if (expr instanceof InvocationExpression) {
            Value[] args = ((InvocationExpression) expr).getArguments().stream()
                    .map(e -> writeExpr(e, curBlock, symbolTable, structTable, argTypesByFun, locals))
                    .toArray(Value[]::new);
            Type retType = symbolTable.get(((InvocationExpression) expr).getName()).getFirst();
            ARMRegister result = new ARMRegister(retType);
            curBlock.addInstruction(new ARMInvocation(argTypesByFun.get(((InvocationExpression) expr).getName()), retType, result, ((InvocationExpression) expr).getName(), args));
            return result;
        } else if (expr instanceof NewExpression) {
            ARMRegister alloced = new ARMRegister(new StructType(0, ((NewExpression) expr).getId()));
            curBlock.addInstruction(
                    new ARMMalloc(alloced, 4 * structTable.get(((NewExpression) expr).getId()).keySet().size()));
            return alloced;
        } else if (expr instanceof NullExpression) {
            return ARMNull.instance();
        } else if (expr instanceof ReadExpression) {
            curBlock.addInstruction(new ARMRead());
            ARMRegister result = new ARMRegister(new IntType());
            curBlock.addInstruction(new ARMLoad(result.getType(), result, new ARMRegister(result.getType(), "@.read_scratch")));
            return result;
        }
        throw new RuntimeException("Invalid Expression");
    }

    private static void fixPhis(BasicBlock startBlock) {
        Queue<BasicBlock> visited = new ArrayDeque<>();
        Queue<BasicBlock> toVisit = new ArrayDeque<>();
        toVisit.add(startBlock);
        while (!toVisit.isEmpty()) {
            BasicBlock cur = toVisit.poll();
            visited.add(cur);
            for (BasicBlock succ : cur.getSuccessors()) {
                if (!visited.contains(succ)) {
                    toVisit.add(succ);
                }
            }

            cur.resolvePhis();
        }
    }

    private static void propogateConstants(BasicBlock startBlock, List<Declaration> params) {
        Map<String, ConstValue> valueByRegister = new HashMap<>();
        Set<ARMRegister> ssaWorklist = new HashSet<>();
        Map<String, DefUse> defUseMap = generateDefUseMap(startBlock, params, valueByRegister, ssaWorklist);

        while (!ssaWorklist.isEmpty()) {
            ARMRegister cur = ssaWorklist.toArray(new ARMRegister[ssaWorklist.size()])[0];
            ssaWorklist.remove(cur);
            DefUse du = defUseMap.get(cur.toString());
            for (ARMInstruction use : du.getUses()) {
                ARMRegister reg = use.getDefRegister();
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
                for (ARMInstruction use : defUseMap.get(e.getKey()).getUses()) {
                    use.replace(e.getKey(), (ConstImm)e.getValue());
                }
            }
        }
    }

    private static boolean eliminateUselessCode(BasicBlock block, List<Declaration> params) {
        Map<ARMInstruction, BasicBlock> blockByInstruction = new HashMap<>();
        Map<String, DefUse> defUseMap = new HashMap<>();
        List<ARMPhi> phis = new ArrayList<>();
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

            for (ARMPhi phi : cur.getPhis()) {
                blockByInstruction.put(phi, cur);
                ARMRegister def = phi.getDefRegister();
                if (def != null) {
                    defUseMap.put(def.toString(), new DefUse(phi));
                }
                phis.add(phi);
            }
            for (ARMInstruction inst : cur.getInstructions()) {
                blockByInstruction.put(inst, cur);
                ARMRegister def = inst.getDefRegister();
                if (def != null) {
                    defUseMap.put(def.toString(), new DefUse(inst));
                }
                for (ARMRegister use : inst.getUseRegisters()) {
                    if (use.toString().charAt(0) != '@' && defUseMap.containsKey(use.toString())) {
                        defUseMap.get(use.toString()).addUse(inst);
                    }
                }
            }
        }
        for (ARMPhi phi : phis) {
            for (ARMRegister use : phi.getUseRegisters()) {
                if (use.toString().charAt(0) != '@') {
                    defUseMap.get(use.toString()).addUse(phi);
                }
            }
        }

        boolean changed = false;
        for (DefUse du : defUseMap.values()) {
            if (du.getUses().size() == 0 && du.getDefinition() != null && !(du.getDefinition() instanceof ARMInvocation)) {
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

            List<ARMInstruction> insts = cur.getInstructions();
            ARMInstruction inst = insts.get(insts.size() - 1);
            if (inst instanceof ARMLLVMConditionalBranch && ((ARMLLVMConditionalBranch) inst).isTrivial()) {
                insts.remove(inst);
                Pair<ARMJump, String> info = ((ARMLLVMConditionalBranch) inst).getNewBranchInstAndBadLabel();
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
                for (ARMPhi phi : cur.getPhis()) {
                    phi.removeLabel(block.getLabel());
                }
            }
        }
    }

    private static Map<String, DefUse> generateDefUseMap(BasicBlock startBlock,
                                                         List<Declaration> params,
                                                         Map<String, ConstValue> valueByRegister,
                                                         Set<ARMRegister> ssaWorklist) {
        Map<String, DefUse> defUseMap = new HashMap<>();
        for (Declaration param : params) {
            defUseMap.put("%" + param.getName(),new DefUse(null));
            valueByRegister.put("%" + param.getName(), new Bottom());
            ssaWorklist.add(new ARMRegister(param.getType(), "%" + param.getName()));
        }
        Queue<BasicBlock> visited = new ArrayDeque<>();
        Queue<BasicBlock> toVisit = new ArrayDeque<>();
        toVisit.add(startBlock);
        List<ARMPhi> phis = new ArrayList<>();
        while (!toVisit.isEmpty()) {
            BasicBlock cur = toVisit.poll();
            visited.add(cur);
            for (BasicBlock succ : cur.getSuccessors()) {
                if (!visited.contains(succ)) {
                    toVisit.add(succ);
                }
            }

            for (ARMPhi phi : cur.getPhis()) {
                ARMRegister def = phi.getDefRegister();
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
            for (ARMInstruction inst : cur.getInstructions()) {
                ARMRegister def = inst.getDefRegister();
                if (def != null) {
                    defUseMap.put(def.toString(), new DefUse(inst));
                }
                for (ARMRegister use : inst.getUseRegisters()) {
                    if (use.toString().charAt(0) != '@' && defUseMap.containsKey(use.toString())) {
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
        for (ARMPhi phi : phis) {
            for (ARMRegister use : phi.getUseRegisters()) {
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
