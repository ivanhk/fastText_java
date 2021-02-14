/*     */ package ai.searchbox.FastText4J.math;
/*     */ 
/*     */ import ai.searchbox.FastText4J.Utils;
/*     */ 
/*     */ public class Vector
/*     */ {
/*     */   public int m;
/*     */   public float[] data;
/*     */   
/*     */   public Vector(int size) {
/*  11 */     this.m = size;
/*  12 */     this.data = new float[this.m];
/*     */   }
/*     */   
/*     */   public Vector(float[] v) {
/*  16 */     this.m = v.length;
/*  17 */     this.data = new float[this.m];
/*     */     
/*  19 */     for (int i = 0; i < this.m; i++)
/*  20 */       set(i, v[i]); 
/*     */   }
/*     */   
/*     */   public int size() {
/*  24 */     return this.m;
/*     */   }
/*     */   
/*     */   public void zero() {
/*  28 */     for (int i = 0; i < this.m; i++) {
/*  29 */       this.data[i] = 0.0F;
/*     */     }
/*     */   }
/*     */   
/*     */   public void mul(float a) {
/*  34 */     for (int i = 0; i < this.m; i++) {
/*  35 */       this.data[i] = this.data[i] * a;
/*     */     }
/*     */   }
/*     */   
/*     */   public void addRow(Matrix A, int i) {
/*  40 */     Utils.checkArgument((i >= 0));
/*  41 */     Utils.checkArgument((i < A.m));
/*  42 */     Utils.checkArgument((this.m == A.n));
/*     */     
/*  44 */     A.addToVector(this, i);
/*     */   }
/*     */   
/*     */   public void addRow(Matrix A, int i, float a) {
/*  48 */     Utils.checkArgument((i >= 0));
/*  49 */     Utils.checkArgument((i < A.m));
/*  50 */     Utils.checkArgument((this.m == A.n));
/*     */     
/*  52 */     for (int j = 0; j < A.n; j++) {
/*  53 */       this.data[j] = this.data[j] + a * A.data[i][j];
/*     */     }
/*     */   }
/*     */   
/*     */   public void addVector(Vector source) {
/*  58 */     Utils.checkArgument((this.m == source.m));
/*     */     
/*  60 */     for (int i = 0; i < this.m; i++) {
/*  61 */       this.data[i] = this.data[i] + source.get(i);
/*     */     }
/*     */   }
/*     */   
/*     */   public float norm() {
/*  66 */     float sum = 0.0F;
/*  67 */     for (int i = 0; i < this.m; i++) {
/*  68 */       sum += this.data[i] * this.data[i];
/*     */     }
/*  70 */     return (float)Math.sqrt(sum);
/*     */   }
/*     */ 
/*     */   
/*     */   public void mul(Matrix A, Vector vec) {
/*  75 */     Utils.checkArgument((A.m == this.m));
/*  76 */     Utils.checkArgument((A.n == vec.m));
/*     */     
/*  78 */     for (int i = 0; i < this.m; i++) {
/*  79 */       this.data[i] = 0.0F;
/*  80 */       for (int j = 0; j < A.n; j++)
/*     */       {
/*  82 */         this.data[i] = A.dotRow(vec, i);
/*     */       }
/*     */     } 
/*     */   }
/*     */   
/*     */   public int argmax() {
/*  88 */     float max = this.data[0];
/*  89 */     int argmax = 0;
/*  90 */     for (int i = 1; i < this.m; i++) {
/*  91 */       if (this.data[i] > max) {
/*  92 */         max = this.data[i];
/*  93 */         argmax = i;
/*     */       } 
/*     */     } 
/*     */     
/*  97 */     return argmax;
/*     */   }
/*     */   
/*     */   public float get(int i) {
/* 101 */     return this.data[i];
/*     */   }
/*     */   
/*     */   public void set(int i, float value) {
/* 105 */     this.data[i] = value;
/*     */   }
/*     */   
/*     */   public String toString() {
/* 109 */     StringBuilder builder = new StringBuilder(); byte b; int i; float[] arrayOfFloat;
/* 110 */     for (i = (arrayOfFloat = this.data).length, b = 0; b < i; ) { float data = arrayOfFloat[b];
/* 111 */       builder.append(data).append(' '); b++; }
/*     */     
/* 113 */     if (builder.length() > 1) {
/* 114 */       builder.setLength(builder.length() - 1);
/*     */     }
/* 116 */     return builder.toString();
/*     */   }
/*     */ }


/* Location:              /Users/davidgortega/Desktop/FastText4J.jar!/ai/searchbox/FastText4J/math/Vector.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */