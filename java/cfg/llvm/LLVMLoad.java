package cfg.llvm;

import ast.Type;

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
}
