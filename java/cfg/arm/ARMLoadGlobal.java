package cfg.arm;

import constprop.Bottom;
import constprop.ConstImm;
import constprop.ConstValue;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 5/26/2017.
 */
public class ARMLoadGlobal implements ARMInstruction {
    private String name;
    private ARMRegister result;

    public ARMLoadGlobal(String name, ARMRegister result) {
        this.name = name;
        this.result = result;
    }

    @Override
    public ARMRegister getDefRegister() {
        return result;
    }

    @Override
    public List<ARMRegister> getUseRegisters() {
        return new ArrayList<>();
    }

    @Override
    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        return new Bottom();
    }

    @Override
    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    @Override
    public void replace(String reg, ConstImm value) {

    }

    @Override
    public void write(PrintWriter pw) {
        pw.println("\tmovw " + result.toString() + ", #:lower16:" + name);
        pw.println("\tmovt " + result.toString() + ", #:upper16:" + name);
    }
}
