package ast;

public class LvalueId
   implements Lvalue
{
   private final int lineNum;
   private final String id;

   public LvalueId(int lineNum, String id)
   {
      this.lineNum = lineNum;
      this.id = id;
   }

   public int getLineNum() {
      return lineNum;
   }

   public String getId() {
      return id;
   }
}
