package cfg.arm;

import cfg.Value;
import constprop.ConstImm;
import constprop.ConstValue;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/22/2017.
 */
public class ARMPrint implements ARMInstruction {
   private String format;
   private Value value;

   public ARMPrint(String format, Value value) {
      this.format = format;
      this.value = value;
   }

   public String toString() {
      return String.format("call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([5 x i8]* %s, i32 0, i32 0), i64 %s)",
                           format, value.toString());
   }

   @Override
   public ARMRegister getDefRegister() {
      return null;
   }

   @Override
   public List<ARMRegister> getUseRegisters() {
      List<ARMRegister> uses = new ArrayList<>();
      if (value instanceof ARMRegister) {
         uses.add((ARMRegister) value);
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
      this.value = new ARMImmediate(value.getVal());
   }

   public void write(PrintWriter pw) {
      pw.println("\tpush {r0, r1}");
      pw.println("\tmovw r0, #:lower16:" + format);
      pw.println("\tmovt r0, #:upper16:" + format);
      if (value instanceof ARMImmediate) {
         value = ((ARMImmediate) value).writeLoad(pw);
      }
      pw.println("\tmov r1, " + value.toString());
      pw.println("\tbl printf");
      pw.println("\tpop {r0, r1}");
   }
}
