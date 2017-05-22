package cfg.llvm;

import ast.IntType;
import ast.Type;
import cfg.Value;

public class LLVMImmediate implements Value {
    private long val;

    public LLVMImmediate(long val) {
        this.val = val;
    }

    public Type getType() {
        return new IntType();
    }

    public long getVal() {
        return this.val;
    }

    public String toString() {
        return Long.toString(this.val);
    }
}
