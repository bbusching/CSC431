package cfg.llvm;

import ast.Type;
import constprop.ConstImm;
import constprop.ConstValue;
import constprop.Top;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/20/2017.
 */
public class LLVMAlloc implements LLVMInstruction {
    private Type type;
    private LLVMRegister reg;

    public LLVMAlloc(Type t, LLVMRegister r) {
        this.type = t;
        this.reg = r;
    }

    public String toString() {
        return String.format("%s = alloca %s, align 8", reg.toString(), type.toLlvmType());
    }

    @Override
    public LLVMRegister getDefRegister() {
        return reg;
    }

    @Override
    public List<LLVMRegister> getUseRegisters() {
        return new ArrayList<>();
    }

    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        return new Top();
    }

    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    public void replace(String reg, ConstImm value) {
    }
}
