package cfg.llvm;

import ast.Type;
import cfg.Value;

/**
 * Created by Brad on 4/14/2017.
 */
public class LLVMBoolOp implements LLVMInstruction {
    private final Operator operator;
    private final Type type;
    private final LLVMRegister result;
    private final Value op1, op2;

    public LLVMBoolOp(Operator op, Type type, LLVMRegister result, Value op1, Value op2) {
        this.operator = op;
        this.type = type;
        this.result = result;
        this.op1 = op1;
        this.op2 = op2;
    }

    public enum Operator {
        AND("and"),
        OR("or"),
        XOR("xor");

        private String name;
        Operator(String name) {
            this.name = name;
        }
        public String getName() {
            return this.name;
        }
    }

    public String toString() {
        return String.format("%s = %s i1 %s, %s",
                             result.toString(),
                             operator.getName(),
                             op1.toString(),
                             op2.toString());
    }
}
