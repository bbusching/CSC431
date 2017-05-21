package cfg.llvm;

import ast.Type;
import cfg.Value;
import constprop.Bottom;
import constprop.ConstImm;
import constprop.ConstValue;
import constprop.Top;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @Override
    public LLVMRegister getDefRegister() {
        return result;
    }

    @Override
    public List<LLVMRegister> getUseRegisters() {
        List<LLVMRegister> uses = new ArrayList<>();
        if (op1 instanceof LLVMRegister) {
            uses.add((LLVMRegister) op1);
        }
        if (op2 instanceof LLVMRegister) {
            uses.add((LLVMRegister) op2);
        }
        return uses;
    }

    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        if (op1 instanceof LLVMImmediate && op2 instanceof LLVMImmediate) {
            switch (operator) {
                case LT:
                    return new ConstImm(((LLVMImmediate) op1).getVal() < ((LLVMImmediate) op2).getVal() ? 1 : 0);
                case LE:
                    return new ConstImm(((LLVMImmediate) op1).getVal() <= ((LLVMImmediate) op2).getVal() ? 1 : 0);
                case GT:
                    return new ConstImm(((LLVMImmediate) op1).getVal() > ((LLVMImmediate) op2).getVal() ? 1 : 0);
                case GE:
                    return new ConstImm(((LLVMImmediate) op1).getVal() >= ((LLVMImmediate) op2).getVal() ? 1 : 0);
                case EQ:
                    return new ConstImm(((LLVMImmediate) op1).getVal() == ((LLVMImmediate) op2).getVal() ? 1 : 0);
                case NE:
                    return new ConstImm(((LLVMImmediate) op1).getVal() != ((LLVMImmediate) op2).getVal() ? 1 : 0);
                default:
                    throw new RuntimeException("Shouldn't be here");
            }
        } else {
            return new Top();
        }
    }

    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        ConstValue cv1;
        ConstValue cv2;
        if (op1 instanceof LLVMImmediate) {
            cv1 = new ConstImm(((LLVMImmediate) op1).getVal());
        } else {
            cv1 = valueByRegister.get(op1.toString());
        }
        if (op2 instanceof LLVMImmediate) {
            cv2 = new ConstImm(((LLVMImmediate) op2).getVal());
        } else {
            cv2 = valueByRegister.get(op2.toString());
        }

        if (cv1 instanceof Bottom || cv2 instanceof Bottom) {
            return new Bottom();
        } else if (cv1 instanceof Top || cv2 instanceof Top) {
            return new Top();
        } else {
            switch (operator) {
                case LT:
                    return new ConstImm(((ConstImm) cv1).getVal() < ((ConstImm) cv2).getVal() ? 1 : 0);
                case LE:
                    return new ConstImm(((ConstImm) cv1).getVal() <= ((ConstImm) cv2).getVal() ? 1 : 0);
                case GT:
                    return new ConstImm(((ConstImm) cv1).getVal() > ((ConstImm) cv2).getVal() ? 1 : 0);
                case GE:
                    return new ConstImm(((ConstImm) cv1).getVal() >= ((ConstImm) cv2).getVal() ? 1 : 0);
                case EQ:
                    return new ConstImm(((ConstImm) cv1).getVal() == ((ConstImm) cv2).getVal() ? 1 : 0);
                case NE:
                    return new ConstImm(((ConstImm) cv1).getVal() != ((ConstImm) cv2).getVal() ? 1 : 0);
                default:
                    throw new RuntimeException("Shouldn't be here");
            }
        }
    }

    public void replace(String reg, ConstImm value) {
        if (op1 instanceof LLVMRegister && op1.toString().equals(reg)) {
            op1 = new LLVMImmediate(value.getVal());
        }
        if (op2 instanceof LLVMRegister && op2.toString().equals(reg)) {
            op2 = new LLVMImmediate(value.getVal());
        }
    }
}
