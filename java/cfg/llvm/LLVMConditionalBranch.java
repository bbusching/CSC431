package cfg.llvm;

import cfg.Value;

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
}
