package ast;

public class LvalueDot
   implements Lvalue
{
   private final int lineNum;
   private final Expression left;
   private final String id;

   public LvalueDot(int lineNum, Expression left, String id)
   {
      this.lineNum = lineNum;
      this.left = left;
      this.id = id;
   }

   public int getLineNum() {
      return lineNum;
   }

   public Expression getLeft() {
      return left;
   }

   public String getId() {
      return id;
   }
}
