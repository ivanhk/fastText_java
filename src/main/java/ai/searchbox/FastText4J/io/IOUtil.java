/*     */ package ai.searchbox.FastText4J.io;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.nio.ByteBuffer;
/*     */ import java.nio.ByteOrder;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class IOUtil
/*     */ {
/*  15 */   private int string_buf_size_ = 128;
/*  16 */   private byte[] bool_bytes_ = new byte[1];
/*  17 */   private byte[] int_bytes_ = new byte[4];
/*  18 */   private byte[] long_bytes_ = new byte[8];
/*  19 */   private byte[] float_bytes_ = new byte[4];
/*  20 */   private byte[] double_bytes_ = new byte[8];
/*  21 */   private byte[] string_bytes_ = new byte[this.string_buf_size_];
/*  22 */   private StringBuilder stringBuilder_ = new StringBuilder();
/*  23 */   private ByteBuffer float_array_bytebuffer_ = null;
/*  24 */   private byte[] float_array_bytes_ = null;
/*     */ 
/*     */ 
/*     */   
/*     */   public void setStringBufferSize(int size) {
/*  29 */     this.string_buf_size_ = size;
/*  30 */     this.string_bytes_ = new byte[this.string_buf_size_];
/*     */   }
/*     */   
/*     */   public void setFloatArrayBufferSize(int itemSize) {
/*  34 */     this.float_array_bytebuffer_ = ByteBuffer.allocate(itemSize * 4).order(ByteOrder.LITTLE_ENDIAN);
/*  35 */     this.float_array_bytes_ = new byte[itemSize * 4];
/*     */   }
/*     */   
/*     */   public byte readByte(InputStream is) throws IOException {
/*  39 */     return (byte)is.read();
/*     */   }
/*     */   
/*     */   public int readByteAsInt(InputStream is) throws IOException {
/*  43 */     return readByte(is) & 0xFF;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean readBool(InputStream is) throws IOException {
/*  50 */     int ch = readByte(is);
/*  51 */     return (ch != 0);
/*     */   }
/*     */   
/*     */   public int readInt(InputStream is) throws IOException {
/*  55 */     is.read(this.int_bytes_);
/*  56 */     return getInt(this.int_bytes_);
/*     */   }
/*     */   
/*     */   public int getInt(byte[] b) {
/*  60 */     return (b[0] & 0xFF) << 0 | (b[1] & 0xFF) << 8 | (b[2] & 0xFF) << 16 | (b[3] & 0xFF) << 24;
/*     */   }
/*     */   
/*     */   public long readLong(InputStream is) throws IOException {
/*  64 */     is.read(this.long_bytes_);
/*  65 */     return getLong(this.long_bytes_);
/*     */   }
/*     */   
/*     */   public long getLong(byte[] b) {
/*  69 */     return (b[0] & 0xFFL) << 0L | (b[1] & 0xFFL) << 8L | (
/*  70 */       b[2] & 0xFFL) << 16L | (b[3] & 0xFFL) << 24L | (
/*  71 */       b[4] & 0xFFL) << 32L | (b[5] & 0xFFL) << 40L | (
/*  72 */       b[6] & 0xFFL) << 48L | (b[7] & 0xFFL) << 56L;
/*     */   }
/*     */   
/*     */   public float readFloat(InputStream is) throws IOException {
/*  76 */     is.read(this.float_bytes_);
/*  77 */     return getFloat(this.float_bytes_);
/*     */   }
/*     */   
/*     */   public void readFloat(InputStream is, float[] data) throws IOException {
/*  81 */     is.read(this.float_array_bytes_);
/*  82 */     this.float_array_bytebuffer_.clear();
/*  83 */     ((ByteBuffer)this.float_array_bytebuffer_.put(this.float_array_bytes_).flip()).asFloatBuffer().get(data);
/*     */   }
/*     */   
/*     */   public float getFloat(byte[] b) {
/*  87 */     return 
/*  88 */       Float.intBitsToFloat((b[0] & 0xFF) << 0 | (b[1] & 0xFF) << 8 | (b[2] & 0xFF) << 16 | (b[3] & 0xFF) << 24);
/*     */   }
/*     */   
/*     */   public double readDouble(InputStream is) throws IOException {
/*  92 */     is.read(this.double_bytes_);
/*  93 */     return getDouble(this.double_bytes_);
/*     */   }
/*     */   
/*     */   public double getDouble(byte[] b) {
/*  97 */     return Double.longBitsToDouble(getLong(b));
/*     */   }
/*     */   
/*     */   public String readString(InputStream is) throws IOException {
/* 101 */     int b = is.read();
/* 102 */     if (b < 0) {
/* 103 */       return null;
/*     */     }
/* 105 */     int i = -1;
/* 106 */     this.stringBuilder_.setLength(0);
/*     */     
/* 108 */     while (b > -1 && b != 32 && b != 10 && b != 0) {
/* 109 */       this.string_bytes_[++i] = (byte)b;
/* 110 */       b = is.read();
/* 111 */       if (i == this.string_buf_size_ - 1) {
/* 112 */         this.stringBuilder_.append(new String(this.string_bytes_));
/* 113 */         i = -1;
/*     */       } 
/*     */     } 
/* 116 */     this.stringBuilder_.append(new String(this.string_bytes_, 0, i + 1));
/* 117 */     return this.stringBuilder_.toString();
/*     */   }
/*     */   
/*     */   public byte intToByte(int i) {
/* 121 */     return (byte)(i & 0xFF);
/*     */   }
/*     */   
/*     */   public byte[] intToByteArray(int i) {
/* 125 */     this.int_bytes_[0] = (byte)(i >> 0 & 0xFF);
/* 126 */     this.int_bytes_[1] = (byte)(i >> 8 & 0xFF);
/* 127 */     this.int_bytes_[2] = (byte)(i >> 16 & 0xFF);
/* 128 */     this.int_bytes_[3] = (byte)(i >> 24 & 0xFF);
/* 129 */     return this.int_bytes_;
/*     */   }
/*     */   
/*     */   public byte[] longToByteArray(long l) {
/* 133 */     this.long_bytes_[0] = (byte)(int)(l >> 0L & 0xFFL);
/* 134 */     this.long_bytes_[1] = (byte)(int)(l >> 8L & 0xFFL);
/* 135 */     this.long_bytes_[2] = (byte)(int)(l >> 16L & 0xFFL);
/* 136 */     this.long_bytes_[3] = (byte)(int)(l >> 24L & 0xFFL);
/* 137 */     this.long_bytes_[4] = (byte)(int)(l >> 32L & 0xFFL);
/* 138 */     this.long_bytes_[5] = (byte)(int)(l >> 40L & 0xFFL);
/* 139 */     this.long_bytes_[6] = (byte)(int)(l >> 48L & 0xFFL);
/* 140 */     this.long_bytes_[7] = (byte)(int)(l >> 56L & 0xFFL);
/*     */     
/* 142 */     return this.long_bytes_;
/*     */   }
/*     */   
/*     */   public byte[] floatToByteArray(float f) {
/* 146 */     return intToByteArray(Float.floatToIntBits(f));
/*     */   }
/*     */   
/*     */   public byte[] floatToByteArray(float[] f) {
/* 150 */     this.float_array_bytebuffer_.clear();
/* 151 */     this.float_array_bytebuffer_.asFloatBuffer().put(f);
/* 152 */     return this.float_array_bytebuffer_.array();
/*     */   }
/*     */   
/*     */   public byte[] doubleToByteArray(double d) {
/* 156 */     return longToByteArray(Double.doubleToRawLongBits(d));
/*     */   }
/*     */   
/*     */   public byte[] booleanToByteArray(boolean b) {
/* 160 */     return new byte[] { (byte)(b ? 1 : 0) };
/*     */   }
/*     */ }


/* Location:              /Users/davidgortega/Desktop/FastText4J.jar!/ai/searchbox/FastText4J/io/IOUtil.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */