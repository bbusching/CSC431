package cfg.llvm;

import cfg.Pair;
import cfg.Value;
import constprop.ConstImm;
import constprop.ConstValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

   @Override
   public LLVMRegister getDefRegister() {
      return null;
   }

   @Override
   public List<LLVMRegister> getUseRegisters() {
      List<LLVMRegister> uses = new ArrayList<>();
      if (value instanceof LLVMRegister) {
         uses.add((LLVMRegister) value);
      }
      return uses;
   }


   public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
      return null;
   }

   public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
      return null;
   }

   public void replace(String reg, ConstImm value) {
      this.value = new LLVMImmediate(value.getVal());
   }
}
