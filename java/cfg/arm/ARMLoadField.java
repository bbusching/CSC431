package cfg.arm;

import ast.Type;
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
public class ARMLoadField implements ARMInstruction {
    private Type type;
    private ARMRegister result;
    private ARMRegister ptr;
    private int index;

    public ARMLoadField(Type type, ARMRegister result, ARMRegister ptr, int index) {
        this.type = type;
        this.result = result;
        this.ptr = ptr;
        this.index = index;
    }

    public String toString() {
        return String.format("%s = getelementptr %s %s, i1 0, i32 %d",
                             result.toString(),
                             type.toLlvmType(),
                             ptr.toString(),
                             index);
    }
    @Override
    public ARMRegister getDefRegister() {
        return result;
    }

    @Override
    public List<ARMRegister> getUseRegisters() {
        List<ARMRegister> uses = new ArrayList<>();
        uses.add(ptr);
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
        ARMRegister temp = new ARMRegister(type);
        pw.println("\tldr " + temp.toString() + ", " + ptr.toString());
        pw.println("\tldr " + result.toString() + "[" + temp.toString() + ", #" + index * 4 + "]");
    }
}
