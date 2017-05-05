package ast;

public class EmptyType implements Type {
    public static final String TYPE = "empty";

    public String toString() {
        return "EmptyType";
    }

    @Override
    public String toTypeString() {
        return TYPE;
    }

    @Override
    public String toLlvmType() {
        return TYPE;
    }
}
