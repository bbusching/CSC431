package cfg.arm;

import cfg.Value;
import constprop.ConstImm;
import constprop.ConstValue;
import constprop.Top;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 5/26/2017.
 */
public class ARMMov implements ARMInstruction {
    ARMRegister to;
    Value from;

    public ARMMov(Value from, ARMRegister to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public ARMRegister getDefRegister() {
        return to;
    }

    @Override
    public List<ARMRegister> getUseRegisters() {
        List<ARMRegister> l = new ArrayList<>();
        if (from instanceof ARMRegister) {
            l.add((ARMRegister) from);
        }
        return l;
    }

    @Override
    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        return valueByRegister.getOrDefault(to.toString(), new Top());
    }

    @Override
    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        return valueByRegister.getOrDefault(to.toString(), new Top());
    }

    @Override
    public void replace(String reg, ConstImm value) {
    }

    @Override
    public void write(PrintWriter pw) {
        pw.println("\tmov " + to.toString() + ", " + from.toString());
    }
}
