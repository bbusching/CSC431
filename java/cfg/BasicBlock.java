package cfg;

import ast.BoolType;
import ast.IntType;
import ast.StructType;
import ast.Type;
import cfg.arm.*;

import java.io.PrintWriter;
import java.util.*;

public class BasicBlock {
    private boolean executable = true;
    private static int num = 0;
    private String label;
    private boolean sealed = false;
    private Map<String, Value> valueByIdentifier = new HashMap<>();
    private List<BasicBlock> predecessors = new ArrayList<>();
    private List<BasicBlock> successors = new ArrayList<>();
    private List<ARMPhi> phis = new ArrayList<>();
    private List<ARMInstruction> instructions = new ArrayList<>();

    public List<ARMInstruction> getInstructions() {
        return instructions;
    }

    public List<ARMPhi> getPhis() {
        return phis;
    }

    public BasicBlock() {
        this.label = ".L" + num++;
    }

    public String getLabel() {
        return this.label;
    }

    public void setExecutable(boolean executable) {
        this.executable = executable;
    }

    public void addInstruction(ARMInstruction inst) {
        this.instructions.add(inst);
    }

    public void addInstructionToFront(ARMInstruction inst) {
        this.instructions.add(0, inst);
    }

    public void addPhiMov(ARMMov mov) {
        this.instructions.add(this.instructions.size() - 1, mov);
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

    public Value readVariable(String id, Type t) {
        if (valueByIdentifier.containsKey(id)) {
            return valueByIdentifier.get(id);
        } else {
            return readVariabeFromPredecessors(id, t);
        }
    }

    private Value readVariabeFromPredecessors(String id, Type t) {
        if (!sealed) {
            ARMRegister result = new ARMRegister(t);
            this.phis.add(new ARMPhi(id, result));
            writeVariable(id, result);
            return result;
        } else if (predecessors.size() == 0) {
            if (t instanceof BoolType || t instanceof IntType) {
                Value result = new ARMImmediate(0);
                writeVariable(id, result);
                return result;
            } else if (t instanceof StructType) {
                writeVariable(id, ARMNull.instance());
                return ARMNull.instance();
            } else {
                throw new RuntimeException("Should not be here");
            }
        } else if (predecessors.size() == 1) {
            Value result = predecessors.get(0).readVariable(id, t);
            writeVariable(id, result);
            return result;
        } else {
            ARMRegister result = new ARMRegister(t);
            ARMPhi newphi = new ARMPhi(id, result);
            this.phis.add(newphi);
            writeVariable(id, result);
            addPhiOperands(id, t, newphi);
            return result;
        }
    }

    private void addPhiOperands(String var, Type t, ARMPhi phi) {
        for (BasicBlock pred : predecessors) {
            phi.addOperand(new Pair<>(pred.readVariable(var, t), pred.getLabel()));
        }
    }
    public void seal() {
        this.sealed = true;
        for (ARMPhi phi : phis) {
            addPhiOperands(phi.getIdentifier(), phi.getResult().getType(), phi);
        }
    }

    public boolean isEmpty() {
        return this.instructions.isEmpty();
    }

    public void writeBlock(PrintWriter pw) {
        if (executable) {
            pw.println(label + ":");
            for (ARMInstruction inst : this.instructions) {
                inst.write(pw);
            }
        }
    }

    public void remove(ARMInstruction inst) {
        phis.remove(inst);
        instructions.remove(inst);
    }

    public void removePredecessor(BasicBlock pred) {
        predecessors.remove(pred);
        if (predecessors.size() == 0) {
            setExecutable(false);
            Queue<BasicBlock> toVisit = new ArrayDeque<>();
            Set<BasicBlock> visited = new HashSet<>();
            for (BasicBlock succ : successors) {
                toVisit.add(succ);
                visited.add(succ);
            }

            while (!toVisit.isEmpty()) {
                BasicBlock cur = toVisit.poll();
                for (BasicBlock bb : cur.getSuccessors()) {
                    if (!visited.contains(bb)) {
                        toVisit.add(bb);
                        visited.add(bb);
                    }
                }

                for (ARMPhi phi : cur.getPhis()) {
                    phi.removeLabel(this.label);
                }
            }
        }
    }

    public void resolvePhis() {
        for (ARMPhi phi : phis) {
            ARMRegister temp = new ARMRegister(null);
            for (BasicBlock pred : predecessors) {
                for (Pair<Value, String> operand : phi.getOperands()) {
                    if (operand.getSecond().equals(pred.getLabel())) {
                        pred.addPhiMov(new ARMMov(operand.getFirst(), phi.getDefRegister()));
                    }
                }
            }
        }
    }
}
