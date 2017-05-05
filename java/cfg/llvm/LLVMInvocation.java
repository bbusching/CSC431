package cfg.llvm;

import ast.Type;
import ast.VoidType;
import cfg.Value;

import java.util.List;

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
}
