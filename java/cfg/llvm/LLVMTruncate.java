package cfg.llvm;

import ast.BoolType;
import ast.Type;
import cfg.Value;

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
}
