package cfg.llvm;

/**
 * Created by Brad on 4/22/2017.
 */
public class LLVMFree implements LLVMInstruction {
    private LLVMRegister register;

    public LLVMFree(LLVMRegister register) {
        this.register = register;
    }

    public String toString() {
        return String.format("call void @free(i8* %s)", register.toString());
    }
}
