package cfg.llvm;

import ast.Type;
import cfg.Value;

/**
 * Created by Brad on 4/20/2017.
 */
public class LLVMComparison implements LLVMInstruction {
    private Operator operator;
    private Type type;
    private LLVMRegister result;
    private Value op1, op2;

    public LLVMComparison(Operator operator, Type type, LLVMRegister result, Value op1, Value op2) {
        this.operator = operator;
        this.type = type;
        this.result = result;
        this.op1 = op1;
        this.op2 = op2;
    }

    public String toString() {
        return String.format("%s = icmp %s %s %s, %s",
                             result.toString(),
                             operator.getName(),
                             type.toLlvmType(),
                             op1.toString(),
                             op2.toString());
    }

    public enum Operator {
        LT("slt"),
        LE("sle"),
        GT("sgt"),
        GE("sge"),
        EQ("eq"),
        NE("ne");

        private String name;
        Operator(String name) {
            this.name = name;
        }
        public String getName() {
            return this.name;
        }
    }
}
