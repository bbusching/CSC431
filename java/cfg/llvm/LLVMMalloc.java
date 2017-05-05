package cfg.llvm;

/**
 * Created by Brad on 4/22/2017.
 */
public class LLVMMalloc implements LLVMInstruction {
    private LLVMRegister result;
    private int bytes;

    public LLVMMalloc(LLVMRegister result, int bytes) {
        this.result = result;
        this.bytes = bytes;
    }

    public String toString() {
        return String.format("%s = call i8* @malloc(i64 %d)", result.toString(), bytes);
    }
}
