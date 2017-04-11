package ast;

public class StructType
   implements Type
{
   public static final String NULL = "";
   private final int lineNum;
   private final String name;

   public StructType(int lineNum, String name)
   {
      this.lineNum = lineNum;
      this.name = name;
   }

   public int getLineNum() {
      return lineNum;
   }

   public String getName() {
      return name;
   }

   public String toTypeString() {
      return this.name;
   }

   public String toString() {
      return String.format("StructType{%s}", this.name);
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof StructType
              && (this.name.equals(((StructType) o).getName()) || NULL.equals(((StructType) o).getName()));
   }
}
