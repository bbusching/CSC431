package cfg.arm;

import constprop.ConstImm;
import constprop.ConstValue;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/22/2017.
 */
public class ARMRead implements ARMInstruction {
    public String toString() {
        return "call i32 (i8*, ...)* @scanf(i8* getelementptr inbounds ([4 x i8]* @.read, i32 0, i32 0), i64* @.read_scratch)";
    }

    @Override
    public ARMRegister getDefRegister() {
        return null;
    }

    @Override
    public List<ARMRegister> getUseRegisters() {
        List<ARMRegister> uses = new ArrayList<>();
        return uses;
    }

    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    public void replace(String reg, ConstImm value) {
    }

    public void write(PrintWriter pw) {
        pw.println("\tpush {r0, r1}");
        pw.println("\tmovw r0, #:lower16:.read");
        pw.println("\tmovt r0, #:upper16:.read");
        pw.println("\tmovw r1, #:lower16:.read_scratch");
        pw.println("\tmovt r1, #:upper16:.read_scratch");
        pw.println("\tbl __isoc99_scanf");
        pw.println("\tpop {r0, r1}");
    }
}
