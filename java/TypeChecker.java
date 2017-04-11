import ast.*;
import ast.Statement;

import java.util.*;
import java.util.stream.Collectors;

public class TypeChecker {
    public static void checkSemantics(Program program) throws TypeCheckerException {
        checkRedeclarations(program);
        checkMainArgsAndReturn(program.getFuncs());
        checkStructScope(program.getTypes());
        checkFunctionScope(program.getFuncs());
        typeCheck(program);
    }

    public static void checkRedeclarations(Program program) throws TypeCheckerException {
        checkStructRedeclarations(program.getTypes());
        checkStructFieldRedeclarations(program.getTypes());
        checkGlobalRedeclarations(program.getDecls());
        checkFunctionRedeclaration(program.getFuncs());
        checkParamRedeclaration(program.getFuncs());
        checkLocalRedeclaration(program.getFuncs());
        checkParamLocalRedeclaration(program.getFuncs());
    }

    private static void checkStructRedeclarations(List<TypeDeclaration> types) throws TypeCheckerException {
        Set<String> typeNames = new HashSet<>();
        for (TypeDeclaration type : types) {
            if (typeNames.contains(type.getName())) {
                throw new TypeCheckerException(
                        String.format("Struct already defined: %s\nAt line: %d", type.getName(), type.getLineNum()));
            }
            typeNames.add(type.getName());
        }
    }

    private static void checkStructFieldRedeclarations(List<TypeDeclaration> types) throws TypeCheckerException {
        for (TypeDeclaration type : types) {
            Set<String> typeNames = new HashSet<>();
            for (Declaration decl : type.getFields()) {
                if (typeNames.contains(decl.getName())) {
                    throw new TypeCheckerException(
                            String.format("Struct (%s) field already defined: %s\nAt line: %d",
                                    type.getName(), decl.getName(), decl.getLineNum()));
                }
                typeNames.add(decl.getName());
            }
        }
    }

    private static void checkGlobalRedeclarations(List<Declaration> decls) throws TypeCheckerException {
        Set<String> varNames = new HashSet<>();
        for (Declaration decl : decls) {
            if (varNames.contains(decl.getName())) {
                throw new TypeCheckerException(
                        String.format("Global var already defined: %s\nAt line: %d", decl.getName(), decl.getLineNum()));
            }
            varNames.add(decl.getName());
        }
    }

    private static void checkFunctionRedeclaration(List<Function> funs) throws TypeCheckerException {
        Set<String> functions = new HashSet<>();
        for (Function f : funs) {
            if (functions.contains(f.getName())) {
                throw new TypeCheckerException(
                        String.format("Function already defined: %s\nAt line: %d", f.getName(), f.getLineNum()));
            }
            functions.add(f.getName());
        }
    }

    private static void checkParamRedeclaration(List<Function> funs) throws TypeCheckerException {
        for (Function f : funs) {
            Set<String> params = new HashSet<>();
            for (Declaration decl : f.getParams()) {
                if (params.contains(decl.getName())) {
                    throw new TypeCheckerException(
                            String.format("Function (%s) already defined parameter: %s\nAt line: %d",
                                    f.getName(), decl.getName(), decl.getLineNum()));

                }
                params.add(decl.getName());
            }
        }
    }

    private static void checkLocalRedeclaration(List<Function> funs) throws TypeCheckerException {
        for (Function f : funs) {
            Set<String> locals = new HashSet<>();
            for (Declaration decl : f.getLocals()) {
                if (locals.contains(decl.getName())) {
                    throw new TypeCheckerException(
                            String.format("Function (%s) already defined local: %s\nAt line: %d",
                                    f.getName(), decl.getName(), decl.getLineNum()));

                }
                locals.add(decl.getName());
            }
        }
    }

    private static void checkParamLocalRedeclaration(List<Function> funs) throws TypeCheckerException {
        for (Function f : funs) {
            Set<String> params = f.getParams().stream()
                    .map(Declaration::getName)
                    .collect(Collectors.toSet());

            for (Declaration decl : f.getLocals()) {
                if (params.contains(decl.getName())) {
                    throw new TypeCheckerException(
                            String.format("Function (%s) already defined param with name: %s\nAt line: %d",
                                    f.getName(), decl.getName(), decl.getLineNum()));
                }
            }
        }
    }

