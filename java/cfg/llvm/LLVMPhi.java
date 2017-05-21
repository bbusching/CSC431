package cfg.llvm;

import cfg.Pair;
import cfg.Value;
import constprop.Bottom;
import constprop.ConstImm;
import constprop.ConstValue;
import constprop.Top;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 5/5/2017.
 */
public class LLVMPhi implements LLVMInstruction {
    private String identifier;
    private LLVMRegister result;
    private List<Pair<Value, String>> operands = new ArrayList<>();

    public LLVMPhi(String identifier, LLVMRegister result) {
        this.identifier = identifier;
        this.result = result;
    }

    public void addOperand(Pair<Value, String> operand) {
        this.operands.add(operand);
    }

    public String getIdentifier() {
        return identifier;
    }

    public LLVMRegister getResult() {
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
    public LLVMRegister getDefRegister() {
        return result;
    }

    @Override
    public List<LLVMRegister> getUseRegisters() {
        List<LLVMRegister> uses = new ArrayList<>();
        for (Pair<Value, String> pairs : operands) {
            if (pairs.getFirst() instanceof LLVMRegister) {
                uses.add((LLVMRegister) pairs.getFirst());
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
        if (v instanceof LLVMImmediate) {
            return new ConstImm(((LLVMImmediate) v).getVal());
        } else if (v instanceof LLVMRegister) {
            return valueMap.getOrDefault(((LLVMRegister) v).toString(), new Top());
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
            if (operands.get(i).getFirst() instanceof LLVMRegister && operands.get(i).getFirst().toString().equals(reg)) {
                Pair<Value, String> p = operands.remove(i);
                operands.add(i, new Pair<>(new LLVMImmediate(value.getVal()), p.getSecond()));
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
}
