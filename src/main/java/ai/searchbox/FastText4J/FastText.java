/*     */ package ai.searchbox.FastText4J;
/*     */ 
/*     */ import ai.searchbox.FastText4J.io.LineReader;
/*     */ import ai.searchbox.FastText4J.io.MappedByteBufferLineReader;
/*     */ import ai.searchbox.FastText4J.math.Matrix;
/*     */ import ai.searchbox.FastText4J.math.Vector;
/*     */ import com.google.common.collect.MinMaxPriorityQueue;
/*     */ import java.io.File;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Comparator;
/*     */ import java.util.List;
/*     */ import java.util.Set;
/*     */ import java.util.concurrent.atomic.AtomicLong;
/*     */ import org.apache.log4j.Logger;
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
/*     */ public class FastText
/*     */ {
/*  30 */   private static final Logger logger = Logger.getLogger(FastText.class.getName());
/*     */   
/*  32 */   public static int FASTTEXT_VERSION = 12;
/*  33 */   public static int FASTTEXT_FILEFORMAT_MAGIC_INT = 793712314;
/*     */   
/*     */   private long start_;
/*     */   
/*     */   int threadCount;
/*     */   
/*     */   long threadFileSize;
/*     */   
/*     */   private Args args_;
/*     */   private Dictionary dict_;
/*     */   private Model model_;
/*     */   private Matrix input_;
/*     */   private Matrix output_;
/*  46 */   private Matrix wordVectors = null;
/*  47 */   private Matrix wordVectorsOut = null;
/*     */   
/*     */   private boolean isQuant = false;
/*     */   
/*     */   private AtomicLong tokenCount_;
/*     */   
/*  53 */   private String charsetName_ = "UTF-8";
/*  54 */   private Class<? extends LineReader> lineReaderClass_ = (Class)MappedByteBufferLineReader.class;
/*     */   
/*     */   public Args getArgs() {
/*  57 */     return this.args_;
/*     */   }
/*     */   
/*     */   public void setArgs(Args args) {
/*  61 */     this.args_ = args;
/*  62 */     this.dict_ = new Dictionary(args);
/*     */   }
/*     */   public Dictionary dict() {
/*  65 */     return this.dict_;
/*     */   }
/*     */   public Vector getWordVectorIn(String word) {
/*  68 */     Vector vec = new Vector(this.args_.dim);
/*  69 */     vec.zero();
/*     */     
/*  71 */     List<Integer> ngrams = this.dict_.getNgrams(word);
/*     */     
/*  73 */     for (Integer it : ngrams) {
/*  74 */       vec.addRow(this.input_, it.intValue());
/*     */     }
/*     */     
/*  77 */     if (ngrams.size() > 0) {
/*  78 */       vec.mul(1.0F / ngrams.size());
/*     */     }
/*     */     
/*  81 */     return vec;
/*     */   }
/*     */   
/*     */   public Vector getWordVectorOut(String word) {
/*  85 */     int id = this.dict_.getId(word);
/*     */     
/*  87 */     Vector vec = new Vector(this.args_.dim);
/*  88 */     vec.zero();
/*     */     
/*  90 */     if (this.isQuant) return vec;
/*     */     
/*  92 */     vec.addRow(this.output_, id);
/*     */     
/*  94 */     return vec;
/*     */   }
/*     */   
/*     */   public Vector getSentenceVector(List<String> sentence) {
/*  98 */     Vector svec = new Vector(this.args_.dim);
/*  99 */     svec.zero();
/*     */     
/* 101 */     if (this.args_.model == Args.ModelType.sup) {
/* 102 */       List<Integer> tokens = new ArrayList<>();
/* 103 */       List<Integer> labels = new ArrayList<>();
/* 104 */       this.dict_.getLine(sentence.<String>toArray(new String[sentence.size()]), tokens, labels, this.model_.rng);
/*     */       
/* 106 */       for (int i = 0; i < tokens.size(); i++) {
/* 107 */         svec.addRow(this.input_, ((Integer)tokens.get(i)).intValue());
/*     */       }
/*     */       
/* 110 */       if (!tokens.isEmpty()) {
/* 111 */         svec.mul(1.0F / tokens.size());
/*     */       }
/*     */     } else {
/*     */       
/* 115 */       int count = 0;
/* 116 */       for (String word : sentence) {
/* 117 */         Vector vec = getWordVectorIn(word);
/*     */         
/* 119 */         svec.addVector(vec);
/* 120 */         count++;
/*     */       } 
/*     */       
/* 123 */       if (count > 0) {
/* 124 */         svec.mul(1.0F / count);
/*     */       }
/*     */     } 
/*     */     
/* 128 */     return svec;
/*     */   }
/*     */   
/*     */   public Vector getSentenceVectorOut(List<String> sentence) {
/* 132 */     Vector svec = new Vector(this.args_.dim);
/* 133 */     svec.zero();
/*     */     
/* 135 */     int count = 0;
/* 136 */     for (String word : sentence) {
/* 137 */       Vector vec = getWordVectorOut(word);
/*     */       
/* 139 */       svec.addVector(vec);
/* 140 */       count++;
/*     */     } 
/*     */     
/* 143 */     if (count > 0) {
/* 144 */       svec.mul(1.0F / count);
/*     */     }
/*     */     
/* 147 */     return svec;
/*     */   }
/*     */   
/*     */   public List<Pair<Float, String>> predict(String[] lineTokens, int k) {
/* 151 */     List<Pair<Float, String>> predictions = new ArrayList<>();
/*     */     
/* 153 */     List<Integer> words = new ArrayList<>();
/* 154 */     List<Integer> labels = new ArrayList<>();
/*     */     
/* 156 */     this.dict_.getLine(lineTokens, words, labels, this.model_.rng);
/* 157 */     this.dict_.addNgrams(words, this.args_.wordNgrams);
/*     */     
/* 159 */     if (words.isEmpty()) return predictions;
/*     */     
/* 161 */     List<Pair<Float, Integer>> modelPredictions = new ArrayList<>(k + 1);
/* 162 */     this.model_.predict(words, k, modelPredictions);
/*     */     
/* 164 */     for (Pair<Float, Integer> pair : modelPredictions) {
/* 165 */       predictions.add(new Pair<>(pair.getKey(), this.dict_.getLabel(((Integer)pair.getValue()).intValue())));
/*     */     }
/*     */     
/* 168 */     return predictions;
/*     */   }
/*     */   
/*     */   public List<FastTextSynonym> findNN(Vector queryVec, int k, Set<String> banSet) {
/* 172 */     return findNN(this.wordVectors, queryVec, k, banSet);
/*     */   }
/*     */   
/*     */   public List<FastTextSynonym> findNNOut(Vector queryVec, int k, Set<String> banSet) {
/* 176 */     return findNN(this.wordVectorsOut, queryVec, k, banSet);
/*     */   }
/*     */   
/*     */   public List<FastTextSynonym> findNN(Matrix wordVectors, Vector queryVec, int k, Set<String> banSet) {
/* 180 */     MinMaxPriorityQueue<Pair<Float, String>> heap = 
/* 181 */       MinMaxPriorityQueue.orderedBy(new HeapComparator())
/* 182 */       .expectedSize(this.dict_.nlabels())
/* 183 */       .create();
/*     */     
/* 185 */     float queryNorm = queryVec.norm();
/* 186 */     if (queryNorm > 0.0F) {
/* 187 */       queryVec.mul(1.0F / queryNorm);
/*     */     }
/*     */     
/* 190 */     for (int i = 0; i < this.dict_.nwords(); i++) {
/* 191 */       String word = this.dict_.getWord(i);
/* 192 */       float dp = wordVectors.dotRow(queryVec, i);
/* 193 */       heap.add(new Pair<>(Float.valueOf(dp), word));
/*     */     } 
/*     */     
/* 196 */     List<FastTextSynonym> syns = new ArrayList<>();
/* 197 */     int j = 0;
/* 198 */     while (j < k && heap.size() > 0) {
/* 199 */       Pair<Float, String> synonym = (Pair<Float, String>)heap.pollFirst();
/* 200 */       boolean banned = banSet.contains(synonym.getValue());
/* 201 */       if (!banned) {
/* 202 */         syns.add(new FastTextSynonym(synonym.getValue(), ((Float)synonym.getKey()).floatValue()));
/* 203 */         j++;
/*     */       } 
/*     */     } 
/*     */     
/* 207 */     return syns;
/*     */   }
/*     */   
/*     */   public void saveModel() throws IOException {
/* 211 */     if (Utils.isEmpty(this.args_.output)) {
/* 212 */       if (this.args_.verbose > 1) {
/* 213 */         System.out.println("output is empty, skip save model file");
/*     */       }
/*     */       
/*     */       return;
/*     */     } 
/* 218 */     File file = new File(String.valueOf(this.args_.output) + ".bin");
/*     */     
/* 220 */     if (file.exists()) file.delete(); 
/* 221 */     if (file.getParentFile() != null) file.getParentFile().mkdirs();
/*     */     
/* 223 */     if (this.args_.verbose > 1) {
/* 224 */       System.out.println("Saving model to " + file.getCanonicalPath().toString());
/*     */     }
/*     */     
/* 227 */     Exception exception1 = null, exception2 = null;
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
/*     */   public void loadModel(String filename) throws IOException {
/* 244 */     logger.info("Loading " + filename);
/* 245 */     File file = new File(filename);
/*     */     
/* 247 */     if (!file.exists() || !file.isFile() || !file.canRead()) {
/* 248 */       throw new IOException("Model file cannot be opened for loading!");
/*     */     }
/*     */     
/* 251 */     Exception exception1 = null, exception2 = null;
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
/*     */ 
/*     */   
/*     */   public void train() throws Exception {
/* 312 */     if ("-".equals(this.args_.input)) {
/* 313 */       throw new IOException("Cannot use stdin for training!");
/*     */     }
/*     */     
/* 316 */     File file = new File(this.args_.input);
/* 317 */     if (!file.exists() || !file.isFile() || !file.canRead()) {
/* 318 */       throw new IOException("Input file cannot be opened! " + this.args_.input);
/*     */     }
/*     */     
/* 321 */     logger.debug("Building dict");
/* 322 */     this.dict_ = new Dictionary(this.args_);
/* 323 */     this.dict_.setCharsetName(this.charsetName_);
/* 324 */     this.dict_.setLineReaderClass(this.lineReaderClass_);
/* 325 */     this.dict_.readFromFile(this.args_.input);
/*     */     
/* 327 */     logger.debug("Building input matrix");
/* 328 */     if (!Utils.isEmpty(this.args_.pretrainedVectors)) {
/* 329 */       loadVecFile();
/*     */     } else {
/* 331 */       this.input_ = new Matrix(this.dict_.nwords() + this.args_.bucket, this.args_.dim);
/* 332 */       this.input_.uniform(1.0F / this.args_.dim);
/*     */     } 
/*     */     
/* 335 */     logger.debug("Building output matrix");
/* 336 */     int m = (this.args_.model == Args.ModelType.sup) ? this.dict_.nlabels() : this.dict_.nwords();
/* 337 */     this.output_ = new Matrix(m, this.args_.dim);
/* 338 */     this.output_.zero();
/*     */     
/* 340 */     this.start_ = System.currentTimeMillis();
/* 341 */     this.tokenCount_ = new AtomicLong(0L);
/* 342 */     long t0 = System.currentTimeMillis();
/*     */     
/* 344 */     this.threadFileSize = Utils.sizeLine(this.args_.input);
/* 345 */     this.threadCount = this.args_.thread;
/* 346 */     for (int i = 0; i < this.args_.thread; i++) {
/* 347 */       logger.debug("Spawning training thread");
/* 348 */       Thread t = new TrainThread(this, i);
/* 349 */       t.setUncaughtExceptionHandler(this.trainThreadExcpetionHandler);
/* 350 */       t.start();
/*     */     } 
/*     */     
/* 353 */     synchronized (this) {
/* 354 */       while (this.threadCount > 0) {
/*     */         try {
/* 356 */           wait();
/* 357 */         } catch (InterruptedException interruptedException) {}
/*     */       } 
/*     */     } 
/*     */     
/* 361 */     this.model_ = new Model(this.input_, this.output_, this.args_, 0);
/*     */     
/* 363 */     if (this.args_.verbose > 1) {
/* 364 */       long trainTime = (System.currentTimeMillis() - t0) / 1000L;
/* 365 */       System.out.printf("\nTrain time used: %d sec\n", new Object[] { Long.valueOf(trainTime) });
/*     */     } 
/*     */     
/* 368 */     logger.debug("Saving fasttext");
/* 369 */     saveModel();
/* 370 */     if (this.args_.model != Args.ModelType.sup) {
/* 371 */       saveVecFile();
/*     */     }
/*     */   }
/*     */   
/*     */   public void test(InputStream in, int k) throws IOException, Exception {
/* 376 */     int nexamples = 0, nlabels = 0;
/* 377 */     double precision = 0.0D;
/* 378 */     List<Integer> line = new ArrayList<>();
/* 379 */     List<Integer> labels = new ArrayList<>();
/*     */     
/* 381 */     LineReader lineReader = null;
/*     */     try {
/* 383 */       lineReader = this.lineReaderClass_.getConstructor(new Class[] { InputStream.class, String.class }).newInstance(new Object[] { in, this.charsetName_ });
/*     */       String[] lineTokens;
/* 385 */       while ((lineTokens = lineReader.readLineTokens()) != null && (
/* 386 */         lineTokens.length != 1 || !"quit".equals(lineTokens[0])))
/*     */       {
/*     */         
/* 389 */         this.dict_.getLine(lineTokens, line, labels, this.model_.rng);
/* 390 */         this.dict_.addNgrams(line, this.args_.wordNgrams);
/* 391 */         if (labels.size() > 0 && line.size() > 0) {
/* 392 */           List<Pair<Float, Integer>> modelPredictions = new ArrayList<>();
/* 393 */           this.model_.predict(line, k, modelPredictions);
/* 394 */           for (Pair<Float, Integer> pair : modelPredictions) {
/* 395 */             if (labels.contains(pair.getValue())) {
/* 396 */               precision++;
/*     */             }
/*     */           } 
/* 399 */           nexamples++;
/* 400 */           nlabels += labels.size();
/*     */         }
/*     */       
/*     */       }
/*     */     
/*     */     } finally {
/*     */       
/* 407 */       if (lineReader != null) {
/* 408 */         lineReader.close();
/*     */       }
/*     */     } 
/*     */     
/* 412 */     System.out.printf("P@%d: %.3f%n", new Object[] { Integer.valueOf(k), Double.valueOf(precision / (k * nexamples)) });
/* 413 */     System.out.printf("R@%d: %.3f%n", new Object[] { Integer.valueOf(k), Double.valueOf(precision / nlabels) });
/* 414 */     System.out.println("Number of examples: " + nexamples);
/*     */   }
/*     */   
/*     */   void cbow(Model model, float lr, List<Integer> line) {
/* 418 */     List<Integer> bow = new ArrayList<>();
/* 419 */     for (int w = 0; w < line.size(); w++) {
/* 420 */       bow.clear();
/*     */       
/* 422 */       int boundary = Utils.randomInt(model.rng, 1, this.args_.ws);
/* 423 */       for (int c = -boundary; c <= boundary; c++) {
/* 424 */         if (c != 0 && w + c >= 0 && w + c < line.size()) {
/* 425 */           List<Integer> ngrams = this.dict_.getNgrams(((Integer)line.get(w + c)).intValue());
/* 426 */           bow.addAll(ngrams);
/*     */         } 
/*     */       } 
/*     */       
/* 430 */       model.update(bow, ((Integer)line.get(w)).intValue(), lr);
/*     */     } 
/*     */   }
/*     */   
/*     */   void skipgram(Model model, float lr, List<Integer> line) {
/* 435 */     for (int w = 0; w < line.size(); w++) {
/* 436 */       int boundary = Utils.randomInt(model.rng, 1, this.args_.ws);
/* 437 */       List<Integer> ngrams = this.dict_.getNgrams(((Integer)line.get(w)).intValue());
/*     */       
/* 439 */       for (int c = -boundary; c <= boundary; c++) {
/* 440 */         if (c != 0 && w + c >= 0 && w + c < line.size()) {
/* 441 */           model.update(ngrams, ((Integer)line.get(w + c)).intValue(), lr);
/*     */         }
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   void supervised(Model model, float lr, List<Integer> line, List<Integer> labels) {
/* 448 */     if (labels.size() == 0 || line.size() == 0) {
/*     */       return;
/*     */     }
/* 451 */     int i = Utils.randomInt(model.rng, 1, labels.size()) - 1;
/*     */     
/* 453 */     model.update(line, ((Integer)labels.get(i)).intValue(), lr);
/*     */   }
/*     */   
/*     */   void checkModel(int magic, int version) {
/* 457 */     if (magic != FASTTEXT_FILEFORMAT_MAGIC_INT) {
/* 458 */       throw new IllegalArgumentException("Unhandled file format");
/*     */     }
/*     */     
/* 461 */     if (version > FASTTEXT_VERSION) {
/* 462 */       throw new IllegalArgumentException(
/* 463 */           "Input model version (" + version + ") doesn't match current version (" + FASTTEXT_VERSION + ")");
/*     */     }
/*     */   }
/*     */ 
/*     */   
/*     */   public void loadVecFile() throws IOException {
/* 469 */     loadVecFile(this.args_.pretrainedVectors);
/*     */   }
/*     */   
/*     */   public void loadVecFile(String path) throws IOException {
/* 473 */     Exception exception1 = null, exception2 = null;
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
/*     */   public void saveVecFile() throws IOException {
/* 526 */     saveVecFile(String.valueOf(this.args_.output) + ".vec", true);
/*     */   }
/*     */   
/*     */   public void saveVecFile(String path, boolean in) throws IOException {
/* 530 */     File file = new File(path);
/*     */     
/* 532 */     if (file.exists()) file.delete(); 
/* 533 */     if (file.getParentFile() != null) file.getParentFile().mkdirs();
/*     */     
/* 535 */     if (this.args_.verbose > 1) {
/* 536 */       System.out.println("Saving Vectors to " + file.getCanonicalPath().toString());
/*     */     }
/*     */     
/* 539 */     Exception exception1 = null, exception2 = null;
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
/* 561 */   Thread.UncaughtExceptionHandler trainThreadExcpetionHandler = new Thread.UncaughtExceptionHandler() {
/*     */       public void uncaughtException(Thread th, Throwable ex) {
/* 563 */         ex.printStackTrace();
/*     */       }
/*     */     };
/*     */   
/*     */   private Matrix precomputeWordVectors(boolean in) {
/* 568 */     Matrix wordVectors = new Matrix(this.dict_.nwords(), this.args_.dim);
/* 569 */     wordVectors.zero();
/*     */     
/* 571 */     for (int i = 0; i < this.dict_.nwords(); i++) {
/* 572 */       String word = this.dict_.getWord(i);
/*     */       
/*     */       try {
/* 575 */         Vector vec = in ? getWordVectorIn(word) : getWordVectorOut(word);
/*     */         
/* 577 */         float norm = vec.norm();
/* 578 */         if (norm > 0.0F) {
/* 579 */           wordVectors.addRow(vec, i, 1.0F / norm);
/*     */         }
/*     */       }
/* 582 */       catch (Exception e) {
/* 583 */         logger.error("Failed precomputing word vectors for " + word + " in in:" + in);
/*     */       } 
/*     */     } 
/*     */     
/* 587 */     return wordVectors;
/*     */   }
/*     */   
/*     */   public static class HeapComparator<T>
/*     */     implements Comparator<Pair<Float, T>>
/*     */   {
/*     */     public int compare(Pair<Float, T> p1, Pair<Float, T> p2) {
/* 594 */       if (((Float)p1.getKey()).equals(p2.getKey()))
/* 595 */         return 0; 
/* 596 */       if (((Float)p1.getKey()).floatValue() < ((Float)p2.getKey()).floatValue()) {
/* 597 */         return 1;
/*     */       }
/*     */       
/* 600 */       return -1;
/*     */     }
/*     */   }
/*     */   
/*     */   public class TrainThread extends Thread {
/*     */     final FastText ft;
/*     */     int threadId;
/*     */     
/*     */     public TrainThread(FastText ft, int threadId) {
/* 609 */       super("FT-TrainThread-" + threadId);
/* 610 */       this.ft = ft;
/* 611 */       this.threadId = threadId;
/*     */     }
/*     */     
/*     */     public void run() {
/* 615 */       if (FastText.this.args_.verbose > 2) {
/* 616 */         System.out.println("thread: " + this.threadId + " RUNNING!");
/*     */       }
/*     */       try {
/* 619 */         Exception exception2, exception1 = null;
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
/*     */ 
/*     */ 
/*     */       
/*     */       }
/* 680 */       catch (Exception e) {
/* 681 */         FastText.logger.error(e);
/*     */       } 
/*     */       
/* 684 */       synchronized (this.ft) {
/* 685 */         if (FastText.this.args_.verbose > 2) {
/* 686 */           System.out.println("\nthread: " + this.threadId + " EXIT!");
/*     */         }
/*     */         
/* 689 */         this.ft.threadCount--;
/* 690 */         this.ft.notify();
/*     */       } 
/*     */     }
/*     */     
/*     */     private void printInfo(float progress, float loss) throws Exception {
/* 695 */       float t = (float)(System.currentTimeMillis() - FastText.this.start_) / 1000.0F;
/* 696 */       float ws = (float)FastText.this.tokenCount_.get() / t;
/* 697 */       float wst = (float)FastText.this.tokenCount_.get() / t / FastText.this.args_.thread;
/* 698 */       float lr = (float)(FastText.this.args_.lr * (1.0F - progress));
/* 699 */       int eta = (int)(t / progress * (1.0F - progress));
/* 700 */       int etah = eta / 3600;
/* 701 */       int etam = (eta - etah * 3600) / 60;
/*     */       
/* 703 */       System.out.printf("\rProgress: %.1f%% words/sec: %d words/sec/thread: %d lr: %.6f loss: %.6f eta: %d h %d m", new Object[] {
/* 704 */             Float.valueOf(100.0F * progress), Integer.valueOf((int)ws), Integer.valueOf((int)wst), Float.valueOf(lr), Float.valueOf(loss), Integer.valueOf(etah), Integer.valueOf(etam) });
/* 705 */       System.out.println("ss");
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public class FastTextSynonym
/*     */   {
/*     */     private final String word;
/*     */     
/*     */     private final double cosineSimilarity;
/*     */ 
/*     */     
/*     */     public FastTextSynonym(String word, double cosineSimilarity) {
/* 719 */       this.word = word;
/* 720 */       this.cosineSimilarity = cosineSimilarity;
/*     */     }
/*     */     
/*     */     public String word() {
/* 724 */       return this.word;
/*     */     }
/*     */     
/*     */     public double cosineSimilarity() {
/* 728 */       return this.cosineSimilarity;
/*     */     }
/*     */   }
/*     */ 
/*     */   
/*     */   public class FastTextPrediction
/*     */   {
/*     */     private final String label;
/*     */     private final double logProbability;
/*     */     
/*     */     public FastTextPrediction(String label, double logProbability) {
/* 739 */       this.label = label;
/* 740 */       this.logProbability = logProbability;
/*     */     }
/*     */     
/*     */     public String label() {
/* 744 */       return this.label;
/*     */     }
/*     */     
/*     */     public double logProbability() {
/* 748 */       return this.logProbability;
/*     */     }
/*     */     
/*     */     public double probability() {
/* 752 */       return Math.exp(this.logProbability);
/*     */     }
/*     */   }
/*     */ }


/* Location:              /Users/davidgortega/Desktop/FastText4J.jar!/ai/searchbox/FastText4J/FastText.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */