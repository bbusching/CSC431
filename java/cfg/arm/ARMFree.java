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
public class ARMFree implements ARMInstruction {
    private ARMRegister register;

    public ARMFree(ARMRegister register) {
        this.register = register;
    }

    public String toString() {
        return String.format("call void @free(i8* %s)", register.toString());
    }

    @Override
    public ARMRegister getDefRegister() {
        return null;
    }

    @Override
    public List<ARMRegister> getUseRegisters() {
        List<ARMRegister> uses = new ArrayList<>();
        if (register instanceof ARMRegister) {
            uses.add((ARMRegister) register);
        }
        return uses;
    }

    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    public void replace(String reg, ConstImm value) {}

    public void write(PrintWriter pw) {
        pw.println("\tpush r0");
        pw.println("\tmov r0, " + register.toString());
        pw.println("\tbl free");
        pw.println("\tpop r0");
    }
}
