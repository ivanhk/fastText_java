/*     */ package ai.searchbox.FastText4J.math;
/*     */ 
/*     */ import ai.searchbox.FastText4J.Utils;
/*     */ import ai.searchbox.FastText4J.io.IOUtil;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.OutputStream;
/*     */ import java.util.Random;
/*     */ 
/*     */ 
/*     */ public class Matrix
/*     */ {
/*  13 */   public int m = 0;
/*  14 */   public int n = 0;
/*  15 */   public float[][] data = null;
/*     */   
/*     */   public Matrix() {}
/*     */   
/*     */   public Matrix(int m, int n) {
/*  20 */     this.m = m;
/*  21 */     this.n = n;
/*  22 */     this.data = new float[m][n];
/*     */   }
/*     */   
/*     */   public void zero() {
/*  26 */     for (int i = 0; i < this.m; i++) {
/*  27 */       for (int j = 0; j < this.n; j++) {
/*  28 */         this.data[i][j] = 0.0F;
/*     */       }
/*     */     } 
/*     */   }
/*     */   
/*     */   public void uniform(float a) {
/*  34 */     Random random = new Random(1L);
/*  35 */     for (int i = 0; i < this.m; i++) {
/*  36 */       for (int j = 0; j < this.n; j++) {
/*  37 */         this.data[i][j] = Utils.randomFloat(random, -a, a);
/*     */       }
/*     */     } 
/*     */   }
/*     */   
/*     */   public void addToVector(Vector x, int t) {
/*  43 */     for (int j = 0; j < this.n; j++) {
/*  44 */       x.data[j] = x.data[j] + this.data[t][j];
/*     */     }
/*     */   }
/*     */   
/*     */   public void addRow(Vector vec, int i, float a) {
/*  49 */     Utils.checkArgument((i >= 0));
/*  50 */     Utils.checkArgument((i < this.m));
/*  51 */     Utils.checkArgument((vec.m == this.n));
/*     */     
/*  53 */     for (int j = 0; j < this.n; j++) {
/*  54 */       this.data[i][j] = this.data[i][j] + a * vec.data[j];
/*     */     }
/*     */   }
/*     */   
/*     */   public float dotRow(Vector vec, int i) {
/*  59 */     Utils.checkArgument((i >= 0));
/*  60 */     Utils.checkArgument((i < this.m));
/*  61 */     Utils.checkArgument((vec.m == this.n));
/*     */     
/*  63 */     float d = 0.0F;
/*  64 */     for (int j = 0; j < this.n; j++) {
/*  65 */       d += this.data[i][j] * vec.data[j];
/*     */     }
/*  67 */     return d;
/*     */   }
/*     */   
/*     */   public void load(InputStream input) throws IOException {
/*  71 */     IOUtil ioutil = new IOUtil();
/*     */     
/*  73 */     this.m = (int)ioutil.readLong(input);
/*  74 */     this.n = (int)ioutil.readLong(input);
/*     */     
/*  76 */     ioutil.setFloatArrayBufferSize(this.n);
/*  77 */     this.data = new float[this.m][this.n];
/*  78 */     for (int i = 0; i < this.m; i++) {
/*  79 */       ioutil.readFloat(input, this.data[i]);
/*     */     }
/*     */   }
/*     */   
/*     */   public void save(OutputStream ofs) throws IOException {
/*  84 */     IOUtil ioutil = new IOUtil();
/*     */     
/*  86 */     ioutil.setFloatArrayBufferSize(this.n);
/*  87 */     ofs.write(ioutil.longToByteArray(this.m));
/*  88 */     ofs.write(ioutil.longToByteArray(this.n));
/*  89 */     for (int i = 0; i < this.m; i++) {
/*  90 */       ofs.write(ioutil.floatToByteArray(this.data[i]));
/*     */     }
/*     */   }
/*     */   
/*     */   public String toString() {
/*  95 */     StringBuilder builder = new StringBuilder();
/*  96 */     builder.append("Matrix [data_=");
/*  97 */     if (this.data != null) {
/*  98 */       builder.append("[");
/*  99 */       for (int i = 0; i < this.m && i < 10; i++) {
/* 100 */         for (int j = 0; j < this.n && j < 10; j++) {
/* 101 */           builder.append(this.data[i][j]).append(",");
/*     */         }
/*     */       } 
/* 104 */       builder.setLength(builder.length() - 1);
/* 105 */       builder.append("]");
/*     */     } else {
/* 107 */       builder.append("null");
/*     */     } 
/* 109 */     builder.append(", m_=");
/* 110 */     builder.append(this.m);
/* 111 */     builder.append(", n_=");
/* 112 */     builder.append(this.n);
/* 113 */     builder.append("]");
/* 114 */     return builder.toString();
/*     */   }
/*     */ }


/* Location:              /Users/davidgortega/Desktop/FastText4J.jar!/ai/searchbox/FastText4J/math/Matrix.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */