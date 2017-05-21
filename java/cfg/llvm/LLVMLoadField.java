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
public class LLVMLoadField implements LLVMInstruction {
    private Type type;
    private LLVMRegister result;
    private LLVMRegister ptr;
    private int index;

    public LLVMLoadField(Type type, LLVMRegister result, LLVMRegister ptr, int index) {
        this.type = type;
        this.result = result;
        this.ptr = ptr;
        this.index = index;
    }

    public String toString() {
        return String.format("%s = getelementptr %s %s, i1 0, i32 %d",
                             result.toString(),
                             type.toLlvmType(),
                             ptr.toString(),
                             index);
    }
    @Override
    public LLVMRegister getDefRegister() {
        return result;
    }

    @Override
    public List<LLVMRegister> getUseRegisters() {
        List<LLVMRegister> uses = new ArrayList<>();
        uses.add(ptr);
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
