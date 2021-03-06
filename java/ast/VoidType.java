package ast;

public class VoidType
   implements Type
{
   public static final String TYPE = "void";

   public String toTypeString() {
      return TYPE;
   }

   public String toString() {
      return "VoidType";
   }

   @Override
   public String toLlvmType() {
      return TYPE;
   }

   public boolean equals(Object o) {
      return o instanceof VoidType;
   }
}
