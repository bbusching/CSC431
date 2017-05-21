package cfg.llvm;

import constprop.ConstImm;
import constprop.ConstValue;

import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/14/2017.
 */
public interface LLVMInstruction {
    String toString();
    LLVMRegister getDefRegister();
    List<LLVMRegister> getUseRegisters();
    ConstValue initialize(Map<String, ConstValue> valueByRegister);
    ConstValue evaluate(Map<String, ConstValue> valueByRegister);
    void replace(String reg, ConstImm value);
}
