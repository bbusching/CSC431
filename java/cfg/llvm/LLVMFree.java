package cfg.llvm;

import constprop.ConstImm;
import constprop.ConstValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/22/2017.
 */
public class LLVMFree implements LLVMInstruction {
    private LLVMRegister register;

    public LLVMFree(LLVMRegister register) {
        this.register = register;
    }

    public String toString() {
        return String.format("call void @free(i8* %s)", register.toString());
    }

    @Override
    public LLVMRegister getDefRegister() {
        return null;
    }

    @Override
    public List<LLVMRegister> getUseRegisters() {
        List<LLVMRegister> uses = new ArrayList<>();
        if (register instanceof LLVMRegister) {
            uses.add((LLVMRegister) register);
        }
        return uses;
    }

    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    public void replace(String reg, ConstImm value) {}
}
