package cfg.arm;

import ast.Type;
import ast.VoidType;
import cfg.Value;
import constprop.Bottom;
import constprop.ConstImm;
import constprop.ConstValue;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/20/2017.
 */
public class ARMInvocation implements ARMInstruction {
    private List<Type> argTypes;
    private Type type;
    private ARMRegister result;
    private String function;
    private Value[] args;

    public ARMInvocation(List<Type> argTypes, Type type, ARMRegister result, String function, Value... args) {
        this.argTypes = argTypes;
        this.type = type;
        this.result = result;
        this.function = function;
        this.args = args;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!(type instanceof VoidType)) {
            sb.append(result.toString());
            sb.append(" = ");
        };
        sb.append(String.format("call %s @%s(", type.toLlvmType(), function));
        for (int i = 0; i < args.length; ++i) {
            sb.append(String.format("%s %s", argTypes.get(i).toLlvmType(), args[i].toString()));
            if (i < args.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();

    }

    @Override
    public ARMRegister getDefRegister() {
        return result;
    }

    @Override
    public List<ARMRegister> getUseRegisters() {
        List<ARMRegister> uses = new ArrayList<>();
        for (Value arg : args) {
            if (arg instanceof ARMRegister) {
                uses.add((ARMRegister) arg);
            }
        }
        return uses;
    }

    public ConstValue initialize(Map<String, ConstValue> valueByRegister) {
        return new Bottom();
    }

    public ConstValue evaluate(Map<String, ConstValue> valueByRegister) {
        return null;
    }

    public void replace(String reg, ConstImm value) {
        for (int i = 0; i < args.length; ++i) {
            if (args[i] instanceof ARMRegister && args[i].toString().equals(reg)) {
                args[i] = new ARMImmediate(value.getVal());
            }
        }
    }

    public void write(PrintWriter pw) {
        pw.println("\t push {r0 - r3}");
        for (int i = 0; i < args.length && i < 4; ++i) {
            if (args[i] instanceof ARMImmediate) {
                args[i] = ((ARMImmediate) args[i]).writeLoad(pw);
            }
            pw.println("\tmov r" + i + ", " + args[i].toString());
        }
        for (int i = args.length; i > 3; --i) {
            if (args[i] instanceof ARMImmediate) {
                args[i] = ((ARMImmediate) args[i]).writeLoad(pw);
            }
            pw.println("\tpush " + args[i]);
        }
        pw.println("\tmov " + result.toString() + ", r0");
        pw.println("\tpop {r0 - r3}");
    }
}
