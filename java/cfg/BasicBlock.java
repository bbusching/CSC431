package cfg;

import cfg.llvm.LLVMInstruction;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class BasicBlock {
    private static int num = 0;
    private String label;
    //private List<BasicBlock> predecessors = new ArrayList<>();
    private List<BasicBlock> successors = new ArrayList<>();
    private List<LLVMInstruction> instructions = new ArrayList<>();

    public BasicBlock() {
        this.label = "L" + num++;
    }

    public String getLabel() {
        return this.label;
    }

    public void addInstruction(LLVMInstruction inst) {
        this.instructions.add(inst);
    }

    public void addInstructionToFront(LLVMInstruction inst) {
        this.instructions.add(0, inst);
    }

    public void addSuccessor(BasicBlock bb) {
        this.successors.add(bb);
    }

    public List<BasicBlock> getSuccessors() {
        return this.successors;
    }

    public boolean isEmpty() {
        return this.instructions.isEmpty();
    }

    public void writeBlock(PrintWriter pw) {
        pw.println(String.format("%s:", this.label));
        for (LLVMInstruction inst: this.instructions) {
            pw.println(String.format("  %s", inst.toString()));
        }
    }
}
