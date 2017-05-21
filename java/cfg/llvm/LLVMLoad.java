package cfg.llvm;

import ast.Type;
import constprop.Bottom;
import constprop.ConstImm;
import constprop.ConstValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/20/2017.
 */
public class LLVMLoad implements LLVMInstruction {
    private Type type;
    private LLVMRegister result;
    private LLVMRegister op;

    public LLVMLoad(Type type, LLVMRegister result, LLVMRegister op) {
        this.type = type;
        this.result = result;
        this.op = op;
    }

    public String toString() {
        return String.format("%s = load %s* %s",
                             result.toString(),
                             type.toLlvmType(),
                             op.toString());
    }

    @Override
    public LLVMRegister getDefRegister() {
        return result;
    }

    @Override
    public List<LLVMRegister> getUseRegisters() {
        List<LLVMRegister> uses = new ArrayList<>();
        uses.add(op);
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
