package cfg.arm;

import ast.Type;
import cfg.Value;
import constprop.Bottom;
import constprop.ConstImm;
import constprop.ConstValue;
import constprop.Top;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/20/2017.
 */
public class ARMComparison implements ARMInstruction {
    private Operator operator;
    private Type type;
    private ARMRegister result;
    private Value op1, op2;

    public ARMComparison(Operator operator, Type type, ARMRegister result, Value op1, Value op2) {
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
    public ARMRegister getDefRegister() {
        return result;
    }

    @Override
    public List<ARMRegister> getUseRegisters() {
        List<ARMRegister> uses = new ArrayList<>();
        if (op1 instanceof ARMRegister) {
            uses.add((ARMRegister) op1);
        }
        if (op2 instanceof ARMRegister) {
            uses.add((ARMRegister) op2);
        }
        return uses;
    }

    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        if (op1 instanceof ARMImmediate && op2 instanceof ARMImmediate) {
            switch (operator) {
                case LT:
                    return new ConstImm(((ARMImmediate) op1).getVal() < ((ARMImmediate) op2).getVal() ? 1 : 0);
                case LE:
                    return new ConstImm(((ARMImmediate) op1).getVal() <= ((ARMImmediate) op2).getVal() ? 1 : 0);
                case GT:
                    return new ConstImm(((ARMImmediate) op1).getVal() > ((ARMImmediate) op2).getVal() ? 1 : 0);
                case GE:
                    return new ConstImm(((ARMImmediate) op1).getVal() >= ((ARMImmediate) op2).getVal() ? 1 : 0);
                case EQ:
                    return new ConstImm(((ARMImmediate) op1).getVal() == ((ARMImmediate) op2).getVal() ? 1 : 0);
                case NE:
                    return new ConstImm(((ARMImmediate) op1).getVal() != ((ARMImmediate) op2).getVal() ? 1 : 0);
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
        if (op1 instanceof ARMImmediate) {
            cv1 = new ConstImm(((ARMImmediate) op1).getVal());
        } else {
            cv1 = valueByRegister.get(op1.toString());
        }
        if (op2 instanceof ARMImmediate) {
            cv2 = new ConstImm(((ARMImmediate) op2).getVal());
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
        if (op1 instanceof ARMRegister && op1.toString().equals(reg)) {
            op1 = new ARMImmediate(value.getVal());
        }
        if (op2 instanceof ARMRegister && op2.toString().equals(reg)) {
            op2 = new ARMImmediate(value.getVal());
        }
    }

    public void write(PrintWriter pw) {
        if (op1 instanceof ARMImmediate) {
            op1 = ((ARMImmediate) op1).writeLoad(pw);
        }
        if (op2 instanceof ARMImmediate) {
            op2 = ((ARMImmediate) op2).writeLoad(pw);
        }
        pw.println("\tmov " + result.toString() + ", 0");
        switch (operator) {
            case EQ:
                pw.println(String.format("\tcmp %s, %s", op1.toString(), op2.toString()));
                pw.println(String.format("\tmoveq %s, 1", result.toString()));
                return;
            case NE:
                pw.println(String.format("\tcmp %s, %s", op1.toString(), op2.toString()));
                pw.println(String.format("\tmovne %s, 1", result.toString()));
                return;
            case GE:
                pw.println(String.format("\tcmp %s, %s", op1.toString(), op2.toString()));
                pw.println(String.format("\tmovge %s, 1", result.toString()));
                return;
            case GT:
                pw.println(String.format("\tadd %s, %s, #1", op2.toString(), op2.toString()));
                pw.println(String.format("\tcmp %s, %s", op1.toString(), op2.toString()));
                pw.println(String.format("\tmovge %s, 1", result.toString()));
                return;
            case LT:
                pw.println(String.format("\tcmp %s, %s", op1.toString(), op2.toString()));
                pw.println(String.format("\tmovlt %s, 1", result.toString()));
                return;
            case LE:
                pw.println(String.format("\tadd %s, %s, #1", op2.toString(), op2.toString()));
                pw.println(String.format("\tcmp %s, %s", op1.toString(), op2.toString()));
                pw.println(String.format("\tmovlt %s, 1", result.toString()));
                return;
        }
    }
}
