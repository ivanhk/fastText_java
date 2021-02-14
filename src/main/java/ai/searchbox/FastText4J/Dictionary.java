/*     */ package ai.searchbox.FastText4J;
/*     */ 
/*     */ import ai.searchbox.FastText4J.io.IOUtil;
/*     */ import ai.searchbox.FastText4J.io.LineReader;
/*     */ import ai.searchbox.FastText4J.io.MappedByteBufferLineReader;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.OutputStream;
/*     */ import java.math.BigInteger;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collections;
/*     */ import java.util.Comparator;
/*     */ import java.util.HashMap;
/*     */ import java.util.Iterator;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Random;
/*     */ import org.apache.log4j.Logger;
/*     */ 
/*     */ 
/*     */ 
/*     */ public class Dictionary
/*     */ {
/*  24 */   private static final Logger logger = Logger.getLogger(Dictionary.class.getName());
/*     */   
/*     */   private static final int MAX_VOCAB_SIZE = 30000000;
/*     */   private static final int MAX_LINE_SIZE = 1024;
/*  28 */   private static final Integer WORDID_DEFAULT = Integer.valueOf(-1);
/*     */   
/*     */   private static final String EOS = "</s>";
/*     */   
/*     */   private static final String BOW = "<";
/*     */   private static final String EOW = ">";
/*  34 */   private int size = 0;
/*  35 */   private int nwords = 0;
/*  36 */   private long ntokens = 0L;
/*  37 */   private int nlabels = 0;
/*     */   
/*  39 */   protected long pruneIdxSize = -1L;
/*     */   
/*     */   Map<Integer, Integer> pruneIdx;
/*     */   
/*     */   private Args args_;
/*     */   private List<Entry> words;
/*     */   private Map<Long, Integer> word2int;
/*     */   private List<Float> pdiscard;
/*  47 */   private String charsetName_ = "UTF-8";
/*  48 */   private Class<? extends LineReader> lineReaderClass_ = (Class)MappedByteBufferLineReader.class;
/*     */ 
/*     */ 
/*     */   
/*     */   private transient Comparator<Entry> entry_comparator;
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public List<Entry> getWords() {
/*  58 */     return this.words;
/*     */   }
/*     */   
/*     */   public int nwords() {
/*  62 */     return this.nwords;
/*     */   }
/*     */   
/*     */   public int nlabels() {
/*  66 */     return this.nlabels;
/*     */   }
/*     */   
/*     */   public long ntokens() {
/*  70 */     return this.ntokens;
/*     */   }
/*     */   
/*     */   public Map<Long, Integer> getWord2int() {
/*  74 */     return this.word2int;
/*     */   }
/*     */   
/*     */   public List<Float> getPdiscard() {
/*  78 */     return this.pdiscard;
/*     */   }
/*     */   
/*     */   public int getSize() {
/*  82 */     return this.size;
/*     */   }
/*     */   
/*     */   public Args getArgs() {
/*  86 */     return this.args_;
/*     */   }
/*     */   public boolean isPruned() {
/*  89 */     return (this.pruneIdxSize >= 0L);
/*     */   }
/*     */   
/*     */   public String getCharsetName() {
/*  93 */     return this.charsetName_;
/*     */   }
/*     */   
/*     */   public void setCharsetName(String charsetName) {
/*  97 */     this.charsetName_ = charsetName;
/*     */   }
/*     */   
/*     */   public Class<? extends LineReader> getLineReaderClass() {
/* 101 */     return this.lineReaderClass_;
/*     */   }
/*     */   
/*     */   public void setLineReaderClass(Class<? extends LineReader> lineReaderClass) {
/* 105 */     this.lineReaderClass_ = lineReaderClass;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public int getId(String w) {
/* 111 */     long h = find(w);
/* 112 */     return ((Integer)Utils.<Long, Integer>mapGetOrDefault(this.word2int, Long.valueOf(h), WORDID_DEFAULT)).intValue();
/*     */   }
/*     */   
/*     */   public EntryType getType(int id) {
/* 116 */     Utils.checkArgument((id >= 0));
/* 117 */     Utils.checkArgument((id < this.size));
/*     */     
/* 119 */     return ((Entry)this.words.get(id)).type;
/*     */   }
/*     */   
/*     */   public String getWord(int id) {
/* 123 */     Utils.checkArgument((id >= 0));
/* 124 */     Utils.checkArgument((id < this.size));
/*     */     
/* 126 */     return ((Entry)this.words.get(id)).word;
/*     */   }
/*     */   
/*     */   public String getLabel(int lid) {
/* 130 */     Utils.checkArgument((lid >= 0));
/* 131 */     Utils.checkArgument((lid < this.nlabels));
/* 132 */     return ((Entry)this.words.get(lid + this.nwords)).word;
/*     */   }
/*     */   
/*     */   public long find(String w) {
/* 136 */     long h = hash(w) % 30000000L;
/* 137 */     Entry e = null;
/* 138 */     while (Utils.mapGetOrDefault(this.word2int, Long.valueOf(h), WORDID_DEFAULT) != WORDID_DEFAULT && (
/* 139 */       e = this.words.get(((Integer)this.word2int.get(Long.valueOf(h))).intValue())) != null && 
/* 140 */       !w.equals(e.word)) {
/* 141 */       h = (h + 1L) % 30000000L;
/*     */     }
/*     */     
/* 144 */     return h;
/*     */   }
/*     */   
/*     */   public void add(String w) {
/* 148 */     long h = find(w);
/*     */     
/* 150 */     if (Utils.mapGetOrDefault(this.word2int, Long.valueOf(h), WORDID_DEFAULT) == WORDID_DEFAULT) {
/* 151 */       Entry e = new Entry();
/* 152 */       e.word = w;
/* 153 */       e.count = 1L;
/* 154 */       e.type = w.startsWith(this.args_.label) ? EntryType.label : EntryType.word;
/*     */       
/* 156 */       this.words.add(e);
/* 157 */       this.word2int.put(Long.valueOf(h), Integer.valueOf(this.size++));
/*     */     } else {
/*     */       
/* 160 */       ((Entry)this.words.get(((Integer)this.word2int.get(Long.valueOf(h))).intValue())).count++;
/*     */     } 
/*     */     
/* 163 */     this.ntokens++;
/*     */   }
/*     */ 
/*     */   
/*     */   public final List<Integer> getNgrams(String word) {
/* 168 */     if (!word.equals("</s>")) {
/* 169 */       return computeNgrams("<" + word + ">");
/*     */     }
/*     */     
/* 172 */     int id = getId(word);
/* 173 */     if (id != WORDID_DEFAULT.intValue()) return getNgrams(id);
/*     */ 
/*     */     
/* 176 */     return new ArrayList<>();
/*     */   }
/*     */   
/*     */   public final List<Integer> getNgrams(int i) {
/* 180 */     Utils.checkArgument((i >= 0));
/* 181 */     Utils.checkArgument((i < this.nwords));
/*     */     
/* 183 */     return ((Entry)this.words.get(i)).subwords;
/*     */   }
/*     */   
/*     */   public void addNgrams(List<Integer> line, int n) {
/* 187 */     Utils.checkArgument((n > 0));
/*     */     
/* 189 */     int line_size = line.size();
/* 190 */     for (int i = 0; i < line_size; i++) {
/* 191 */       BigInteger h = BigInteger.valueOf(((Integer)line.get(i)).intValue());
/* 192 */       BigInteger r = BigInteger.valueOf(116049371L);
/* 193 */       BigInteger b = BigInteger.valueOf(this.args_.bucket);
/*     */       
/* 195 */       for (int j = i + 1; j < line_size && j < i + n; j++) {
/* 196 */         h = h.multiply(r).add(BigInteger.valueOf(((Integer)line.get(j)).intValue()));
/* 197 */         line.add(Integer.valueOf(this.nwords + h.remainder(b).intValue()));
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   public int getLine(String[] tokens, List<Integer> words, List<Integer> labels, Random urd) {
/* 203 */     words.clear();
/* 204 */     labels.clear();
/*     */     
/* 206 */     int ntokens = 0;
/*     */     
/* 208 */     if (tokens != null)
/* 209 */       for (int i = 0; i <= tokens.length; i++) {
/* 210 */         if (i >= tokens.length || !Utils.isEmpty(tokens[i])) {
/*     */ 
/*     */ 
/*     */           
/* 214 */           int wid = (i == tokens.length) ? getId("</s>") : getId(tokens[i]);
/* 215 */           if (wid >= 0) {
/*     */ 
/*     */ 
/*     */             
/* 219 */             ntokens++;
/*     */             
/* 221 */             EntryType type = getType(wid);
/*     */             
/* 223 */             if (type == EntryType.word && !discard(wid, Utils.randomFloat(urd, 0.0F, 1.0F))) {
/* 224 */               words.add(Integer.valueOf(wid));
/*     */             }
/*     */             
/* 227 */             if (type == EntryType.label) {
/* 228 */               labels.add(Integer.valueOf(wid - this.nwords));
/*     */             }
/*     */             
/* 231 */             if (words.size() > 1024 && this.args_.model != Args.ModelType.sup) {
/*     */               break;
/*     */             }
/*     */           } 
/*     */         } 
/*     */       }  
/* 237 */     return ntokens;
/*     */   }
/*     */ 
/*     */   
/*     */   public List<Long> countType(EntryType type) {
/* 242 */     int size = (EntryType.label == type) ? nlabels() : nwords();
/* 243 */     List<Long> counts = new ArrayList<>(size);
/*     */     
/* 245 */     for (Entry w : this.words) {
/* 246 */       if (w.type == type) counts.add(Long.valueOf(w.count));
/*     */     
/*     */     } 
/* 249 */     return counts;
/*     */   }
/*     */ 
/*     */   
/*     */   public void readFromFile(String file) throws Exception {
/* 254 */     Exception exception1 = null, exception2 = null; try { LineReader lineReader = this.lineReaderClass_
/* 255 */         .getConstructor(new Class[] { String.class, String.class
/* 256 */           }).newInstance(new Object[] { file, this.charsetName_ });
/*     */       
/* 258 */       try { long minThreshold = 1L;
/*     */         
/*     */         String[] lineTokens;
/* 261 */         while ((lineTokens = lineReader.readLineTokens()) != null) {
/* 262 */           for (int i = 0; i <= lineTokens.length; i++) {
/* 263 */             if (i == lineTokens.length) {
/* 264 */               add("</s>");
/*     */             } else {
/* 266 */               if (Utils.isEmpty(lineTokens[i])) {
/*     */                 continue;
/*     */               }
/*     */               
/* 270 */               add(lineTokens[i]);
/*     */             } 
/*     */             
/* 273 */             if (this.size > 2.25E7D) {
/* 274 */               minThreshold++;
/* 275 */               threshold(minThreshold, minThreshold);
/*     */             } 
/*     */             
/* 278 */             if (this.ntokens % 1000000L == 0L && this.args_.verbose > 1)
/* 279 */               System.out.printf("\rRead %dM words", new Object[] { Long.valueOf(this.ntokens / 1000000L) });  continue;
/*     */           } 
/*     */         }  }
/*     */       finally
/* 283 */       { if (lineReader != null) lineReader.close();  }  } finally { exception2 = null; if (exception1 == null) { exception1 = exception2; } else if (exception1 != exception2) { exception1.addSuppressed(exception2); }
/*     */        }
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 289 */     if (Args.ModelType.cbow == this.args_.model || Args.ModelType.sg == this.args_.model) {
/* 290 */       initNgrams();
/*     */     }
/*     */     
/* 293 */     if (this.args_.verbose > 0) {
/* 294 */       System.out.printf("\rRead %dM words\n", new Object[] { Long.valueOf(this.ntokens / 1000000L) });
/* 295 */       System.out.println("Number of words:  " + this.nwords);
/* 296 */       System.out.println("Number of labels: " + this.nlabels);
/*     */     } 
/*     */     
/* 299 */     if (this.size == 0) {
/* 300 */       throw new Exception("Empty vocabulary. Try a smaller -minCount value.");
/*     */     }
/*     */   }
/*     */ 
/*     */   
/*     */   public void load(InputStream is) throws IOException {
/* 306 */     IOUtil io = new IOUtil();
/* 307 */     this.size = io.readInt(is);
/* 308 */     this.nwords = io.readInt(is);
/* 309 */     this.nlabels = io.readInt(is);
/* 310 */     this.ntokens = io.readLong(is);
/* 311 */     this.pruneIdxSize = io.readLong(is);
/*     */     
/* 313 */     this.words = new ArrayList<>(this.size);
/* 314 */     this.word2int = new HashMap<>(this.size);
/*     */     int i;
/* 316 */     for (i = 0; i < this.size; i++) {
/* 317 */       Entry e = new Entry();
/* 318 */       e.word = io.readString(is);
/* 319 */       e.count = io.readLong(is);
/* 320 */       e.type = EntryType.fromValue(io.readByteAsInt(is));
/*     */       
/* 322 */       this.words.add(e);
/*     */       
/* 324 */       this.word2int.put(Long.valueOf(find(e.word)), Integer.valueOf(i));
/*     */     } 
/*     */     
/* 327 */     this.pruneIdx = new HashMap<>((int)Math.max(0L, this.pruneIdxSize));
/* 328 */     if (this.pruneIdxSize > 0L) {
/* 329 */       for (i = 0; i < this.pruneIdxSize; i++) {
/* 330 */         int first = io.readInt(is);
/* 331 */         int second = io.readInt(is);
/* 332 */         this.pruneIdx.put(Integer.valueOf(first), Integer.valueOf(second));
/*     */       } 
/*     */     }
/*     */     
/* 336 */     initTableDiscard();
/* 337 */     initNgrams();
/*     */   }
/*     */   
/*     */   public void save(OutputStream ofs) throws IOException {
/* 341 */     IOUtil io = new IOUtil();
/* 342 */     ofs.write(io.intToByteArray(this.size));
/* 343 */     ofs.write(io.intToByteArray(this.nwords));
/* 344 */     ofs.write(io.intToByteArray(this.nlabels));
/* 345 */     ofs.write(io.longToByteArray(this.ntokens));
/* 346 */     ofs.write(io.longToByteArray(this.pruneIdxSize));
/*     */     
/* 348 */     for (int i = 0; i < this.size; i++) {
/* 349 */       Entry e = this.words.get(i);
/* 350 */       ofs.write(e.word.getBytes());
/* 351 */       ofs.write(0);
/* 352 */       ofs.write(io.longToByteArray(e.count));
/* 353 */       ofs.write(io.intToByte(e.type.value));
/*     */     } 
/*     */   }
/*     */   
/*     */   public void threshold(long t, long tl) {
/* 358 */     Collections.sort(this.words, this.entry_comparator);
/*     */     
/* 360 */     Iterator<Entry> iterator = this.words.iterator();
/* 361 */     while (iterator.hasNext()) {
/* 362 */       Entry _entry = iterator.next();
/* 363 */       if ((EntryType.word == _entry.type && _entry.count < t) || (
/* 364 */         EntryType.label == _entry.type && _entry.count < tl)) {
/* 365 */         iterator.remove();
/*     */       }
/*     */     } 
/*     */     
/* 369 */     ((ArrayList)this.words).trimToSize();
/* 370 */     this.size = 0;
/* 371 */     this.nwords = 0;
/* 372 */     this.nlabels = 0;
/* 373 */     this.word2int = new HashMap<>(this.words.size());
/* 374 */     for (Entry _entry : this.words) {
/* 375 */       long h = find(_entry.word);
/* 376 */       this.word2int.put(Long.valueOf(h), Integer.valueOf(this.size++));
/*     */       
/* 378 */       if (EntryType.word == _entry.type) {
/* 379 */         this.nwords++; continue;
/*     */       } 
/* 381 */       this.nlabels++;
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public long hash(String str) {
/* 387 */     int h = -2128831035; byte b; int i; byte[] arrayOfByte;
/* 388 */     for (i = (arrayOfByte = str.getBytes()).length, b = 0; b < i; ) { byte strByte = arrayOfByte[b];
/* 389 */       h = (h ^ strByte) * 16777619;
/*     */       
/*     */       b++; }
/*     */     
/* 393 */     return h & 0xFFFFFFFFL;
/*     */   }
/*     */ 
/*     */   
/*     */   public String toString() {
/* 398 */     StringBuilder builder = new StringBuilder();
/* 399 */     builder.append("Dictionary [words_=");
/* 400 */     builder.append(this.words);
/* 401 */     builder.append(", pdiscard_=");
/* 402 */     builder.append(this.pdiscard);
/* 403 */     builder.append(", word2int_=");
/* 404 */     builder.append(this.word2int);
/* 405 */     builder.append(", size_=");
/* 406 */     builder.append(this.size);
/* 407 */     builder.append(", nwords_=");
/* 408 */     builder.append(this.nwords);
/* 409 */     builder.append(", nlabels_=");
/* 410 */     builder.append(this.nlabels);
/* 411 */     builder.append(", ntokens_=");
/* 412 */     builder.append(this.ntokens);
/* 413 */     builder.append("]");
/* 414 */     return builder.toString();
/*     */   }
/*     */ 
/*     */   
/*     */   private void initTableDiscard() {
/* 419 */     this.pdiscard = new ArrayList<>(this.size);
/* 420 */     for (int i = 0; i < this.size; i++) {
/* 421 */       float f = (float)((Entry)this.words.get(i)).count / (float)this.ntokens;
/* 422 */       this.pdiscard.add(Float.valueOf((float)(Math.sqrt(this.args_.t / f) + this.args_.t / f)));
/*     */     } 
/*     */   }
/*     */   
/*     */   private void initNgrams() {
/* 427 */     for (int i = 0; i < this.size; i++) {
/* 428 */       String word = "<" + ((Entry)this.words.get(i)).word + ">";
/*     */       
/* 430 */       Entry e = this.words.get(i);
/* 431 */       e.subwords = new ArrayList<>();
/*     */       
/* 433 */       if (!((Entry)this.words.get(i)).word.equals("</s>")) {
/* 434 */         e.subwords = computeNgrams(word);
/*     */       }
/*     */       
/* 437 */       e.subwords.add(Integer.valueOf(i));
/*     */     } 
/*     */   }
/*     */   
/*     */   private boolean charMatches(char ch) {
/* 442 */     return !(ch != ' ' && ch != '\t' && ch != '\n' && ch != '\f' && ch != '\r');
/*     */   }
/*     */   
/*     */   private boolean discard(int id, float rand) {
/* 446 */     Utils.checkArgument((id >= 0));
/* 447 */     Utils.checkArgument((id < this.nwords));
/*     */     
/* 449 */     return (this.args_.model == Args.ModelType.sup) ? false : ((rand > ((Float)this.pdiscard.get(id)).floatValue()));
/*     */   }
/*     */   
/*     */   private List<Integer> computeNgrams(String word) {
/* 453 */     List<Integer> ngrams = new ArrayList<>();
/*     */     
/* 455 */     if (word.equals("</s>")) {
/* 456 */       return ngrams;
/*     */     }
/*     */     
/* 459 */     for (int i = 0; i < word.length(); i++) {
/* 460 */       StringBuilder ngram = new StringBuilder();
/*     */       
/* 462 */       if (!charMatches(word.charAt(i)))
/*     */       {
/*     */ 
/*     */         
/* 466 */         for (int j = i, n = 1; j < word.length() && n <= this.args_.maxn; n++) {
/* 467 */           ngram.append(word.charAt(j++));
/*     */           
/* 469 */           while (j < word.length() && charMatches(word.charAt(j))) {
/* 470 */             ngram.append(word.charAt(j++));
/*     */           }
/*     */           
/* 473 */           if (n >= this.args_.minn && (n != 1 || (i != 0 && j != word.length()))) {
/* 474 */             int h = (int)(this.nwords + hash(ngram.toString()) % this.args_.bucket);
/*     */             
/* 476 */             if (h < 0) {
/* 477 */               logger.error("computeNgrams h<0: " + h + " on word: " + word);
/*     */             }
/*     */             
/* 480 */             pushHash(ngrams, h);
/*     */           } 
/*     */         } 
/*     */       }
/*     */     } 
/* 485 */     return ngrams;
/*     */   }
/*     */   
/*     */   private void pushHash(List<Integer> hashes, int id) {
/* 489 */     if (this.pruneIdxSize == 0L || id < 0) {
/*     */       return;
/*     */     }
/*     */     
/* 493 */     if (this.pruneIdxSize > 0L) {
/* 494 */       int pruneId = getPruning(id);
/* 495 */       if (pruneId >= 0) {
/* 496 */         id = pruneId;
/*     */       } else {
/*     */         return;
/*     */       } 
/*     */     } 
/*     */ 
/*     */     
/* 503 */     hashes.add(Integer.valueOf(id));
/*     */   }
/*     */   
/*     */   private int getPruning(int id) {
/* 507 */     return ((Integer)this.pruneIdx.getOrDefault(Integer.valueOf(id), Integer.valueOf(-1))).intValue();
/*     */   }
/*     */   
/*     */   public Dictionary(Args args) {
/* 511 */     this.entry_comparator = new Comparator<Entry>()
/*     */       {
/*     */         public int compare(Dictionary.Entry o1, Dictionary.Entry o2) {
/* 514 */           int cmp = (o1.type.value < o2.type.value) ? -1 : ((o1.type.value == o2.type.value) ? 0 : 1);
/*     */           
/* 516 */           if (cmp == 0) {
/* 517 */             cmp = (o2.count < o1.count) ? -1 : ((o2.count == o1.count) ? 0 : 1);
/*     */           }
/*     */           
/* 520 */           return cmp;
/*     */         }
/*     */       };
/*     */     this.args_ = args;
/*     */     this.words = new ArrayList<>(30000000);
/*     */     this.word2int = new HashMap<>(30000000);
/*     */   }
/* 527 */   public enum EntryType { word(0), label(1);
/*     */     
/*     */     private int value;
/*     */     
/*     */     public int getValue() {
/* 532 */       return this.value;
/*     */     }
/*     */     
/*     */     public static EntryType fromValue(int value) throws IllegalArgumentException {
/*     */       try {
/* 537 */         return values()[value];
/* 538 */       } catch (ArrayIndexOutOfBoundsException e) {
/* 539 */         throw new IllegalArgumentException("Unknown entry_type enum value :" + value);
/*     */       } 
/*     */     }
/*     */ 
/*     */     
/*     */     public String toString() {
/* 545 */       return (this.value == 0) ? "word" : ((this.value == 1) ? "label" : "unknown");
/*     */     }
/*     */     
/*     */     EntryType(int value) {
/* 549 */       this.value = value;
/*     */     } }
/*     */ 
/*     */   
/*     */   public class Entry
/*     */   {
/*     */     public String word;
/*     */     public Dictionary.EntryType type;
/*     */     public long count;
/*     */     public List<Integer> subwords;
/*     */     
/*     */     public String toString() {
/* 561 */       StringBuilder builder = new StringBuilder();
/* 562 */       builder.append("entry [word=");
/* 563 */       builder.append(this.word);
/* 564 */       builder.append(", count=");
/* 565 */       builder.append(this.count);
/* 566 */       builder.append(", type=");
/* 567 */       builder.append(this.type);
/* 568 */       builder.append(", subwords=");
/* 569 */       builder.append(this.subwords);
/* 570 */       builder.append("]");
/* 571 */       return builder.toString();
/*     */     }
/*     */   }
/*     */ }


/* Location:              /Users/davidgortega/Desktop/FastText4J.jar!/ai/searchbox/FastText4J/Dictionary.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */