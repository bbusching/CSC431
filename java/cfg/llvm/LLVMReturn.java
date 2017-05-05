package cfg.llvm;

import ast.Type;
import cfg.Value;

/**
 * Created by Brad on 4/20/2017.
 */
public class LLVMReturn implements LLVMInstruction {
    private Type type;
    private Value reg;

    public LLVMReturn(Type type, Value reg) {
        this.type = type;
        this.reg = reg;
    }

    public String toString() {
        return String.format("ret %s %s", type.toLlvmType(), reg.toString());
    }
}
