/*     */ package ai.searchbox.FastText4J.io;
/*     */ 
/*     */ import java.io.BufferedReader;
/*     */ import java.io.FileInputStream;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.InputStreamReader;
/*     */ import java.io.UnsupportedEncodingException;
/*     */ import org.apache.log4j.Logger;
/*     */ import org.apache.lucene.analysis.Analyzer;
/*     */ import org.apache.lucene.analysis.TokenStream;
/*     */ import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class BufferedLineReader
/*     */   extends LineReader
/*     */ {
/*  22 */   private static final Logger logger = Logger.getLogger(BufferedLineReader.class.getName());
/*     */   
/*  24 */   private String lineDelimitingRegex_ = " |\r|\t|\\v|\f|\000";
/*     */   
/*     */   private BufferedReader br_;
/*     */   
/*     */   public BufferedLineReader(String filename, String charsetName) throws IOException, UnsupportedEncodingException {
/*  29 */     super(filename, charsetName);
/*  30 */     FileInputStream fis = new FileInputStream(this.file_);
/*  31 */     this.br_ = new BufferedReader(new InputStreamReader(fis, this.charset_));
/*     */   }
/*     */   
/*     */   public BufferedLineReader(InputStream inputStream, String charsetName) throws UnsupportedEncodingException {
/*  35 */     super(inputStream, charsetName);
/*  36 */     this.br_ = new BufferedReader(new InputStreamReader(inputStream, this.charset_));
/*     */   }
/*     */ 
/*     */   
/*     */   public long skipLine(long n) throws IOException {
/*  41 */     if (n < 0L) {
/*  42 */       throw new IllegalArgumentException("skip value is negative");
/*     */     }
/*     */     
/*  45 */     long currentLine = 0L;
/*  46 */     long readLine = 0L;
/*  47 */     synchronized (this.lock) {
/*  48 */       String line; while (currentLine < n && (line = this.br_.readLine()) != null) {
/*  49 */         readLine++;
/*  50 */         if (line == null || line.isEmpty() || line.startsWith("#")) {
/*     */           continue;
/*     */         }
/*  53 */         currentLine++;
/*     */       } 
/*  55 */       return readLine;
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public String readLine() throws IOException {
/*  61 */     synchronized (this.lock) {
/*  62 */       String lineString = this.br_.readLine();
/*  63 */       while (lineString != null && (lineString.isEmpty() || lineString.startsWith("#"))) {
/*  64 */         lineString = this.br_.readLine();
/*     */       }
/*  66 */       return lineString;
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public String[] readLineTokens() throws IOException {
/*  72 */     logger.debug("reading tokens");
/*     */     
/*  74 */     String line = readLine();
/*  75 */     logger.debug("line readed");
/*     */     
/*  77 */     if (line == null) {
/*  78 */       return null;
/*     */     }
/*  80 */     return line.split(this.lineDelimitingRegex_, -1);
/*     */   }
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
/*     */   private String[] analyze(String text, Analyzer analyzer) throws IOException {
/* 111 */     int size = 0;
/*     */     
/* 113 */     TokenStream tokenStream1 = analyzer.tokenStream("all", text);
/* 114 */     tokenStream1.reset();
/* 115 */     while (tokenStream1.incrementToken()) {
/* 116 */       size++;
/*     */     }
/* 118 */     tokenStream1.close();
/* 119 */     logger.debug("size calculated");
/*     */     
/* 121 */     String[] result = new String[size];
/* 122 */     int index = 0;
/*     */     
/* 124 */     TokenStream tokenStream = analyzer.tokenStream("all", text);
/* 125 */     CharTermAttribute attr = (CharTermAttribute)tokenStream.addAttribute(CharTermAttribute.class);
/* 126 */     tokenStream.reset();
/* 127 */     while (tokenStream.incrementToken()) {
/* 128 */       result[index] = attr.toString();
/* 129 */       index++;
/*     */     } 
/*     */     
/* 132 */     tokenStream.close();
/*     */     
/* 134 */     logger.debug("tokens readed");
/*     */     
/* 136 */     return result;
/*     */   }
/*     */ 
/*     */   
/*     */   public int read(char[] cbuf, int off, int len) throws IOException {
/* 141 */     synchronized (this.lock) {
/* 142 */       return this.br_.read(cbuf, off, len);
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public void close() throws IOException {
/* 148 */     synchronized (this.lock) {
/* 149 */       if (this.br_ != null) {
/* 150 */         this.br_.close();
/*     */       }
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public void rewind() throws IOException {
/* 157 */     synchronized (this.lock) {
/* 158 */       if (this.br_ != null) {
/* 159 */         this.br_.close();
/*     */       }
/* 161 */       if (this.file_ != null) {
/* 162 */         FileInputStream fis = new FileInputStream(this.file_);
/* 163 */         this.br_ = new BufferedReader(new InputStreamReader(fis, this.charset_));
/*     */       }
/*     */       else {
/*     */         
/* 167 */         throw new UnsupportedOperationException("InputStream rewind not supported");
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   public String getLineDelimitingRege() {
/* 173 */     return this.lineDelimitingRegex_;
/*     */   }
/*     */   
/*     */   public void setLineDelimitingRegex(String lineDelimitingRegex) {
/* 177 */     this.lineDelimitingRegex_ = lineDelimitingRegex;
/*     */   }
/*     */ }


/* Location:              /Users/davidgortega/Desktop/FastText4J.jar!/ai/searchbox/FastText4J/io/BufferedLineReader.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */