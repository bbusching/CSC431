package cfg.llvm;

import ast.Type;

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
}
