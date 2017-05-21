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
public class LLVMStore implements LLVMInstruction {
    private Type type;
    private Value val;
    private LLVMRegister pointer;

    public LLVMStore(Type type, Value val, LLVMRegister pointer) {
        this.type = type;
        this.val = val;
        this.pointer = pointer;
    }

    public String toString() {
        return String.format("store %s %s, %s* %s",
                             type.toLlvmType(),
                             val.toString(),
                             type.toLlvmType(),
                             pointer.toString());
    }
    @Override
    public LLVMRegister getDefRegister() {
        return null;
    }

    @Override
    public List<LLVMRegister> getUseRegisters() {
        List<LLVMRegister> uses = new ArrayList<>();
        uses.add(pointer);
        if (val instanceof LLVMRegister) {
            uses.add((LLVMRegister) val);
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
        val = new LLVMImmediate(value.getVal());
    }
}
