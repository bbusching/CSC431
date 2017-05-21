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
 * Created by Brad on 4/14/2017.
 */
public class LLVMBoolOp implements LLVMInstruction {
    private final Operator operator;
    private final Type type;
    private final LLVMRegister result;
    private Value op1, op2;

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
                case AND:
                    return new ConstImm(((LLVMImmediate) op1).getVal() & ((LLVMImmediate) op2).getVal());
                case OR:
                    return new ConstImm(((LLVMImmediate) op1).getVal() | ((LLVMImmediate) op2).getVal());
                case XOR:
                    return new ConstImm(((LLVMImmediate) op1).getVal() ^ ((LLVMImmediate) op2).getVal());
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
                case AND:
                    return new ConstImm(((ConstImm) cv1).getVal() & ((ConstImm) cv2).getVal());
                case OR:
                    return new ConstImm(((ConstImm) cv1).getVal() | ((ConstImm) cv2).getVal());
                case XOR:
                    return new ConstImm(((ConstImm) cv1).getVal() ^ ((ConstImm) cv2).getVal());
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
