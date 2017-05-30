package cfg.arm;

import ast.Type;
import cfg.Value;

/**
 * Created by Brad on 4/14/2017.
 */
public class ARMRegister implements Value {
    private static int number = 0;
    private final Type t;
    private final String name;

    public ARMRegister(Type t) {
        this(t, "v" + number++);
    }

    public ARMRegister(Type t, String name) {
        this.t = t;
        this.name = name;
    }

    public Type getType() {
        return this.t;
    }

    public String toString() {
        return this.name;
    }
}
