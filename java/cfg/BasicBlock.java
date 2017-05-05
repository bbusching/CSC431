package cfg;

import ast.BoolType;
import ast.IntType;
import ast.StructType;
import ast.Type;
import cfg.llvm.*;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicBlock {
    private static int num = 0;
    private String label;
    private boolean sealed = false;
    private Map<String, Value> valueByIdentifier = new HashMap<>();
    private List<BasicBlock> predecessors = new ArrayList<>();
    private List<BasicBlock> successors = new ArrayList<>();
    private List<LLVMPhi> phis = new ArrayList<>();
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

    public void addPredecessor(BasicBlock bb) {
        this.predecessors.add(bb);
    }

    public List<BasicBlock> getPredecessors() {
        return this.predecessors;
    }

    public void addSuccessor(BasicBlock bb) {
        this.successors.add(bb);
    }

    public List<BasicBlock> getSuccessors() {
        return this.successors;
    }

    public void writeVariable(String id, Value val) {
        this.valueByIdentifier.put(id, val);
    }

    public Value readVariabe(String id, Type t) {
        if (valueByIdentifier.containsKey(id)) {
            return valueByIdentifier.get(id);
        } else {
            return readVariabeFromPredecessors(id, t);
        }
    }

    private Value readVariabeFromPredecessors(String id, Type t) {
        if (!sealed) {
            LLVMRegister result = new LLVMRegister(t);
            this.phis.add(new LLVMPhi(id, result));
            writeVariable(id, result);
            return result;
        } else if (predecessors.size() == 0) {
            if (t instanceof BoolType || t instanceof IntType) {
                Value result = new LLVMImmediate(0);
                writeVariable(id, result);
                return result;
            } else if (t instanceof StructType) {
                writeVariable(id, LLVMNull.instance());
                return LLVMNull.instance();
            } else {
                throw new RuntimeException("Should not be here");
            }
        } else if (predecessors.size() == 1) {
            Value result = predecessors.get(0).readVariabe(id, t);
            writeVariable(id, result);
            return result;
        } else {
            LLVMRegister result = new LLVMRegister(t);
            LLVMPhi newphi = new LLVMPhi(id, result);
            this.phis.add(newphi);
            writeVariable(id, result);
            addPhiOperands(id, t, newphi);
            return result;
        }
    }

    private void addPhiOperands(String var, Type t, LLVMPhi phi) {
        for (BasicBlock pred : predecessors) {
            phi.addOperand(new Pair<>(pred.readVariabe(var, t), pred.getLabel()));
        }
    }
    public void seal() {
        this.sealed = true;
        for (LLVMPhi phi : phis) {
            addPhiOperands(phi.getIdentifier(), phi.getResult().getType(), phi);
        }
    }

    public boolean isEmpty() {
        return this.instructions.isEmpty();
    }

    public void writeBlock(PrintWriter pw) {
        pw.println(String.format("%s:", this.label));
        for (LLVMPhi phi : phis) {
            pw.println(String.format("  %s", phi.toString()));
        }
        for (LLVMInstruction inst: this.instructions) {
            pw.println(String.format("  %s", inst.toString()));
        }
    }
}
