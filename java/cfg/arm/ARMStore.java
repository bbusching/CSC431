package cfg.arm;

import ast.Type;
import cfg.Value;
import constprop.ConstImm;
import constprop.ConstValue;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/20/2017.
 */
public class ARMStore implements ARMInstruction {
    private Type type;
    private Value val;
    private ARMRegister pointer;

    public ARMStore(Type type, Value val, ARMRegister pointer) {
        this.type = type;
        this.val = val;
        this.pointer = pointer;
    }

    public String toString() {
        return String.format("store %s %s, %s* %s",
                             type.toLlvmType(),
                             val.toString(),
                             type.toLlvmType(),
                             pointer.toString());
    }
    @Override
    public ARMRegister getDefRegister() {
        return null;
    }

    @Override
    public List<ARMRegister> getUseRegisters() {
        List<ARMRegister> uses = new ArrayList<>();
        uses.add(pointer);
        if (val instanceof ARMRegister) {
            uses.add((ARMRegister) val);
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
        val = new ARMImmediate(value.getVal());
    }

    public void write(PrintWriter pw) {
        if (val instanceof ARMImmediate) {
            val = ((ARMImmediate) val).writeLoad(pw);
        }
        pw.println("\tstr " + val.toString() + ", [" + pointer.toString() + "]");
    }
}