    public static void checkMainArgsAndReturn(List<Function> funs) throws TypeCheckerException {
        for (Function f : funs) {
            if ("main".equals(f.getName())) {
                if (!(f.getRetType() instanceof IntType)) {
                    throw new TypeCheckerException(
                            String.format("Main function has incorrect return type.\n\tExpected : IntType\n\tActual: %s\nAt line: %d",
                            f.getRetType().getClass().toString(), f.getLineNum()));
                } else if (f.getParams().size() != 0) {
                    throw new TypeCheckerException(
                            String.format("Main function should accept 0 arguments\nAt line: %d", f.getLineNum()));
                }
                return;
            }
        }

        throw new TypeCheckerException(String.format("Main function is missing."));
    }

    public static void checkStructScope(List<TypeDeclaration> structDefs) throws TypeCheckerException {
        Set<String> validTypes = new HashSet<String>() {{
            add("int");
            add("bool");
        }};

        for (TypeDeclaration struct : structDefs) {
            validTypes.add(struct.getName());
            for (Declaration field : struct.getFields()) {
                if (!validTypes.contains(field.getType().toTypeString())) {
                    throw new TypeCheckerException(
                            String.format("Struct (%s) declares invalid field: %s\nAt line: %d",
                                    struct.getName(), field.getType().toString(), field.getLineNum()));
                }
            }
        }
    }

    public static void checkFunctionScope(List<Function> funs) throws TypeCheckerException {
        Set<String> validFuns = new HashSet<>();

        for (Function f : funs) {
            validFuns.add(f.getName());
            verifyFunCalls(f.getBody(), validFuns);
        }
    }

    private static void verifyFunCalls(Statement stmt, Set<String> funs) throws TypeCheckerException {
        if (stmt instanceof AssignmentStatement) {
            verifyFunCallExpr(((AssignmentStatement) stmt).getSource(), funs);
            Lvalue target = ((AssignmentStatement) stmt).getTarget();
            if (target instanceof LvalueDot) {
                verifyFunCallExpr(((LvalueDot) target).getLeft(), funs);
            }
        } else if (stmt instanceof BlockStatement) {
            for (Statement s : ((BlockStatement) stmt).getStatements()) {
                verifyFunCalls(s, funs);
            }
        } else if (stmt instanceof ConditionalStatement) {
            verifyFunCallExpr(((ConditionalStatement) stmt).getGuard(), funs);
            verifyFunCalls(((ConditionalStatement) stmt).getThenBlock(), funs);
            verifyFunCalls(((ConditionalStatement) stmt).getElseBlock(), funs);
        } else if (stmt instanceof DeleteStatement) {
            verifyFunCallExpr(((DeleteStatement) stmt).getExpression(), funs);
        } else if (stmt instanceof InvocationExpression) {
            for (Expression e : ((InvocationExpression) stmt).getArguments()) {
                if (!funs.contains(((InvocationExpression) stmt).getName())) {
                    throw new TypeCheckerException(
                            String.format("Invalid function invocation: %s\nAt line: %d",
                                    ((InvocationExpression) stmt).getName(),
                                    ((InvocationExpression) stmt).getLineNum()));
                }
                verifyFunCallExpr(e, funs);
            }
        } else if (stmt instanceof PrintLnStatement) {
            verifyFunCallExpr(((PrintLnStatement) stmt).getExpression(), funs);
        } else if (stmt instanceof PrintStatement) {
            verifyFunCallExpr(((PrintStatement) stmt).getExpression(), funs);
        } else if (stmt instanceof ReturnStatement) {
            verifyFunCallExpr(((ReturnStatement) stmt).getExpression(), funs);
        } else if (stmt instanceof WhileStatement) {
            verifyFunCallExpr(((WhileStatement) stmt).getGuard(), funs);
            verifyFunCalls(((WhileStatement) stmt).getBody(), funs);
        }
    }

    private static void verifyFunCallExpr(Expression expr, Set<String> funs) throws TypeCheckerException {
        if (expr instanceof InvocationExpression) {
            if (!funs.contains(((InvocationExpression) expr).getName())) {
                throw new TypeCheckerException(
                        String.format("Invalid function call: %s\nAt line: %d",
                                ((InvocationExpression) expr).getName(), ((InvocationExpression) expr).getLineNum()));
            }
        } else if (expr instanceof BinaryExpression) {
            verifyFunCallExpr(((BinaryExpression) expr).getLeft(), funs);
            verifyFunCallExpr(((BinaryExpression) expr).getRight(), funs);
        } else if (expr instanceof DotExpression) {
            verifyFunCallExpr(((DotExpression) expr).getLeft(), funs);
        } else if (expr instanceof UnaryExpression) {
            verifyFunCallExpr(((UnaryExpression) expr).getOperand(), funs);
        }
    }

