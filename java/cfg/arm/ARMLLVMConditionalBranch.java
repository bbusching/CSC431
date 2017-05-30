package cfg.arm;

import cfg.Pair;
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
public class ARMLLVMConditionalBranch implements ARMInstruction {
    private Value conditional;
    private String trueLabel;
    private String falseLabel;

    public ARMLLVMConditionalBranch(Value conditional, String trueLabel, String falseLabel) {
        this.conditional = conditional;
        this.trueLabel = trueLabel;
        this.falseLabel = falseLabel;
    }

    public String toString() {
        return String.format("br i1 %s, label %%%s, label %%%s",
                             conditional.toString(),
                             trueLabel,
                             falseLabel);
    }

    @Override
    public ARMRegister getDefRegister() {
        return null;
    }

    @Override
    public List<ARMRegister> getUseRegisters() {
        List<ARMRegister> uses = new ArrayList<>();
        if (conditional instanceof ARMRegister) {
            uses.add((ARMRegister) conditional);
        }
        return uses;
    }

    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        return valueByRegister.get(((ARMRegister) conditional).toString());
    }

    public void replace(String reg, ConstImm value) {
        conditional = new ARMImmediate(value.getVal());
    }

    public boolean isTrivial() {
        return conditional instanceof ARMImmediate;
    }

    public Pair<ARMJump, String> getNewBranchInstAndBadLabel() {
        if (isTrivial()) {
            if (((ARMImmediate) conditional).getVal() == 0) {
                return new Pair<>(new ARMJump(falseLabel), trueLabel);
            } else {
                return new Pair<>(new ARMJump(trueLabel), falseLabel);
            }
        }
        throw new RuntimeException("Something bad happened");
    }

    public void write(PrintWriter pw) {
        if (conditional instanceof ARMImmediate) {
            conditional = ((ARMImmediate) conditional).writeLoad(pw);
        }
        pw.println("\tcmp " + conditional.toString() + ", #0");
        pw.println("\tbne " + trueLabel);
        pw.println("\tbl " + falseLabel);
    }
}
