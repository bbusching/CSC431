package cfg.llvm;

/**
 * Created by Brad on 4/20/2017.
 */
public class LLVMJump implements LLVMInstruction {
    private String label;

    public LLVMJump(String label) {
        this.label = label;
    }

    public String toString() {
        return String.format("br label %%%s", label);
    }
}
