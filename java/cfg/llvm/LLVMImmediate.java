package cfg.llvm;

import ast.IntType;
import ast.Type;
import cfg.Value;

public class LLVMImmediate implements Value {
    private int val;

    public LLVMImmediate(int val) {
        this.val = val;
    }

    public Type getType() {
        return new IntType();
    }

    public int getVal() {
        return this.val;
    }

    public String toString() {
        return Integer.toString(this.val);
    }
}
