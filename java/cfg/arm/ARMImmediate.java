package cfg.arm;

import ast.IntType;
import ast.Type;
import cfg.Value;

import java.io.PrintWriter;

public class ARMImmediate implements Value {
    private int val;

    public ARMImmediate(int val) {
        this.val = val;
    }

    public Type getType() {
        return new IntType();
    }

    public int getVal() {
        return this.val;
    }

    public String toString() {
        return Long.toString(this.val);
    }

    public ARMRegister writeLoad(PrintWriter pw) {
        ARMRegister reg = new ARMRegister(new IntType());
        pw.println(String.format("\tmovw %s, #%d", reg.toString(), val & 0x0FFFF));
        pw.println(String.format("\tmovt %s, #%d", reg.toString(), (val & 0xFFFF0000) >> 16));
        return reg;
    }
}
