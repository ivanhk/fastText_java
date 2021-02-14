/*     */ package ai.searchbox.FastText4J.math.quant;
/*     */ 
/*     */ import ai.searchbox.FastText4J.io.IOUtil;
/*     */ import ai.searchbox.FastText4J.math.Vector;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.OutputStream;
/*     */ import java.util.Random;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class ProductQuantizer
/*     */ {
/*     */   private static final int SEED = 1234;
/*     */   private static final int NUM_BITS = 8;
/*     */   private static final int KSUB = 256;
/*     */   private static final int MAX_POINTS_PER_CLUSTER = 256;
/*     */   private static final int MAX_POINTS = 65536;
/*     */   private static final int NUM_ITER = 25;
/*     */   private static final double EPS = 1.0E-7D;
/*     */   int dim;
/*     */   int nsubq;
/*     */   int dsub;
/*     */   int lastdsub;
/*     */   float[] centroids;
/*  28 */   final Random rng = new Random(1234L);
/*     */   
/*     */   public float distL2(float[] x, float[] y, int d) {
/*  31 */     return distL2(x, y, d, 0, 0);
/*     */   }
/*     */   
/*     */   public float distL2(float[] x, float[] y, int d, int xpos, int ypos) {
/*  35 */     float dist = 0.0F;
/*  36 */     for (int i = 0; i < d; i++) {
/*  37 */       float tmp = x[i + xpos] - y[i + ypos];
/*  38 */       dist += tmp * tmp;
/*     */     } 
/*  40 */     return dist;
/*     */   }
/*     */   
/*     */   public int dim() {
/*  44 */     return this.dim;
/*     */   }
/*     */   
/*     */   public int dsub() {
/*  48 */     return this.dsub;
/*     */   }
/*     */   
/*     */   public int nsubq() {
/*  52 */     return this.nsubq;
/*     */   }
/*     */   
/*     */   public int lastdsub() {
/*  56 */     return this.lastdsub;
/*     */   }
/*     */   
/*     */   public float[] centroids() {
/*  60 */     return this.centroids;
/*     */   }
/*     */   
/*     */   public float getCentroid(int position) {
/*  64 */     return this.centroids[position];
/*     */   }
/*     */   
/*     */   public int getCentroidsPosition(int m, int i) {
/*  68 */     if (m == this.nsubq - 1) {
/*  69 */       return m * 256 * this.dsub + i * this.lastdsub;
/*     */     }
/*  71 */     return (m * 256 + i) * this.dsub;
/*     */   }
/*     */ 
/*     */   
/*     */   public void train(int n, float[] x) {
/*  76 */     throw new UnsupportedOperationException("Not implemented yet");
/*     */   }
/*     */   
/*     */   public void computeCode(float[] x, QCodes codes, int xBeginPosition, int codeBeginPosition) {
/*  80 */     throw new UnsupportedOperationException("Not implemented yet");
/*     */   }
/*     */   
/*     */   public void computeCodes(float[] x, QCodes codes, int m) {
/*  84 */     throw new UnsupportedOperationException("Not implemented yet");
/*     */   }
/*     */   
/*     */   public float mulCode(Vector x, QCodes codes, int t, float alpha) {
/*  88 */     float res = 0.0F;
/*  89 */     int d = this.dsub;
/*  90 */     int codePos = this.nsubq + t;
/*  91 */     for (int m = 0; m < this.nsubq; m++) {
/*  92 */       int c = getCentroidsPosition(m, codes.get(m + codePos));
/*  93 */       if (m == this.nsubq - 1) {
/*  94 */         d = this.lastdsub;
/*     */       }
/*  96 */       for (int n = 0; n < d; n++) {
/*  97 */         res += x.data[m * this.dsub + n] * this.centroids[c * n];
/*     */       }
/*     */     } 
/* 100 */     return res * alpha;
/*     */   }
/*     */   
/*     */   public void addCode(Vector x, QCodes codes, int t, float alpha) {
/* 104 */     int d = this.dsub;
/* 105 */     int codePos = this.nsubq * t;
/* 106 */     for (int m = 0; m < this.nsubq; m++) {
/* 107 */       int c = getCentroidsPosition(m, codes.get(m + codePos));
/* 108 */       if (m == this.nsubq - 1) {
/* 109 */         d = this.lastdsub;
/*     */       }
/* 111 */       for (int n = 0; n < d; n++) {
/* 112 */         x.data[m * this.dsub + n] = x.data[m * this.dsub + n] + alpha * this.centroids[c + n];
/*     */       }
/*     */     } 
/*     */   }
/*     */   
/*     */   public void save(OutputStream os) throws IOException {
/* 118 */     IOUtil io = new IOUtil();
/* 119 */     os.write(io.intToByteArray(this.dim));
/* 120 */     os.write(io.intToByteArray(this.nsubq));
/* 121 */     os.write(io.intToByteArray(this.dsub));
/* 122 */     os.write(io.intToByteArray(this.lastdsub));
/*     */     
/* 124 */     for (int i = 0; i < this.centroids.length; i++) {
/* 125 */       os.write(io.floatToByteArray(this.centroids[i]));
/*     */     }
/*     */   }
/*     */   
/*     */   public void load(InputStream is) throws IOException {
/* 130 */     IOUtil io = new IOUtil();
/* 131 */     this.dim = io.readInt(is);
/* 132 */     this.nsubq = io.readInt(is);
/* 133 */     this.dsub = io.readInt(is);
/* 134 */     this.lastdsub = io.readInt(is);
/*     */     
/* 136 */     this.centroids = new float[this.dim * 256];
/* 137 */     for (int i = 0; i < this.centroids.length; i++) {
/* 138 */       this.centroids[i] = io.readFloat(is);
/*     */     }
/*     */   }
/*     */   
/*     */   public static int findCentroidsSize(int dimension) {
/* 143 */     return dimension * 256;
/*     */   }
/*     */   
/*     */   private float assignCentroid(float[] x, int xStartPosition, int c0Position, QCodes codes, int codeStartPosition, int d) {
/* 147 */     throw new UnsupportedOperationException("Not implemented yet");
/*     */   }
/*     */   
/*     */   private void eStep(float[] x, int cPosition, QCodes codes, int d, int n) {
/* 151 */     throw new UnsupportedOperationException("Not implemented yet");
/*     */   }
/*     */   
/*     */   private void mStep(float[] x0, int cPosition, QCodes codes, int d, int n) {
/* 155 */     throw new UnsupportedOperationException("Not implemented yet");
/*     */   }
/*     */   
/*     */   private void kmeans(float[] x, int cPosition, int n, int d) {
/* 159 */     throw new UnsupportedOperationException("Not implemented yet");
/*     */   }
/*     */ }


/* Location:              /Users/davidgortega/Desktop/FastText4J.jar!/ai/searchbox/FastText4J/math/quant/ProductQuantizer.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */