package cfg.arm;

import ast.Type;
import cfg.Value;
import constprop.Bottom;
import constprop.ConstImm;
import constprop.ConstValue;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/20/2017.
 */
public class ARMStoreField implements ARMInstruction {
    private Value val;
    private ARMRegister ptr;
    private int index;

    public ARMStoreField(Value result, ARMRegister ptr, int index) {
        this.val = result;
        this.ptr = ptr;
        this.index = index;
    }

    public String toString() {
        return "";
    }

    @Override
    public ARMRegister getDefRegister() {
        return null;
    }

    @Override
    public List<ARMRegister> getUseRegisters() {
        List<ARMRegister> uses = new ArrayList<>();
        uses.add(ptr);
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
        if (val instanceof ARMImmediate) {
            val = ((ARMImmediate) val).writeLoad(pw);
        }
        pw.println("\tstr " + val.toString() + ", [" + ptr.toString() + ", #" + index * 4 + "]");
    }
}

