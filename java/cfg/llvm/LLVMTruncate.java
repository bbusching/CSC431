package cfg.llvm;

import ast.BoolType;
import ast.Type;
import cfg.Value;
import constprop.ConstImm;
import constprop.ConstValue;
import constprop.Top;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/26/2017.
 */
public class LLVMTruncate implements LLVMInstruction {
    private LLVMRegister result;
    private Type toType;
    private Value val;

    public LLVMTruncate(LLVMRegister result, Type toType, Value val) {
        this.result = result;
        this.toType = toType;
        this.val = val;
    }

    @Override
    public String toString() {
        return String.format("%s = trunc %s %s to %s",
                             result.toString(),
                             val.getType().toLlvmType(),
                             val.toString(),
                             toType instanceof BoolType ? "i1" : toType.toLlvmType());
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
        }
        return new Top();
    }

    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        if (val instanceof LLVMImmediate) {
            return new ConstImm(((LLVMImmediate) val).getVal());
        } else {
            return valueByRegister.get(val.toString());
        }
    }

    public void replace(String reg, ConstImm value) {
        val = new LLVMImmediate(value.getVal());
    }
}
