package cfg.arm;

import constprop.Bottom;
import constprop.ConstImm;
import constprop.ConstValue;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/22/2017.
 */
public class ARMMalloc implements ARMInstruction {
    private ARMRegister result;
    private int bytes;

    public ARMMalloc(ARMRegister result, int bytes) {
        this.result = result;
        this.bytes = bytes;
    }

    public String toString() {
        return String.format("%s = call i8* @malloc(i64 %d)", result.toString(), bytes);
    }
    @Override
    public ARMRegister getDefRegister() {
        return result;
    }

    @Override
    public List<ARMRegister> getUseRegisters() {
        List<ARMRegister> uses = new ArrayList<>();
        return uses;
    }

    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        return new Bottom();
    }

    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    public void replace(String reg, ConstImm value) {
    }

    public void write(PrintWriter pw) {
        pw.println("\tpush r0");
        pw.println("\tmov r0, " + bytes);
        pw.println("\tbl malloc");
        pw.println("\tstr " + result.toString() + ", r0");
        pw.println("\tpop r0");
    }
}
