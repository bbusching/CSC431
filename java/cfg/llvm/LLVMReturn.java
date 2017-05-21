package cfg.llvm;

import ast.Type;
import cfg.Value;
import constprop.ConstImm;
import constprop.ConstValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/20/2017.
 */
public class LLVMReturn implements LLVMInstruction {
    private Type type;
    private Value reg;

    public LLVMReturn(Type type, Value reg) {
        this.type = type;
        this.reg = reg;
    }

    public String toString() {
        return String.format("ret %s %s", type.toLlvmType(), reg.toString());
    }
    @Override
    public LLVMRegister getDefRegister() {
        return null;
    }

    @Override
    public List<LLVMRegister> getUseRegisters() {
        List<LLVMRegister> uses = new ArrayList<>();
        if (reg instanceof LLVMRegister) {
            uses.add((LLVMRegister) reg);
        }
        return uses;
    }

    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    public void replace(String reg, ConstImm value) {
        this.reg = new LLVMImmediate(value.getVal());
    }
}