    public static void typeCheck(Program program) throws TypeCheckerException {
        Map<String, Type> symbolTable = new HashMap<>();
        Map<String, List<Type>> argTypesByFunName = new HashMap<>();
        for (Declaration global : program.getDecls()) {
            symbolTable.put(global.getName(), global.getType());
        }
        for (Function f : program.getFuncs()) {
            symbolTable.put(f.getName(), f.getRetType());
            argTypesByFunName.put(f.getName(), f.getParams().stream()
                    .map(Declaration::getType)
                    .collect(Collectors.toList()));
        }

        Map<String, Map<String, Type>> structTable = new HashMap<>();
        for (TypeDeclaration td : program.getTypes()) {
            Map<String, Type> typeByField = td.getFields().stream()
                    .collect(Collectors.toMap(Declaration::getName, Declaration::getType));
            structTable.put(td.getName(), typeByField);
        }

        for (Function f : program.getFuncs()) {
            Map<String, Type> symbolTableWithParamsAndLocals = new HashMap<>(symbolTable);
            for (Declaration decl : f.getParams()) {
                symbolTableWithParamsAndLocals.put(decl.getName(), decl.getType());
            }
            for (Declaration decl : f.getLocals()) {
                symbolTableWithParamsAndLocals.put(decl.getName(), decl.getType());
            }
            typeCheck(f, symbolTableWithParamsAndLocals, structTable, argTypesByFunName);
        }
    }

    private static Type typeCheck(Function f,
                                  Map<String, Type> symbolTable,
                                  Map<String, Map<String, Type>> structTable,
                                  Map<String, List<Type>> argTypesByFunName) throws TypeCheckerException {
        Type retType = typeCheck(f.getBody(), symbolTable, structTable, argTypesByFunName);
        if (!f.getRetType().equals(retType)) {
            throw new TypeCheckerException(
                    String.format("Invalid return type: %s\n\tExpected: %s\n\tActual: %s\nAt line %d",
                            f.getName(), f.getRetType().toString(), retType.toString(), f.getLineNum())
            );
        }
        return retType;
    }

