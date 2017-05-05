package ast;

public class BoolType
   implements Type
{
   public final static String TYPE = "bool";

   public String toTypeString() {
      return TYPE;
   }

   public String toString() {
      return "BoolType";
   }

   @Override
   public String toLlvmType() {
      return "i1";
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof BoolType;
   }
}
