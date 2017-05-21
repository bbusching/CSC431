package cfg.llvm;

import constprop.Bottom;
import constprop.ConstImm;
import constprop.ConstValue;
import constprop.Top;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/22/2017.
 */
public class LLVMMalloc implements LLVMInstruction {
    private LLVMRegister result;
    private int bytes;

    public LLVMMalloc(LLVMRegister result, int bytes) {
        this.result = result;
        this.bytes = bytes;
    }

    public String toString() {
        return String.format("%s = call i8* @malloc(i64 %d)", result.toString(), bytes);
    }
    @Override
    public LLVMRegister getDefRegister() {
        return result;
    }

    @Override
    public List<LLVMRegister> getUseRegisters() {
        List<LLVMRegister> uses = new ArrayList<>();
        return uses;
    }

    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        return new Bottom();
    }

    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    public void replace(String reg, ConstImm value) {
    }
}