    private static Type typeCheck(Statement stmt,
                                  Map<String, Type> symbolTable,
                                  Map<String, Map<String, Type>> structTable,
                                  Map<String, List<Type>> argTypesByFunName) throws TypeCheckerException {
        if (stmt instanceof AssignmentStatement) {
            Type target = typeCheck(((AssignmentStatement) stmt).getTarget(), symbolTable, structTable, argTypesByFunName);
            Type source = typeCheck(((AssignmentStatement) stmt).getSource(), symbolTable, structTable, argTypesByFunName);
            if (!target.equals(source)) {
                throw new TypeCheckerException(
                        String.format("Invalid assignment.\n\tTarget: %s\n\tSource: %s\nAt line: %d",
                                target.toString(), source.toString(), ((AssignmentStatement) stmt).getLineNum())
                );
            }
            return new EmptyType();
        } else if (stmt instanceof BlockStatement) {
            for (Statement s : ((BlockStatement) stmt).getStatements()) {
                Type t = typeCheck(s, symbolTable, structTable, argTypesByFunName);
                if (!(t instanceof EmptyType)) {
                   return t; // type check unreachable code?
                }
            }
            return new EmptyType();
        } else if (stmt instanceof ConditionalStatement) {
            Type guard = typeCheck(((ConditionalStatement) stmt).getGuard(), symbolTable, structTable, argTypesByFunName);
            if (!(guard instanceof BoolType)) {
                throw new TypeCheckerException(
                        String.format("Invalid guard expression\n\tExpected: BoolType\n\tActual: %s\nAT line: %d",
                                guard.toString(), ((ConditionalStatement) stmt).getLineNum())
                );
            }
            Type then = typeCheck(((ConditionalStatement) stmt).getThenBlock(), symbolTable, structTable, argTypesByFunName);
            Type els = typeCheck(((ConditionalStatement) stmt).getElseBlock(), symbolTable, structTable, argTypesByFunName);
            if (then instanceof EmptyType || els instanceof EmptyType) {
                return new EmptyType();
            } else if (!then.equals(els)){
                throw new TypeCheckerException(
                        String.format("Conditional has multiple returns of different types.\n\tThen: %s\n\tElse: %s\nAt line: %d",
                                then.toString(), els.toString(), ((ConditionalStatement) stmt).getLineNum())
                );
            } else {
                return then;
            }
        } else if (stmt instanceof DeleteStatement) {
            Type t = typeCheck(((DeleteStatement) stmt).getExpression(), symbolTable, structTable, argTypesByFunName);
            if (!(t instanceof StructType)) {
                throw new TypeCheckerException(
                        String.format("Invalid delete statement.\n\tExpected: StructType\n\tActual: %s\nAt line: %d",
                                t.toString(), ((DeleteStatement) stmt).getLineNum())
                );
            }
            return new EmptyType();
        } else if (stmt instanceof InvocationStatement) {
            typeCheck(((InvocationStatement) stmt).getExpression(), symbolTable, structTable, argTypesByFunName);
            return new EmptyType();
        } else if (stmt instanceof PrintLnStatement) {
            Type t = typeCheck(((PrintLnStatement) stmt).getExpression(), symbolTable, structTable, argTypesByFunName);
            if (!(t instanceof IntType)) {
                throw new TypeCheckerException(
                        String.format("Invalid println.\n\tExpected: IntType\n\tActual: %s\nAt line: %d",
                                t.toString(), ((PrintLnStatement) stmt).getLineNum())
                );
            }
            return new EmptyType();
        } else if (stmt instanceof PrintStatement) {
            Type t = typeCheck(((PrintStatement) stmt).getExpression(), symbolTable, structTable, argTypesByFunName);
            if (!(t instanceof IntType)) {
                throw new TypeCheckerException(
                        String.format("Invalid print.\n\tExpected: IntType\n\tActual: %s\nAt line: %d",
                                t.toString(), ((PrintStatement) stmt).getLineNum())
                );
            }
            return new EmptyType();
        } else if (stmt instanceof ReturnStatement) {
            return typeCheck(((ReturnStatement) stmt).getExpression(), symbolTable, structTable, argTypesByFunName);
        } else if (stmt instanceof ReturnEmptyStatement) {
            return new VoidType();
        } else if (stmt instanceof WhileStatement) {
            Type guard = typeCheck(((WhileStatement) stmt).getGuard(), symbolTable, structTable, argTypesByFunName);
            if (!(guard instanceof BoolType)) {
                throw new TypeCheckerException(
                        String.format("Invalid guard expression\n\tExpected: BoolType\n\tActual: %s\nAT line: %d",
                                guard.toString(), ((WhileStatement) stmt).getLineNum())
                );
            }
            return new EmptyType();
        } else {
            throw new TypeCheckerException("Invalid Statement.");
        }
    }

