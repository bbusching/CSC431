package ast;

public interface Type
{
   public String toTypeString();
   public String toLlvmType();
   public boolean equals(Object o);
}
