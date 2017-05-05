package ast;

public class IntType
   implements Type
{
   public final static String TYPE = "int";

   public String toTypeString() {
      return TYPE;
   }

   public String toString() {
      return "IntType";
   }

   @Override
   public String toLlvmType() {
      return "i64";
   }

   public boolean equals(Object o) {
      return o instanceof IntType;
   }
}
