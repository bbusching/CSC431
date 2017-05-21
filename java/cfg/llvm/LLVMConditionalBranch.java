package cfg.llvm;

import cfg.Pair;
import cfg.Value;
import constprop.ConstImm;
import constprop.ConstValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/20/2017.
 */
public class LLVMConditionalBranch implements LLVMInstruction {
    private Value conditional;
    private String trueLabel;
    private String falseLabel;

    public LLVMConditionalBranch(Value conditional, String trueLabel, String falseLabel) {
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
    public LLVMRegister getDefRegister() {
        return null;
    }

    @Override
    public List<LLVMRegister> getUseRegisters() {
        List<LLVMRegister> uses = new ArrayList<>();
        if (conditional instanceof LLVMRegister) {
            uses.add((LLVMRegister) conditional);
        }
        return uses;
    }

    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        return valueByRegister.get(((LLVMRegister) conditional).toString());
    }

    public void replace(String reg, ConstImm value) {
        conditional = new LLVMImmediate(value.getVal());
    }

    public boolean isTrivial() {
        return conditional instanceof LLVMImmediate;
    }

    public Pair<LLVMJump, String> getNewBranchInstAndBadLabel() {
        if (isTrivial()) {
            if (((LLVMImmediate) conditional).getVal() == 0) {
                return new Pair<>(new LLVMJump(falseLabel), trueLabel);
            } else {
                return new Pair<>(new LLVMJump(trueLabel), falseLabel);
            }
        }
        throw new RuntimeException("Something bad happened");
    }
}
