package cfg.llvm;

import cfg.Pair;
import cfg.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Brad on 5/5/2017.
 */
public class LLVMPhi implements LLVMInstruction {
    private String identifier;
    private LLVMRegister result;
    private List<Pair<Value, String>> operands = new ArrayList<>();

    public LLVMPhi(String identifier, LLVMRegister result) {
        this.identifier = identifier;
        this.result = result;
    }

    public void addOperand(Pair<Value, String> operand) {
        this.operands.add(operand);
    }

    public String getIdentifier() {
        return identifier;
    }

    public LLVMRegister getResult() {
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(String.format("%s = phi %s ", result.toString(), result.getType().toLlvmType()));

        for (int i = 0; i < operands.size(); ++i) {
            Pair<Value, String> p = operands.get(i);
            sb.append(String.format("[ %s, %%%s]", p.getFirst().toString(), p.getSecond()));
            if (i != operands.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("\t; var " + identifier);
        return sb.toString();
    }
}
