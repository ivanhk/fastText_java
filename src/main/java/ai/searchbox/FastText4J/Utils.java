/*     */ package ai.searchbox.FastText4J;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.util.List;
/*     */ import java.util.ListIterator;
/*     */ import java.util.Map;
/*     */ import java.util.Random;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class Utils
/*     */ {
/*     */   private static final int SHUFFLE_THRESHOLD = 5;
/*     */   
/*     */   public static void checkArgument(boolean expression) {
/*  17 */     if (!expression) {
/*  18 */       throw new IllegalArgumentException();
/*     */     }
/*     */   }
/*     */   
/*     */   public static boolean isEmpty(String str) {
/*  23 */     return !(str != null && !str.isEmpty());
/*     */   }
/*     */   
/*     */   public static <K, V> V mapGetOrDefault(Map<K, V> map, K key, V defaultValue) {
/*  27 */     return map.containsKey(key) ? map.get(key) : defaultValue;
/*     */   }
/*     */   
/*     */   public static int randomInt(Random rnd, int lower, int upper) {
/*  31 */     checkArgument(((lower <= upper)) & ((lower > 0)));
/*     */     
/*  33 */     if (lower == upper) {
/*  34 */       return lower;
/*     */     }
/*     */     
/*  37 */     return rnd.nextInt(upper - lower) + lower;
/*     */   }
/*     */   
/*     */   public static float randomFloat(Random rnd, float lower, float upper) {
/*  41 */     checkArgument((lower <= upper));
/*     */     
/*  43 */     if (lower == upper) {
/*  44 */       return lower;
/*     */     }
/*     */     
/*  47 */     return rnd.nextFloat() * (upper - lower) + lower;
/*     */   }
/*     */   
/*     */   public static long sizeLine(String filename) throws IOException {
/*  51 */     Exception exception1 = null, exception2 = null;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*     */     try {
/*     */     
/*     */     } finally {
/*  70 */       exception2 = null; if (exception1 == null) { exception1 = exception2; } else if (exception1 != exception2) { exception1.addSuppressed(exception2); }
/*     */     
/*     */     } 
/*     */   }
/*     */   
/*     */   public static void shuffle(List<?> list, Random rnd) {
/*  76 */     int size = list.size();
/*  77 */     if (size < 5 || list instanceof java.util.RandomAccess) {
/*  78 */       for (int i = size; i > 1; i--)
/*  79 */         swap(list, i - 1, rnd.nextInt(i)); 
/*     */     } else {
/*  81 */       Object[] arr = list.toArray();
/*     */ 
/*     */       
/*  84 */       for (int i = size; i > 1; i--) {
/*  85 */         swap(arr, i - 1, rnd.nextInt(i));
/*     */       }
/*     */ 
/*     */ 
/*     */ 
/*     */       
/*  91 */       ListIterator<?> it = list.listIterator();
/*  92 */       for (int j = 0; j < arr.length; j++) {
/*  93 */         it.next();
/*  94 */         it.set(arr[j]);
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   public static void swap(Object[] arr, int i, int j) {
/* 100 */     Object tmp = arr[i];
/* 101 */     arr[i] = arr[j];
/* 102 */     arr[j] = tmp;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public static void swap(List<?> list, int i, int j) {
/* 109 */     List<?> l = list;
/* 110 */     l.set(i, l.set(j, l.get(i)));
/*     */   }
/*     */ }


/* Location:              /Users/davidgortega/Desktop/FastText4J.jar!/ai/searchbox/FastText4J/Utils.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */