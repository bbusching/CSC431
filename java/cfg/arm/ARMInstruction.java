package cfg.arm;

import constprop.ConstImm;
import constprop.ConstValue;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/14/2017.
 */
public interface ARMInstruction {
    String toString();
    ARMRegister getDefRegister();
    List<ARMRegister> getUseRegisters();
    ConstValue initialize(Map<String, ConstValue> valueByRegister);
    ConstValue evaluate(Map<String, ConstValue> valueByRegister);
    void replace(String reg, ConstImm value);
    void write(PrintWriter pw);
}
