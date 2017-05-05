package ast;

/**
 * Created by Brad on 4/20/2017.
 */
public class PointerType implements Type{

    @Override
    public String toTypeString() {
        return "i8*";
    }

    @Override
    public String toLlvmType() {
        return "i8*";
    }
}
