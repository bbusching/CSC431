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
public class ARMLoad implements ARMInstruction {
    private Type type;
    private ARMRegister result;
    private ARMRegister op;

    public ARMLoad(Type type, ARMRegister result, ARMRegister op) {
        this.type = type;
        this.result = result;
        this.op = op;
    }

    public String toString() {
        return String.format("%s = load %s* %s",
                             result.toString(),
                             type.toLlvmType(),
                             op.toString());
    }

    @Override
    public ARMRegister getDefRegister() {
        return result;
    }

    @Override
    public List<ARMRegister> getUseRegisters() {
        List<ARMRegister> uses = new ArrayList<>();
        uses.add(op);
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
        pw.println("\tldr " + result.toString() + ", " + op.toString());
    }
}
