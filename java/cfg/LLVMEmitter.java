package cfg;

import ast.*;
import cfg.llvm.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class LLVMEmitter {
    public static void writeLlvm(Program prog, String filename) {
        try (PrintWriter pw = new PrintWriter(new File(filename))) {
            writeConstants(pw);
            writeStructs(prog.getTypes(), pw);
            writeGlobals(prog.getDecls(), pw);
            for (Function f : prog.getFuncs()) {
                writeFunction(f, pw, createSymbolTable(prog), createStructTable(prog.getTypes()), createArgTypesByFun(prog.getFuncs()));
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
                                      Map<String, List<Type>> argTypesByFun) {
        BasicBlock startBlock = new BasicBlock();

        // Write function header
        pw.print(String.format("define %s @%s(", f.getRetType().toLlvmType(), f.getName()));
        for (int i = 0; i < f.getParams().size(); ++i) {
            symbolTable.put(f.getParams().get(i).getName(),
                            new Pair<>(f.getParams().get(i).getType(), VariableScope.PARAM));
            pw.print(String.format("%s %%_param_%s",
                                   f.getParams().get(i).getType().toLlvmType(), f.getParams().get(i).getName()));
            LLVMRegister param = new LLVMRegister(f.getParams().get(i).getType(), "%" + f.getParams().get(i).getName());
            startBlock.addInstruction(new LLVMAlloc(param.getType(), param));
            startBlock.addInstruction(
                    new LLVMStore(param.getType(),
                                  new LLVMRegister(param.getType(), "%_param_" + f.getParams().get(i).getName()),
                                  param));
            if (i != f.getParams().size() - 1) {
                pw.print(", ");
            }
        }
        pw.println(") {");


        LLVMRegister retval = null;
        if (!(f.getRetType() instanceof VoidType)) {
            retval = new LLVMRegister(f.getRetType(), "%_retval_");
            startBlock.addInstruction(new LLVMAlloc(f.getRetType(), retval));
        }

        // Write locals
        for (Declaration local : f.getLocals()) {
            startBlock.addInstruction(new LLVMAlloc(local.getType(), new LLVMRegister(local.getType(),
                                                                                      "%" + local.getName())));
            symbolTable.put(local.getName(), new Pair<>(local.getType(), VariableScope.LOCAL));
        }

        BasicBlock returnBlock = new BasicBlock();
        BasicBlock returned = writeStatement(f.getBody(), startBlock, returnBlock, retval, symbolTable, structTable, argTypesByFun);
        if (returned != returnBlock) {
            returned.addSuccessor(returnBlock);
            returned.addInstruction(new LLVMJump(returnBlock.getLabel()));
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

        if (!(f.getRetType() instanceof VoidType)) {
            LLVMRegister tempRet = new LLVMRegister(f.getRetType());
            returnBlock.addInstruction(new LLVMLoad(f.getRetType(), tempRet, retval));
            returnBlock.addInstruction(new LLVMReturn(f.getRetType(), tempRet));
        } else {
            returnBlock.addInstruction(new LLVMReturnVoid());
        }
        returnBlock.writeBlock(pw);

        pw.println("}\n");
    }

    private static BasicBlock writeStatement(Statement stmt,
                                             BasicBlock currentBlock,
                                             BasicBlock returnBlock,
                                             LLVMRegister retval,
                                             Map<String, Pair<Type, VariableScope>> symbolTable,
                                             Map<String, Map<String, Pair<Integer, Type>>> structTable,
                                             Map<String, List<Type>> argTypesByFun) {
        if (stmt instanceof AssignmentStatement) {
            LLVMRegister target = writeLval(((AssignmentStatement) stmt).getTarget(), currentBlock, symbolTable,
                                            structTable, argTypesByFun);
            Value source = writeExpr(((AssignmentStatement) stmt).getSource(), currentBlock, symbolTable, structTable, argTypesByFun);
            currentBlock.addInstruction(new LLVMStore(target.getType(), source, target));
            return currentBlock;
        } else if (stmt instanceof BlockStatement) {
            BasicBlock b = currentBlock;
            for (Statement s : ((BlockStatement) stmt).getStatements()) {
                b = writeStatement(s, b, returnBlock, retval, symbolTable, structTable, argTypesByFun);
                if (b == returnBlock) {
                    return returnBlock;
                }
            }
            return b;
        } else if (stmt instanceof ConditionalStatement) {
            Value guard = writeExpr(((ConditionalStatement) stmt).getGuard(), currentBlock, symbolTable, structTable, argTypesByFun);
            BasicBlock thenBlock = new BasicBlock();
            BasicBlock then = writeStatement(((ConditionalStatement) stmt).getThenBlock(),
                                             thenBlock,
                                             returnBlock,
                                             retval,
                                             symbolTable,
                                             structTable,
                                             argTypesByFun);
            BasicBlock elseBlock = new BasicBlock();
            BasicBlock els = writeStatement(((ConditionalStatement) stmt).getElseBlock(),
                                            elseBlock,
                                            returnBlock,
                                            retval,
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
                if (elseBlock.isEmpty()) {
                    then.addSuccessor(els);
                    then.addInstruction(new LLVMJump(elseBlock.getLabel()));
                    return els;
                } else {
                    BasicBlock join = new BasicBlock();
                    then.addInstruction(new LLVMJump(join.getLabel()));
                    then.addSuccessor(join);
                    els.addInstruction(new LLVMJump(join.getLabel()));
                    els.addSuccessor(join);
                    return join;
                }
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
            currentBlock.addInstruction(new LLVMStore(retval.getType(), val, retval));
            currentBlock.addInstruction(new LLVMJump(returnBlock.getLabel()));
            return returnBlock;
        } else if (stmt instanceof ReturnEmptyStatement) {
            currentBlock.addSuccessor(returnBlock);
            currentBlock.addInstruction(new LLVMJump(returnBlock.getLabel()));
            return returnBlock;
        } else if (stmt instanceof WhileStatement) {
            Value guard = writeExpr(((WhileStatement) stmt).getGuard(), currentBlock, symbolTable, structTable, argTypesByFun);
            BasicBlock body = new BasicBlock();
            BasicBlock join = new BasicBlock();
            currentBlock.addInstruction(new LLVMConditionalBranch(guard, body.getLabel(), join.getLabel()));
            BasicBlock returned = writeStatement(((WhileStatement) stmt).getBody(), body, returnBlock, retval,
                                                 symbolTable, structTable, argTypesByFun);
            if (returned != returnBlock) {
                guard = writeExpr(((WhileStatement) stmt).getGuard(), returned, symbolTable, structTable, argTypesByFun);
                returned.addInstruction(new LLVMConditionalBranch(guard, body.getLabel(), join.getLabel()));
                returned.addSuccessor(body);
                returned.addSuccessor(join);
            }
            currentBlock.addSuccessor(body);
            currentBlock.addSuccessor(join);

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
            LLVMRegister result = new LLVMRegister(iden.getFirst());
            if (VariableScope.GLOBAL == iden.getSecond()) {
                curBlock.addInstruction(
                        new LLVMLoad(iden.getFirst(),
                                     result,
                                     new LLVMRegister(iden.getFirst(), "@" + ((IdentifierExpression) expr).getId())));
            } else if (VariableScope.LOCAL == iden.getSecond()) {
                curBlock.addInstruction(
                        new LLVMLoad(iden.getFirst(),
                                     result,
                                     new LLVMRegister(iden.getFirst(), "%" + ((IdentifierExpression) expr).getId())));
            } else if (VariableScope.PARAM == iden.getSecond()) {
                curBlock.addInstruction(
                        new LLVMLoad(iden.getFirst(),
                                     result,
                                     new LLVMRegister(iden.getFirst(), "%" + ((IdentifierExpression) expr).getId())));
            }
            return result;
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

    private static LLVMRegister writeLval(Lvalue lval,
                                          BasicBlock curBlock,
                                          Map<String, Pair<Type, VariableScope>> symbolTable,
                                          Map<String, Map<String, Pair<Integer, Type>>> structTable,
                                          Map<String, List<Type>> argTypesByFun) {
        if (lval instanceof LvalueId) {
            Pair<Type, VariableScope> id = symbolTable.get(((LvalueId) lval).getId());
            if (id.getSecond() == VariableScope.LOCAL) {
                return new LLVMRegister(id.getFirst(), "%" + ((LvalueId) lval).getId());
            } else if (id.getSecond() == VariableScope.GLOBAL) {
                return new LLVMRegister(id.getFirst(), "@" + ((LvalueId) lval).getId());
            } else if (id.getSecond() == VariableScope.PARAM) {
                return new LLVMRegister(id.getFirst(), "%" + ((LvalueId) lval).getId());
            }
        } else if (lval instanceof LvalueDot) {
            LLVMRegister left = (LLVMRegister) writeExpr(((LvalueDot) lval).getLeft(), curBlock, symbolTable,
                                                         structTable, argTypesByFun);
            StructType structType = (StructType) left.getType();
            Pair<Integer, Type> field = structTable.get(structType.getName()).get(((LvalueDot) lval).getId());
            LLVMRegister result = new LLVMRegister(field.getSecond());
            curBlock.addInstruction(new LLVMLoadField(structType, result, left, field.getFirst()));
            return result;
        }
        throw new RuntimeException("Invalid Lvalue.");
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
