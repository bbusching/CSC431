package cfg;

import cfg.llvm.LLVMInstruction;

import java.util.ArrayList;
import java.util.List;

public class DefUse {
    LLVMInstruction definition;
    List<LLVMInstruction> uses;

    public DefUse(LLVMInstruction definition) {
        this.definition = definition;
        uses = new ArrayList<>();
    }

    public LLVMInstruction getDefinition() {
        return definition;
    }

    public void addUse(LLVMInstruction inst) {
        this.uses.add(inst);
    }

    public List<LLVMInstruction> getUses() {
        return this.uses;
    }
}
