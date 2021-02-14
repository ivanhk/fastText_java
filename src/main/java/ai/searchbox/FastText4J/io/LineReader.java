/*    */ package ai.searchbox.FastText4J.io;
/*    */ 
/*    */ import java.io.File;
/*    */ import java.io.IOException;
/*    */ import java.io.InputStream;
/*    */ import java.io.Reader;
/*    */ import java.io.UnsupportedEncodingException;
/*    */ import java.nio.charset.Charset;
/*    */ 
/*    */ public abstract class LineReader
/*    */   extends Reader {
/* 12 */   protected InputStream inputStream_ = null;
/* 13 */   protected File file_ = null;
/* 14 */   protected Charset charset_ = null;
/*    */ 
/*    */   
/*    */   protected LineReader() {}
/*    */ 
/*    */   
/*    */   protected LineReader(Object lock) {
/* 21 */     super(lock);
/*    */   }
/*    */   
/*    */   public LineReader(String filename, String charsetName) throws IOException, UnsupportedEncodingException {
/* 25 */     this();
/* 26 */     this.file_ = new File(filename);
/* 27 */     this.charset_ = Charset.forName(charsetName);
/*    */   }
/*    */   
/*    */   public LineReader(InputStream inputStream, String charsetName) throws UnsupportedEncodingException {
/* 31 */     this();
/* 32 */     this.inputStream_ = inputStream;
/* 33 */     this.charset_ = Charset.forName(charsetName);
/*    */   }
/*    */   
/*    */   public abstract long skipLine(long paramLong) throws IOException;
/*    */   
/*    */   public abstract String readLine() throws IOException;
/*    */   
/*    */   public abstract String[] readLineTokens() throws IOException;
/*    */   
/*    */   public abstract void rewind() throws IOException;
/*    */ }


/* Location:              /Users/davidgortega/Desktop/FastText4J.jar!/ai/searchbox/FastText4J/io/LineReader.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */