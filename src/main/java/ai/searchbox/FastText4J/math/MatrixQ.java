/*    */ package ai.searchbox.FastText4J.math;
/*    */ 
/*    */ import ai.searchbox.FastText4J.Utils;
/*    */ import ai.searchbox.FastText4J.io.IOUtil;
/*    */ import ai.searchbox.FastText4J.math.quant.ProductQuantizer;
/*    */ import ai.searchbox.FastText4J.math.quant.QCodeArray;
/*    */ import ai.searchbox.FastText4J.math.quant.QCodes;
/*    */ import java.io.IOException;
/*    */ import java.io.InputStream;
/*    */ import java.io.OutputStream;
/*    */ 
/*    */ public class MatrixQ extends Matrix {
/*    */   QCodeArray codes;
/* 14 */   ProductQuantizer pq = new ProductQuantizer();
/*    */   
/*    */   QCodeArray normCodes;
/* 17 */   ProductQuantizer npq = new ProductQuantizer();
/*    */   
/*    */   boolean qnorm;
/*    */   
/*    */   public void addToVector(Vector x, int t) {
/* 22 */     float norm = 1.0F;
/* 23 */     if (this.qnorm) {
/* 24 */       int cPosition = this.npq.getCentroidsPosition(0, this.normCodes.get(t));
/* 25 */       norm = this.npq.getCentroid(cPosition);
/*    */     } 
/* 27 */     this.pq.addCode(x, (QCodes)this.codes, t, norm);
/*    */   }
/*    */ 
/*    */   
/*    */   public float dotRow(Vector vec, int i) {
/* 32 */     Utils.checkArgument((i >= 0));
/* 33 */     Utils.checkArgument((i < this.m));
/* 34 */     Utils.checkArgument((vec.m == this.n));
/*    */     
/* 36 */     float norm = 1.0F;
/* 37 */     if (this.qnorm) {
/* 38 */       int cPosition = this.npq.getCentroidsPosition(0, this.normCodes.get(i));
/* 39 */       norm = this.npq.getCentroid(cPosition);
/*    */     } 
/*    */     
/* 42 */     return this.pq.mulCode(vec, (QCodes)this.codes, i, norm);
/*    */   }
/*    */   
/*    */   public void load(InputStream is) throws IOException {
/* 46 */     IOUtil ioutil = new IOUtil();
/*    */     
/* 48 */     this.qnorm = ioutil.readBool(is);
/* 49 */     this.m = (int)ioutil.readLong(is);
/* 50 */     this.n = (int)ioutil.readLong(is);
/*    */     
/* 52 */     int codeSize = ioutil.readInt(is);
/*    */     
/* 54 */     int[] rawCodes = new int[codeSize];
/* 55 */     for (int i = 0; i < codeSize; i++) {
/* 56 */       int c = ioutil.readByteAsInt(is);
/* 57 */       rawCodes[i] = c;
/*    */     } 
/*    */     
/* 60 */     this.codes = new QCodeArray(rawCodes);
/* 61 */     this.pq.load(is);
/*    */     
/* 63 */     if (this.qnorm) {
/* 64 */       int[] rawNormCodes = new int[this.m];
/* 65 */       for (int j = 0; j < this.m; j++) {
/* 66 */         int c = ioutil.readByteAsInt(is);
/* 67 */         rawNormCodes[j] = c;
/*    */       } 
/*    */       
/* 70 */       this.normCodes = new QCodeArray(rawNormCodes);
/* 71 */       this.npq.load(is);
/*    */     } 
/*    */   }
/*    */   
/*    */   public void save(OutputStream os) throws IOException {
/* 76 */     IOUtil ioutil = new IOUtil();
/*    */     
/* 78 */     os.write(ioutil.booleanToByteArray(this.qnorm));
/* 79 */     os.write(ioutil.longToByteArray(this.m));
/* 80 */     os.write(ioutil.longToByteArray(this.n));
/* 81 */     os.write(ioutil.intToByteArray(this.codes.size()));
/*    */     int i;
/* 83 */     for (i = 0; i < this.codes.size(); i++) {
/* 84 */       os.write(ioutil.intToByte(this.codes.get(i)));
/*    */     }
/*    */     
/* 87 */     this.pq.save(os);
/*    */     
/* 89 */     if (this.qnorm) {
/* 90 */       for (i = 0; i < this.m; i++) {
/* 91 */         os.write(ioutil.intToByte(this.normCodes.get(i)));
/*    */       }
/*    */       
/* 94 */       this.npq.save(os);
/*    */     } 
/*    */   }
/*    */ }


/* Location:              /Users/davidgortega/Desktop/FastText4J.jar!/ai/searchbox/FastText4J/math/MatrixQ.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */