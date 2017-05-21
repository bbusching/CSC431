package cfg.llvm;

import cfg.Value;
import constprop.ConstImm;
import constprop.ConstValue;
import constprop.Top;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/20/2017.
 */
public class LLVMBitcast implements LLVMInstruction{
    private LLVMRegister result;
    private Value val;

    public LLVMBitcast(LLVMRegister result, Value val) {
        this.result = result;
        this.val = val;
    }

    public String toString() {
        return String.format("%s = bitcast %s %s to %s",
                             result.toString(),
                             val.getType().toLlvmType(),
                             val.toString(),
                             result.getType().toLlvmType());
    }

    @Override
    public LLVMRegister getDefRegister() {
        return result;
    }

    @Override
    public List<LLVMRegister> getUseRegisters() {
        List<LLVMRegister> uses = new ArrayList<>();
        if (val instanceof LLVMRegister) {
            uses.add((LLVMRegister) val);
        }
        return uses;
    }

    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        if (val instanceof LLVMImmediate) {
            return new ConstImm(((LLVMImmediate) val).getVal());
        } else {
            return new Top();
        }
    }

    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        return valueByRegister.get(val.toString());
    }

    public void replace(String reg, ConstImm value) {
        val = new LLVMImmediate(value.getVal());
    }
}
