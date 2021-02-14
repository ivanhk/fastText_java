/*    */ package ai.searchbox.FastText4J.math.quant;
/*    */ 
/*    */ import ai.searchbox.FastText4J.Utils;
/*    */ 
/*    */ public class QCodeArray
/*    */   implements QCodes
/*    */ {
/*    */   private final int[] codes;
/*    */   
/*    */   public QCodeArray(QCodeArray qcodes) {
/* 11 */     this.codes = qcodes.codes;
/*    */   }
/*    */   
/*    */   public QCodeArray(int[] codes) {
/* 15 */     this.codes = codes;
/*    */   }
/*    */   
/*    */   public QCodeArray(int size) {
/* 19 */     this.codes = new int[size];
/*    */   }
/*    */   
/*    */   public int get(int i) {
/* 23 */     Utils.checkArgument((i >= 0));
/* 24 */     Utils.checkArgument((i < this.codes.length));
/*    */     
/* 26 */     return this.codes[i];
/*    */   }
/*    */   
/*    */   public int size() {
/* 30 */     return this.codes.length;
/*    */   }
/*    */ 
/*    */   
/*    */   public String toString() {
/* 35 */     StringBuilder builder = new StringBuilder();
/* 36 */     builder.append("QCodeArray(size=");
/* 37 */     builder.append(size());
/* 38 */     builder.append(", [");
/* 39 */     for (int i = 0; i < size(); i++) {
/* 40 */       builder.append(get(i)).append(' ');
/*    */     }
/* 42 */     if (builder.length() > 1) {
/* 43 */       builder.setLength(builder.length() - 1);
/*    */     }
/* 45 */     builder.append("])");
/* 46 */     return builder.toString();
/*    */   }
/*    */ }


/* Location:              /Users/davidgortega/Desktop/FastText4J.jar!/ai/searchbox/FastText4J/math/quant/QCodeArray.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */