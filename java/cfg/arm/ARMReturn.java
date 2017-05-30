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
public class ARMReturn implements ARMInstruction {
    private Type type;
    private Value reg;
    private int locals;

    public ARMReturn(Type type, Value reg) {
        this(type, reg, 0);
    }

    public ARMReturn(Type type, Value reg, int locals) {
        this.type = type;
        this.reg = reg;
        this.locals = locals;
    }

    public String toString() {
        return String.format("ret %s %s", type.toLlvmType(), reg.toString());
    }
    @Override
    public ARMRegister getDefRegister() {
        return null;
    }

    @Override
    public List<ARMRegister> getUseRegisters() {
        List<ARMRegister> uses = new ArrayList<>();
        if (reg instanceof ARMRegister) {
            uses.add((ARMRegister) reg);
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
        this.reg = new ARMImmediate(value.getVal());
    }

    public void write(PrintWriter pw) {
        if (reg instanceof ARMImmediate) {
            reg = ((ARMImmediate) reg).writeLoad(pw);
        }
        pw.println("\tmov r0, " + reg.toString());
        pw.println("\tadd sp, sp, #" + 4 * locals);
        pw.println("\tpop {fp, lr}");
        pw.println("\tbx lr");
    }
}
