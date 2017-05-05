package cfg.llvm;

import cfg.Value;

/**
 * Created by Brad on 4/22/2017.
 */
public class LLVMPrint implements LLVMInstruction {
   private String format;
   private Value value;

   public LLVMPrint(String format, Value value) {
      this.format = format;
      this.value = value;
   }

   public String toString() {
      return String.format("call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([5 x i8]* %s, i32 0, i32 0), i64 %s)",
                           format, value.toString());
   }
}
