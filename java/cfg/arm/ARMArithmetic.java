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
 * Created by Brad on 4/14/2017.
 */
public class ARMArithmetic implements ARMInstruction {
    private final Operator operator;
    private final Type type;
    private final ARMRegister result;
    private Value op1, op2;

    public ARMArithmetic(Operator op, Type type, ARMRegister result, Value op1, Value op2) {
        this.operator = op;
        this.type = type;
        this.result = result;
        this.op1 = op1;
        this.op2 = op2;
    }

    public enum Operator {
        ADD("add"),
        MUL("mul"),
        SDIV("sdiv"),
        SUB("sub");

        private String name;
        Operator(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    public void write(PrintWriter pw) {
        if (op1 instanceof ARMImmediate) {
            op1 = ((ARMImmediate) op1).writeLoad(pw);
        }
        if (op2 instanceof ARMImmediate) {
            op2 = ((ARMImmediate) op2).writeLoad(pw);
        }
        if (Operator.SDIV.equals(operator)) {
            pw.println("\tpush {r0 - r1}");
            pw.println("\tmov r0, " + op1.toString());
            pw.println("\tmov r1, " + op2.toString());
            pw.println("\tbl __aeabi_idiv");
            pw.println("\tmov " + result.toString() + ", r0");
            pw.println("\tpop {r0 - r1}");
        } else {
            pw.println(String.format("\t%s %s, %s, %s", operator.getName(), result.toString(), op1.toString(), op2.toString()));
        }
    }

    public String toString() {
        return String.format("%s = %s %s %s, %s",
                             result.toString(),
                             operator.getName(),
                             type.toLlvmType(),
                             op1.toString(),
                             op2.toString());
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
                case ADD:
                    return new ConstImm(((ARMImmediate) op1).getVal() + ((ARMImmediate) op2).getVal());
                case SUB:
                    return new ConstImm(((ARMImmediate) op1).getVal() - ((ARMImmediate) op2).getVal());
                case MUL:
                    return new ConstImm(((ARMImmediate) op1).getVal() * ((ARMImmediate) op2).getVal());
                case SDIV:
                    if (((ARMImmediate) op2).getVal() == 0) {
                        return new Bottom();
                    } else {
                        return new ConstImm(((ARMImmediate) op1).getVal() / ((ARMImmediate) op2).getVal());
                    }
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
                case ADD:
                    return new ConstImm(((ConstImm) cv1).getVal() + ((ConstImm) cv2).getVal());
                case SUB:
                    return new ConstImm(((ConstImm) cv1).getVal() - ((ConstImm) cv2).getVal());
                case MUL:
                    return new ConstImm(((ConstImm) cv1).getVal() * ((ConstImm) cv2).getVal());
                case SDIV:
                    if (((ConstImm) cv2).getVal() == 0) {
                        return new Bottom();
                    } else {
                        return new ConstImm(((ConstImm) cv1).getVal() / ((ConstImm) cv2).getVal());
                    }
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
}
