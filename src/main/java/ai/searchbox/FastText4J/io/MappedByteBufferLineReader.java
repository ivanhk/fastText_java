/*     */ package ai.searchbox.FastText4J.io;
/*     */ 
/*     */ import java.io.BufferedInputStream;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.RandomAccessFile;
/*     */ import java.io.UnsupportedEncodingException;
/*     */ import java.nio.ByteBuffer;
/*     */ import java.nio.CharBuffer;
/*     */ import java.nio.channels.FileChannel;
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ 
/*     */ public class MappedByteBufferLineReader
/*     */   extends LineReader {
/*  16 */   private static int DEFAULT_BUFFER_SIZE = 1024;
/*     */   
/*  18 */   private volatile ByteBuffer byteBuffer_ = null;
/*  19 */   private RandomAccessFile raf_ = null;
/*  20 */   private FileChannel channel_ = null;
/*  21 */   private byte[] bytes_ = null;
/*     */   
/*  23 */   private int string_buf_size_ = DEFAULT_BUFFER_SIZE;
/*     */   
/*     */   private boolean fillLine_ = false;
/*  26 */   private StringBuilder sb_ = new StringBuilder();
/*  27 */   private List<String> tokens_ = new ArrayList<>();
/*     */ 
/*     */   
/*     */   public MappedByteBufferLineReader(String filename, String charsetName) throws IOException, UnsupportedEncodingException {
/*  31 */     super(filename, charsetName);
/*  32 */     this.raf_ = new RandomAccessFile(this.file_, "r");
/*  33 */     this.channel_ = this.raf_.getChannel();
/*  34 */     this.byteBuffer_ = this.channel_.map(FileChannel.MapMode.READ_ONLY, 0L, this.channel_.size());
/*  35 */     this.bytes_ = new byte[this.string_buf_size_];
/*     */   }
/*     */   
/*     */   public MappedByteBufferLineReader(InputStream inputStream, String charsetName) throws UnsupportedEncodingException {
/*  39 */     this(inputStream, charsetName, DEFAULT_BUFFER_SIZE);
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public MappedByteBufferLineReader(InputStream inputStream, String charsetName, int buf_size) throws UnsupportedEncodingException {
/*  45 */     super((inputStream instanceof BufferedInputStream) ? inputStream : new BufferedInputStream(inputStream), charsetName);
/*     */     
/*  47 */     this.string_buf_size_ = buf_size;
/*  48 */     this.byteBuffer_ = ByteBuffer.allocateDirect(this.string_buf_size_);
/*  49 */     this.bytes_ = new byte[this.string_buf_size_];
/*     */     
/*  51 */     if (inputStream == System.in) {
/*  52 */       this.fillLine_ = true;
/*     */     }
/*     */   }
/*     */ 
/*     */   
/*     */   public long skipLine(long n) throws IOException {
/*  58 */     if (n < 0L) {
/*  59 */       throw new IllegalArgumentException("skip value is negative");
/*     */     }
/*     */     
/*  62 */     long currentLine = 0L;
/*  63 */     long readLine = 0L;
/*  64 */     synchronized (this.lock) {
/*  65 */       ensureOpen(); String line;
/*  66 */       while (currentLine < n && (line = getLine()) != null) {
/*  67 */         readLine++;
/*  68 */         if (line == null || line.isEmpty() || line.startsWith("#")) {
/*     */           continue;
/*     */         }
/*  71 */         currentLine++;
/*     */       } 
/*     */     } 
/*  74 */     return readLine;
/*     */   }
/*     */ 
/*     */   
/*     */   public String readLine() throws IOException {
/*  79 */     synchronized (this.lock) {
/*  80 */       ensureOpen();
/*  81 */       String lineString = getLine();
/*  82 */       while (lineString != null && (lineString.isEmpty() || lineString.startsWith("#"))) {
/*  83 */         lineString = getLine();
/*     */       }
/*  85 */       return lineString;
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public String[] readLineTokens() throws IOException {
/*  91 */     synchronized (this.lock) {
/*  92 */       ensureOpen();
/*  93 */       String[] tokens = getLineTokens();
/*  94 */       while (tokens != null && ((tokens.length == 1 && tokens[0].isEmpty()) || tokens[0].startsWith("#"))) {
/*  95 */         tokens = getLineTokens();
/*     */       }
/*  97 */       return tokens;
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public void rewind() throws IOException {
/* 103 */     synchronized (this.lock) {
/* 104 */       ensureOpen();
/* 105 */       if (this.raf_ != null) {
/* 106 */         this.raf_.seek(0L);
/* 107 */         this.channel_.position(0L);
/*     */       } 
/* 109 */       this.byteBuffer_.position(0);
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public int read(char[] cbuf, int off, int len) throws IOException {
/* 115 */     synchronized (this.lock) {
/* 116 */       ensureOpen();
/* 117 */       if (off < 0 || off > cbuf.length || len < 0 || off + len > cbuf.length || off + len < 0)
/* 118 */         throw new IndexOutOfBoundsException(); 
/* 119 */       if (len == 0) {
/* 120 */         return 0;
/*     */       }
/*     */       
/* 123 */       CharBuffer charBuffer = this.byteBuffer_.asCharBuffer();
/* 124 */       int length = Math.min(len, charBuffer.remaining());
/* 125 */       charBuffer.get(cbuf, off, length);
/*     */       
/* 127 */       if (this.inputStream_ != null) {
/* 128 */         off += length;
/*     */         
/* 130 */         while (off < len) {
/* 131 */           fillByteBuffer();
/* 132 */           if (!this.byteBuffer_.hasRemaining()) {
/*     */             break;
/*     */           }
/* 135 */           charBuffer = this.byteBuffer_.asCharBuffer();
/* 136 */           length = Math.min(len, charBuffer.remaining());
/* 137 */           charBuffer.get(cbuf, off, length);
/* 138 */           off += length;
/*     */         } 
/*     */       } 
/* 141 */       return (length == len) ? len : -1;
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public void close() throws IOException {
/* 147 */     synchronized (this.lock) {
/* 148 */       if (this.raf_ != null) {
/* 149 */         this.raf_.close();
/* 150 */       } else if (this.inputStream_ != null) {
/* 151 */         this.inputStream_.close();
/*     */       } 
/* 153 */       this.channel_ = null;
/* 154 */       this.byteBuffer_ = null;
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   private void ensureOpen() throws IOException {
/* 160 */     if (this.byteBuffer_ == null)
/* 161 */       throw new IOException("Stream closed"); 
/*     */   }
/*     */   
/*     */   protected String getLine() throws IOException {
/* 165 */     fillByteBuffer();
/*     */     
/* 167 */     if (!this.byteBuffer_.hasRemaining()) {
/* 168 */       return null;
/*     */     }
/*     */     
/* 171 */     this.sb_.setLength(0);
/*     */     
/* 173 */     int b = -1;
/* 174 */     int i = -1;
/*     */     do {
/* 176 */       b = this.byteBuffer_.get();
/*     */       
/* 178 */       if ((b >= 10 && b <= 13) || b == 0) {
/*     */         break;
/*     */       }
/*     */       
/* 182 */       this.bytes_[++i] = (byte)b;
/*     */       
/* 184 */       if (i == this.string_buf_size_ - 1) {
/* 185 */         this.sb_.append(new String(this.bytes_, this.charset_));
/* 186 */         i = -1;
/*     */       } 
/*     */       
/* 189 */       fillByteBuffer();
/* 190 */     } while (this.byteBuffer_.hasRemaining());
/*     */     
/* 192 */     this.sb_.append(new String(this.bytes_, 0, i + 1, this.charset_));
/*     */     
/* 194 */     return this.sb_.toString();
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   protected String[] getLineTokens() throws IOException {
/* 200 */     fillByteBuffer();
/*     */     
/* 202 */     if (!this.byteBuffer_.hasRemaining()) {
/* 203 */       return null;
/*     */     }
/*     */     
/* 206 */     this.tokens_.clear();
/* 207 */     this.sb_.setLength(0);
/*     */     
/* 209 */     int b = -1;
/* 210 */     int i = -1;
/*     */     do {
/* 212 */       b = this.byteBuffer_.get();
/*     */       
/* 214 */       if ((b >= 10 && b <= 13) || b == 0)
/*     */         break; 
/* 216 */       if (b == 9 || b == 32) {
/* 217 */         this.sb_.append(new String(this.bytes_, 0, i + 1, this.charset_));
/* 218 */         this.tokens_.add(this.sb_.toString());
/* 219 */         this.sb_.setLength(0);
/* 220 */         i = -1;
/*     */       } else {
/* 222 */         this.bytes_[++i] = (byte)b;
/* 223 */         if (i == this.string_buf_size_ - 1) {
/* 224 */           this.sb_.append(new String(this.bytes_, this.charset_));
/* 225 */           i = -1;
/*     */         } 
/*     */       } 
/*     */       
/* 229 */       fillByteBuffer();
/* 230 */     } while (this.byteBuffer_.hasRemaining());
/*     */     
/* 232 */     this.sb_.append(new String(this.bytes_, 0, i + 1, this.charset_));
/* 233 */     this.tokens_.add(this.sb_.toString());
/*     */     
/* 235 */     return this.tokens_.<String>toArray(new String[this.tokens_.size()]);
/*     */   }
/*     */   
/*     */   private void fillByteBuffer() throws IOException {
/* 239 */     if (this.inputStream_ == null || this.byteBuffer_.hasRemaining()) {
/*     */       return;
/*     */     }
/*     */     
/* 243 */     this.byteBuffer_.clear();
/*     */     
/* 245 */     for (int i = 0; i < this.string_buf_size_; i++) {
/* 246 */       int b = this.inputStream_.read();
/*     */       
/* 248 */       if (b < 0) {
/*     */         break;
/*     */       }
/*     */       
/* 252 */       this.byteBuffer_.put((byte)b);
/*     */       
/* 254 */       if (this.fillLine_ && ((
/* 255 */         b >= 10 && b <= 13) || b == 0)) {
/*     */         break;
/*     */       }
/*     */     } 
/*     */ 
/*     */     
/* 261 */     this.byteBuffer_.flip();
/*     */   }
/*     */ }


/* Location:              /Users/davidgortega/Desktop/FastText4J.jar!/ai/searchbox/FastText4J/io/MappedByteBufferLineReader.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */