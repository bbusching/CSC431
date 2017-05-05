package cfg.llvm;

import ast.StructType;
import ast.Type;
import cfg.Value;

/**
 * Created by Brad on 4/20/2017.
 */
public class LLVMNull implements Value {
    private static final Type t = new StructType(0, "");
    private static final LLVMNull instance = new LLVMNull();

    private LLVMNull() {
    }

    public static LLVMNull instance() {
        return instance;
    }

    public Type getType() {
        return t;
    }

    public String toString() {
        return "null";
    }
}
