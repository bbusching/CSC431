package cfg.llvm;

/**
 * Created by Brad on 4/22/2017.
 */
public class LLVMRead implements LLVMInstruction {
    public String toString() {
        return "call i32 (i8*, ...)* @scanf(i8* getelementptr inbounds ([4 x i8]* @.read, i32 0, i32 0), i64* @.read_scratch)";
    }
}
