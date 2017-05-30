package cfg.arm;

import constprop.ConstImm;
import constprop.ConstValue;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/20/2017.
 */
public class ARMReturnVoid implements ARMInstruction {
    private int locals;
    public ARMReturnVoid(int locals) {
        this.locals = locals;
    }
    public String toString() {
        return "ret void";
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
        pw.println("\tadd sp, sp, #" + 4 * locals);
        pw.println("\tpop {fp, lr}");
        pw.println("\tbx lr");
    }
}
