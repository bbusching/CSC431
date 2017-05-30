package cfg.arm;

import cfg.Pair;
import cfg.Value;
import constprop.*;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 5/5/2017.
 */
public class ARMPhi implements ARMInstruction {
    private String identifier;
    private ARMRegister result;
    private List<Pair<Value, String>> operands = new ArrayList<>();

    public ARMPhi(String identifier, ARMRegister result) {
        this.identifier = identifier;
        this.result = result;
    }

    public void addOperand(Pair<Value, String> operand) {
        this.operands.add(operand);
    }

    public String getIdentifier() {
        return identifier;
    }

    public ARMRegister getResult() {
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(String.format("%s = phi %s ", result.toString(), result.getType().toLlvmType()));

        for (int i = 0; i < operands.size(); ++i) {
            Pair<Value, String> p = operands.get(i);
            sb.append(String.format("[ %s, %%%s]", p.getFirst().toString(), p.getSecond()));
            if (i != operands.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("\t; var " + identifier);
        return sb.toString();
    }
    @Override
    public ARMRegister getDefRegister() {
        return result;
    }

    @Override
    public List<ARMRegister> getUseRegisters() {
        List<ARMRegister> uses = new ArrayList<>();
        for (Pair<Value, String> pairs : operands) {
            if (pairs.getFirst() instanceof ARMRegister) {
                uses.add((ARMRegister) pairs.getFirst());
            }
        }
        return uses;
    }

    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        ConstValue cur = getCV(operands.get(0).getFirst(), valueByRegister);
        for (int i = 1; i < operands.size(); ++i) {
            cur = cur.join(getCV(operands.get(i).getFirst(), valueByRegister));
        }
        return cur;
    }

    private ConstValue getCV(Value v, Map<String, ConstValue> valueMap) {
        if (v instanceof ARMImmediate) {
            return new ConstImm(((ARMImmediate) v).getVal());
        } else if (v instanceof ARMNull) {
            return new ConstNull();
        } else if (v instanceof ARMRegister) {
            return valueMap.getOrDefault(((ARMRegister) v).toString(), new Top());
        } else {
            throw new RuntimeException(v.toString());
        }
    }

    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        ConstValue cur = getCV(operands.get(0).getFirst(), valueByRegister);
        for (int i = 1; i < operands.size(); ++i) {
            cur = cur.join(getCV(operands.get(i).getFirst(), valueByRegister));
        }
        return cur;
    }

    public void replace(String reg, ConstImm value) {
        for (int i = 0; i < operands.size(); ++i) {
            if (operands.get(i).getFirst() instanceof ARMRegister && operands.get(i).getFirst().toString().equals(reg)) {
                Pair<Value, String> p = operands.remove(i);
                operands.add(i, new Pair<>(new ARMImmediate(value.getVal()), p.getSecond()));
            }
        }
    }

    public void removeLabel(String label) {
        for (int i = 0; i < operands.size(); ++i) {
            if (operands.get(i).getSecond().equals(label)) {
                operands.remove(i);
            }
        }
    }

    public void write(PrintWriter pw) {}

    public List<Pair<Value, String>> getOperands() {
        return operands;
    }
}
