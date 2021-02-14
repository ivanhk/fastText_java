/*    */ package ai.searchbox.FastText4J;
/*    */ 
/*    */ public class Pair<K, V>
/*    */ {
/*    */   private K key_;
/*    */   private V value_;
/*    */   
/*    */   public Pair(K key, V value) {
/*  9 */     this.key_ = key;
/* 10 */     this.value_ = value;
/*    */   }
/*    */   
/*    */   public K getKey() {
/* 14 */     return this.key_;
/*    */   }
/*    */   
/*    */   public V getValue() {
/* 18 */     return this.value_;
/*    */   }
/*    */   
/*    */   public void setKey(K key) {
/* 22 */     this.key_ = key;
/*    */   }
/*    */   
/*    */   public void setValue(V value) {
/* 26 */     this.value_ = value;
/*    */   }
/*    */ }


/* Location:              /Users/davidgortega/Desktop/FastText4J.jar!/ai/searchbox/FastText4J/Pair.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */