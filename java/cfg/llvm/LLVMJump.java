package cfg.llvm;

import constprop.ConstImm;
import constprop.ConstValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/20/2017.
 */
public class LLVMJump implements LLVMInstruction {
    private String label;

    public LLVMJump(String label) {
        this.label = label;
    }

    public String toString() {
        return String.format("br label %%%s", label);
    }

    @Override
    public LLVMRegister getDefRegister() {
        return null;
    }

    @Override
    public List<LLVMRegister> getUseRegisters() {
        List<LLVMRegister> uses = new ArrayList<>();
        return uses;
    }

    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    public void replace(String reg, ConstImm value) {
    }
}
