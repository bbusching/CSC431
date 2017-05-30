package cfg;

import cfg.arm.ARMInstruction;

import java.util.ArrayList;
import java.util.List;

public class DefUse {
    ARMInstruction definition;
    List<ARMInstruction> uses;

    public DefUse(ARMInstruction definition) {
        this.definition = definition;
        uses = new ArrayList<>();
    }

    public ARMInstruction getDefinition() {
        return definition;
    }

    public void addUse(ARMInstruction inst) {
        this.uses.add(inst);
    }

    public List<ARMInstruction> getUses() {
        return this.uses;
    }
}
