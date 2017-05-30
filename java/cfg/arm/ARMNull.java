package cfg.arm;

import ast.StructType;
import ast.Type;
import cfg.Value;

/**
 * Created by Brad on 4/20/2017.
 */
public class ARMNull implements Value {
    private static final Type t = new StructType(0, "");
    private static final ARMNull instance = new ARMNull();

    private ARMNull() {
    }

    public static ARMNull instance() {
        return instance;
    }

    public Type getType() {
        return t;
    }

    public String toString() {
        return "0";
    }
}
