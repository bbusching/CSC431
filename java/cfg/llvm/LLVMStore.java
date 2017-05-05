package cfg.llvm;

import ast.Type;
import cfg.Value;

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
}
