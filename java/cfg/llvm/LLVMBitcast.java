package cfg.llvm;

import cfg.Value;

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
}