    private static Type typeCheck(Expression expr,
                                  Map<String, Type> symbolTable,
                                  Map<String, Map<String, Type>> structTable,
                                  Map<String, List<Type>> argTypesByFunName) throws TypeCheckerException {
        if (expr instanceof BinaryExpression) {
            Type left = typeCheck(((BinaryExpression) expr).getLeft(), symbolTable, structTable, argTypesByFunName);
            Type right = typeCheck(((BinaryExpression) expr).getRight(), symbolTable, structTable, argTypesByFunName);
            switch (((BinaryExpression) expr).getOperator()) {
                case TIMES:
                case DIVIDE:
                case PLUS:
                case MINUS:
                    if (!(left instanceof IntType)) {
                        throw new TypeCheckerException(
                                String.format("Invalid binary left expression\n\tExpected: IntType\n\tReceived: %s\nAt line: %d",
                                        left.toString(), ((BinaryExpression) expr).getLineNum()));
                    }
                    if (!(right instanceof IntType)) {
                        throw new TypeCheckerException(
                                String.format("Invalid binary right expression\n\tExpected: IntType\n\tReceived: %s\nAt line: %d",
                                        right.toString(), ((BinaryExpression) expr).getLineNum()));
                    }
                    return new IntType();
                case LT:
                case GT:
                case LE:
                case GE:
                    if (!(left instanceof IntType)) {
                        throw new TypeCheckerException(
                                String.format("Invalid binary left expression\n\tExpected: IntType\n\tReceived: %s\nAt line: %d",
                                left.toString(), ((BinaryExpression) expr).getLineNum()));
                    }
                    if (!(right instanceof IntType)) {
                        throw new TypeCheckerException(
                                String.format("Invalid binary right expression\n\tExpected: IntType\n\tReceived: %s\nAt line: %d",
                                        right.toString(), ((BinaryExpression) expr).getLineNum()));
                    }
                    return new BoolType();
                case EQ:
                case NE:
                    if (!((left instanceof IntType && left.equals(right))
                            || (left instanceof StructType && left.equals(right)))) {
                        throw new TypeCheckerException(
                                String.format("Invalid binary eq/ne expression\n\tleft: %s\n\tright: %s\nAt line: %d",
                                        left.toString(), right.toString(), ((BinaryExpression) expr).getLineNum()));
                    }
                    return new BoolType();
                case AND:
                case OR:
                    if (!(left instanceof BoolType)) {
                        throw new TypeCheckerException(
                                String.format("Invalid binary left expression\n\tExpected: BoolType\n\tReceived: %s\nAt line: %d",
                                        left.toString(), ((BinaryExpression) expr).getLineNum())
                        );
                    }
                    if (!(right instanceof BoolType)) {
                        throw new TypeCheckerException(
                                String.format("Invalid binary right expression\n\tExpected: BoolType\n\tReceived: %s\nAt line: %d",
                                        right.toString(), ((BinaryExpression) expr).getLineNum())
                        );
                    }
                    return new BoolType();
                default:
                    throw new TypeCheckerException(String.format("Invalid binary operand: %s\nAt line: %d",
                            ((BinaryExpression) expr).getOperator(), ((BinaryExpression) expr).getLineNum()));
            }
        } else if (expr instanceof DotExpression) {
            Type left = typeCheck(((DotExpression) expr).getLeft(), symbolTable, structTable, argTypesByFunName);
            if (!(left instanceof StructType)) {
                throw new TypeCheckerException(
                        String.format("Dot invalid expression.\n\tExpected: StructType\n\tReceived: %s\nAt line: %d",
                                left.toString(), ((DotExpression) expr).getLineNum())
                );
            } else {
                if (!structTable.containsKey(((StructType) left).getName())) {
                    throw new TypeCheckerException(
                            String.format("Dot invalid struct: %s\nAt line: %d",
                                    ((StructType) left).getName(), ((DotExpression) expr).getLineNum())
                    );
                } else {
                    Map<String, Type> fields = structTable.get(((StructType) left).getName());
                    if (!fields.containsKey(((DotExpression) expr).getId())) {
                        throw new TypeCheckerException(
                                String.format("Dot %s does not contain field: %s\nAt line: %d",
                                        left.toString(), ((DotExpression) expr).getId(), ((DotExpression) expr).getLineNum()));
                    } else {
                        return fields.get(((DotExpression) expr).getId());
                    }
                }
            }
        } else if (expr instanceof UnaryExpression) {
            Type operand = typeCheck(((UnaryExpression) expr).getOperand(), symbolTable, structTable, argTypesByFunName);
            if (UnaryExpression.Operator.NOT.equals(((UnaryExpression) expr).getOperator())) {
                if (!(operand instanceof BoolType)) {
                    throw new TypeCheckerException(
                            String.format("Invalid unary operand.\n\tExpected: BoolType\n\tReceived: %s\nAt line: %d",
                                    operand.toString(), ((UnaryExpression) expr).getLineNum())
                    );
                } else {
                    return new BoolType();
                }
            } else if (UnaryExpression.Operator.MINUS.equals(((UnaryExpression) expr).getOperator())) {
                if (!(operand instanceof IntType)) {
                    throw new TypeCheckerException(
                            String.format("Invalid unary operand.\n\tExpected: IntType\n\tReceived: %s\nAt line: %d",
                                    operand.toString(), ((UnaryExpression) expr).getLineNum())
                    );
                } else {
                    return new IntType();
                }
            } else {
                throw new TypeCheckerException(String.format("Invalid unary operator: %s\nAt line: %d",
                        ((UnaryExpression) expr).getOperator(), ((UnaryExpression) expr).getLineNum()));
            }
        } else if (expr instanceof FalseExpression || expr instanceof TrueExpression) {
            return new BoolType();
        } else if (expr instanceof IdentifierExpression) {
            if (!symbolTable.containsKey(((IdentifierExpression) expr).getId())) {
                throw new TypeCheckerException(
                        String.format("Identifier does not exist: %s\nAtLine: %d",
                                ((IdentifierExpression) expr).getId(), ((IdentifierExpression) expr).getLineNum()));
            } else {
                return symbolTable.get(((IdentifierExpression) expr).getId());
            }
        } else if (expr instanceof IntegerExpression) {
            return new IntType();
        } else if (expr instanceof InvocationExpression) {
            if (!symbolTable.containsKey(((InvocationExpression) expr).getName())) {
                throw new TypeCheckerException(
                        String.format("Function does not exist: %s\nAtLine: %d",
                                ((InvocationExpression) expr).getName(), ((InvocationExpression) expr).getLineNum()));
            }

            List<Type> args = new ArrayList<>();
            for (Expression e : ((InvocationExpression) expr).getArguments()) {
                args.add(typeCheck(e, symbolTable, structTable, argTypesByFunName));
            }
            List<Type> params = argTypesByFunName.get(((InvocationExpression) expr).getName());
            if (params.size() != args.size()) {
                throw new TypeCheckerException(
                        String.format("Invalid number of arguments for: %s\n\tExpected: %d\n\tReceived: %d\nAt line: %d",
                                ((InvocationExpression) expr).getName(), params.size(), args.size(), ((InvocationExpression) expr).getLineNum()));
            }
            for (int i = 0; i < params.size(); ++i) {
                if (!params.get(i).equals(args.get(i))) {
                    throw new TypeCheckerException(
                            String.format("Invalid arg type, argument #%d\n\tExpected: %s\n\tReceived: %s\nAt line: %d",
                            i, params.get(i).toString(), args.get(i).toString(), ((InvocationExpression) expr).getLineNum())
                    );
                }
            }

            return symbolTable.get(((InvocationExpression) expr).getName());
        } else if (expr instanceof NewExpression) {
            if (!structTable.containsKey(((NewExpression) expr).getId())) {
                throw new TypeCheckerException(String.format("Undefined StructType: %s\nAt line: %d",
                        ((NewExpression) expr).getId(), ((NewExpression) expr).getLineNum()));
            }
            return new StructType(((NewExpression) expr).getLineNum(), ((NewExpression) expr).getId());
        } else if (expr instanceof NullExpression) {
            return new StructType(((NullExpression) expr).getLineNum(), StructType.NULL);
        } else if (expr instanceof ReadExpression) {
            return new IntType();
        } else {
            throw new TypeCheckerException("Invalid expression type");
        }
    }

