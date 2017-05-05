package cfg.llvm;

import ast.Type;

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
}
