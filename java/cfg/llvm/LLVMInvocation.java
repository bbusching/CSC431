package cfg.llvm;

import ast.Type;
import ast.VoidType;
import cfg.Value;
import constprop.Bottom;
import constprop.ConstImm;
import constprop.ConstValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Brad on 4/20/2017.
 */
public class LLVMInvocation implements LLVMInstruction {
    private List<Type> argTypes;
    private Type type;
    private LLVMRegister result;
    private String function;
    private Value[] args;

    public LLVMInvocation(List<Type> argTypes, Type type, LLVMRegister result, String function, Value... args) {
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
    public LLVMRegister getDefRegister() {
        return result;
    }

    @Override
    public List<LLVMRegister> getUseRegisters() {
        List<LLVMRegister> uses = new ArrayList<>();
        for (Value arg : args) {
            if (arg instanceof LLVMRegister) {
                uses.add((LLVMRegister) arg);
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
            if (args[i] instanceof LLVMRegister && args[i].toString().equals(reg)) {
                args[i] = new LLVMImmediate(value.getVal());
            }
        }
    }
}