    private static Type typeCheck(Lvalue lval,
                                  Map<String, Type> symbolTable,
                                  Map<String, Map<String, Type>> structTable,
                                  Map<String, List<Type>> argTypesByFunName) throws TypeCheckerException {
        if (lval instanceof LvalueId) {
            if (!symbolTable.containsKey(((LvalueId) lval).getId())) {
                throw new TypeCheckerException(
                        String.format("Lvalue id does not exist: %s\nAt line: %d",
                                ((LvalueId) lval).getId(), ((LvalueId) lval).getLineNum())
                );
            } else {
                return symbolTable.get(((LvalueId) lval).getId());
            }
        } else if (lval instanceof LvalueDot) {
            Type left = typeCheck(((LvalueDot) lval).getLeft(), symbolTable, structTable, argTypesByFunName);
            if (!(left instanceof StructType)) {
                throw new TypeCheckerException(
                        String.format("LvalueDot invalid expression.\n\tExpected: StructType\n\tReceived: %s\nAt line: %d",
                                left.toString(), ((LvalueDot) lval).getLineNum())
                );
            } else {
                if (!structTable.containsKey(((StructType) left).getName())) {
                    throw new TypeCheckerException(
                            String.format("LvalueDot invalid struct: %s\nAt line: %d",
                                    ((StructType) left).getName(), ((LvalueDot) lval).getLineNum())
                    );
                } else {
                    Map<String, Type> fields = structTable.get(((StructType) left).getName());
                    if (!fields.containsKey(((LvalueDot) lval).getId())) {
                        throw new TypeCheckerException(
                                String.format("LvalueDot %s does not contain field: %s\nAt line: %d",
                                        left.toString(), ((LvalueDot) lval).getId(), ((LvalueDot) lval).getLineNum()));
                    } else {
                        return fields.get(((LvalueDot) lval).getId());
                    }
                }
            }
        } else {
            throw new TypeCheckerException("Invalid Lvalue type");
        }
    }

    public static class TypeCheckerException extends Exception {
        public TypeCheckerException(String exc) {
            super(exc);
        }
    }
}
