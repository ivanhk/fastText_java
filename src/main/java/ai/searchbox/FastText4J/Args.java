/*     */ package ai.searchbox.FastText4J;
/*     */ 
/*     */ import ai.searchbox.FastText4J.io.IOUtil;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.OutputStream;
/*     */ 
/*     */ 
/*     */ public class Args
/*     */ {
/*     */   public String input;
/*     */   public String output;
/*     */   public String test;
/*  14 */   public double lr = 0.05D;
/*  15 */   public int lrUpdateRate = 100;
/*  16 */   public int dim = 100;
/*  17 */   public int ws = 5;
/*  18 */   public int epoch = 5;
/*  19 */   public int minCount = 5;
/*  20 */   public int minCountLabel = 0;
/*  21 */   public int neg = 5;
/*  22 */   public int wordNgrams = 1;
/*  23 */   public LossType loss = LossType.ns;
/*  24 */   public ModelType model = ModelType.sg;
/*  25 */   public int bucket = 2000000;
/*  26 */   public int minn = 3;
/*  27 */   public int maxn = 6;
/*  28 */   public int thread = 1;
/*  29 */   public double t = 1.0E-4D;
/*  30 */   public String label = "__label__";
/*  31 */   public int verbose = 2;
/*  32 */   public String pretrainedVectors = "";
/*     */   
/*     */   public void printHelp() {
/*  35 */     System.out.println("\nThe following arguments are mandatory:\n  -input              training file path\n  -output             output file path\n\nThe following arguments are optional:\n  -lr                 learning rate [" + 
/*     */ 
/*     */ 
/*     */         
/*  39 */         this.lr + "]\n" + 
/*  40 */         "  -lrUpdateRate       change the rate of updates for the learning rate [" + this.lrUpdateRate + "]\n" + 
/*  41 */         "  -dim                size of word vectors [" + this.dim + "]\n" + 
/*  42 */         "  -ws                 size of the context window [" + this.ws + "]\n" + 
/*  43 */         "  -epoch              number of epochs [" + this.epoch + "]\n" + 
/*  44 */         "  -minCount           minimal number of word occurences [" + this.minCount + "]\n" + 
/*  45 */         "  -minCountLabel      minimal number of label occurences [" + this.minCountLabel + "]\n" + 
/*  46 */         "  -neg                number of negatives sampled [" + this.neg + "]\n" + 
/*  47 */         "  -wordNgrams         max length of word ngram [" + this.wordNgrams + "]\n" + 
/*  48 */         "  -loss               loss function {ns, hs, softmax} [ns]\n" + 
/*  49 */         "  -bucket             number of buckets [" + this.bucket + "]\n" + 
/*  50 */         "  -minn               min length of char ngram [" + this.minn + "]\n" + 
/*  51 */         "  -maxn               max length of char ngram [" + this.maxn + "]\n" + 
/*  52 */         "  -thread             number of threads [" + this.thread + "]\n" + 
/*  53 */         "  -t                  sampling threshold [" + this.t + "]\n" + 
/*  54 */         "  -label              labels prefix [" + this.label + "]\n" + 
/*  55 */         "  -verbose            verbosity level [" + this.verbose + "]\n" + 
/*  56 */         "  -pretrainedVectors  pretrained word vectors for supervised learning []");
/*     */   }
/*     */   
/*     */   public void save(OutputStream ofs) throws IOException {
/*  60 */     IOUtil ioutil = new IOUtil();
/*  61 */     ofs.write(ioutil.intToByteArray(this.dim));
/*  62 */     ofs.write(ioutil.intToByteArray(this.ws));
/*  63 */     ofs.write(ioutil.intToByteArray(this.epoch));
/*  64 */     ofs.write(ioutil.intToByteArray(this.minCount));
/*  65 */     ofs.write(ioutil.intToByteArray(this.neg));
/*  66 */     ofs.write(ioutil.intToByteArray(this.wordNgrams));
/*  67 */     ofs.write(ioutil.intToByteArray(this.loss.value));
/*  68 */     ofs.write(ioutil.intToByteArray(this.model.value));
/*  69 */     ofs.write(ioutil.intToByteArray(this.bucket));
/*  70 */     ofs.write(ioutil.intToByteArray(this.minn));
/*  71 */     ofs.write(ioutil.intToByteArray(this.maxn));
/*  72 */     ofs.write(ioutil.intToByteArray(this.lrUpdateRate));
/*  73 */     ofs.write(ioutil.doubleToByteArray(this.t));
/*     */   }
/*     */   
/*     */   public void load(InputStream input) throws IOException {
/*  77 */     IOUtil ioutil = new IOUtil();
/*  78 */     this.dim = ioutil.readInt(input);
/*  79 */     this.ws = ioutil.readInt(input);
/*  80 */     this.epoch = ioutil.readInt(input);
/*  81 */     this.minCount = ioutil.readInt(input);
/*  82 */     this.neg = ioutil.readInt(input);
/*  83 */     this.wordNgrams = ioutil.readInt(input);
/*  84 */     this.loss = LossType.fromValue(ioutil.readInt(input));
/*  85 */     this.model = ModelType.fromValue(ioutil.readInt(input));
/*  86 */     this.bucket = ioutil.readInt(input);
/*  87 */     this.minn = ioutil.readInt(input);
/*  88 */     this.maxn = ioutil.readInt(input);
/*  89 */     this.lrUpdateRate = ioutil.readInt(input);
/*  90 */     this.t = ioutil.readDouble(input);
/*     */   }
/*     */   
/*     */   public void parseArgs(String[] args) {
/*  94 */     String command = args[0];
/*     */     
/*  96 */     if ("supervised".equalsIgnoreCase(command)) {
/*  97 */       this.model = ModelType.sup;
/*  98 */       this.loss = LossType.softmax;
/*  99 */       this.minCount = 1;
/* 100 */       this.minn = 0;
/* 101 */       this.maxn = 0;
/* 102 */       this.lr = 0.1D;
/*     */     } 
/*     */     
/* 105 */     if ("cbow".equalsIgnoreCase(command)) {
/* 106 */       this.model = ModelType.cbow;
/*     */     }
/*     */     
/* 109 */     if ("skipgram".equalsIgnoreCase(command)) {
/* 110 */       this.model = ModelType.sg;
/*     */     }
/*     */     
/* 113 */     int ai = 1;
/* 114 */     while (ai < args.length) {
/* 115 */       if (args[ai].charAt(0) != '-') {
/* 116 */         System.out.println("Provided argument without a dash! Usage:");
/* 117 */         printHelp();
/* 118 */         System.exit(1);
/*     */       } 
/*     */       
/* 121 */       if ("-h".equals(args[ai])) {
/* 122 */         System.out.println("Here is the help! Usage:");
/* 123 */         printHelp();
/* 124 */         System.exit(1);
/* 125 */       } else if ("-input".equals(args[ai])) {
/* 126 */         this.input = args[ai + 1];
/* 127 */       } else if ("-test".equals(args[ai])) {
/* 128 */         this.test = args[ai + 1];
/* 129 */       } else if ("-output".equals(args[ai])) {
/* 130 */         this.output = args[ai + 1];
/* 131 */       } else if ("-lr".equals(args[ai])) {
/* 132 */         this.lr = Double.parseDouble(args[ai + 1]);
/* 133 */       } else if ("-lrUpdateRate".equals(args[ai])) {
/* 134 */         this.lrUpdateRate = Integer.parseInt(args[ai + 1]);
/* 135 */       } else if ("-dim".equals(args[ai])) {
/* 136 */         this.dim = Integer.parseInt(args[ai + 1]);
/* 137 */       } else if ("-ws".equals(args[ai])) {
/* 138 */         this.ws = Integer.parseInt(args[ai + 1]);
/* 139 */       } else if ("-epoch".equals(args[ai])) {
/* 140 */         this.epoch = Integer.parseInt(args[ai + 1]);
/* 141 */       } else if ("-minCount".equals(args[ai])) {
/* 142 */         this.minCount = Integer.parseInt(args[ai + 1]);
/* 143 */       } else if ("-minCountLabel".equals(args[ai])) {
/* 144 */         this.minCountLabel = Integer.parseInt(args[ai + 1]);
/* 145 */       } else if ("-neg".equals(args[ai])) {
/* 146 */         this.neg = Integer.parseInt(args[ai + 1]);
/* 147 */       } else if ("-wordNgrams".equals(args[ai])) {
/* 148 */         this.wordNgrams = Integer.parseInt(args[ai + 1]);
/* 149 */       } else if ("-loss".equals(args[ai])) {
/* 150 */         if ("hs".equalsIgnoreCase(args[ai + 1])) {
/* 151 */           this.loss = LossType.hs;
/* 152 */         } else if ("ns".equalsIgnoreCase(args[ai + 1])) {
/* 153 */           this.loss = LossType.ns;
/* 154 */         } else if ("softmax".equalsIgnoreCase(args[ai + 1])) {
/* 155 */           this.loss = LossType.softmax;
/*     */         } else {
/* 157 */           System.out.println("Unknown loss: " + args[ai + 1]);
/* 158 */           printHelp();
/* 159 */           System.exit(1);
/*     */         } 
/* 161 */       } else if ("-bucket".equals(args[ai])) {
/* 162 */         this.bucket = Integer.parseInt(args[ai + 1]);
/* 163 */       } else if ("-minn".equals(args[ai])) {
/* 164 */         this.minn = Integer.parseInt(args[ai + 1]);
/* 165 */       } else if ("-maxn".equals(args[ai])) {
/* 166 */         this.maxn = Integer.parseInt(args[ai + 1]);
/* 167 */       } else if ("-thread".equals(args[ai])) {
/* 168 */         this.thread = Integer.parseInt(args[ai + 1]);
/* 169 */       } else if ("-t".equals(args[ai])) {
/* 170 */         this.t = Double.parseDouble(args[ai + 1]);
/* 171 */       } else if ("-label".equals(args[ai])) {
/* 172 */         this.label = args[ai + 1];
/* 173 */       } else if ("-verbose".equals(args[ai])) {
/* 174 */         this.verbose = Integer.parseInt(args[ai + 1]);
/* 175 */       } else if ("-pretrainedVectors".equals(args[ai])) {
/* 176 */         this.pretrainedVectors = args[ai + 1];
/*     */       } else {
/* 178 */         System.out.println("Unknown argument: " + args[ai]);
/* 179 */         printHelp();
/* 180 */         System.exit(1);
/*     */       } 
/* 182 */       ai += 2;
/*     */     } 
/* 184 */     if (Utils.isEmpty(this.input) || Utils.isEmpty(this.output)) {
/* 185 */       System.out.println("Empty input or output path.");
/* 186 */       printHelp();
/* 187 */       System.exit(1);
/*     */     } 
/* 189 */     if (this.wordNgrams <= 1 && this.maxn == 0) {
/* 190 */       this.bucket = 0;
/*     */     }
/*     */   }
/*     */ 
/*     */   
/*     */   public String toString() {
/* 196 */     StringBuilder builder = new StringBuilder();
/* 197 */     builder.append("Args [input=");
/* 198 */     builder.append(this.input);
/* 199 */     builder.append(", output=");
/* 200 */     builder.append(this.output);
/* 201 */     builder.append(", test=");
/* 202 */     builder.append(this.test);
/* 203 */     builder.append(", lr=");
/* 204 */     builder.append(this.lr);
/* 205 */     builder.append(", lrUpdateRate=");
/* 206 */     builder.append(this.lrUpdateRate);
/* 207 */     builder.append(", dim=");
/* 208 */     builder.append(this.dim);
/* 209 */     builder.append(", ws=");
/* 210 */     builder.append(this.ws);
/* 211 */     builder.append(", epoch=");
/* 212 */     builder.append(this.epoch);
/* 213 */     builder.append(", minCount=");
/* 214 */     builder.append(this.minCount);
/* 215 */     builder.append(", minCountLabel=");
/* 216 */     builder.append(this.minCountLabel);
/* 217 */     builder.append(", neg=");
/* 218 */     builder.append(this.neg);
/* 219 */     builder.append(", wordNgrams=");
/* 220 */     builder.append(this.wordNgrams);
/* 221 */     builder.append(", loss=");
/* 222 */     builder.append(this.loss);
/* 223 */     builder.append(", model=");
/* 224 */     builder.append(this.model);
/* 225 */     builder.append(", bucket=");
/* 226 */     builder.append(this.bucket);
/* 227 */     builder.append(", minn=");
/* 228 */     builder.append(this.minn);
/* 229 */     builder.append(", maxn=");
/* 230 */     builder.append(this.maxn);
/* 231 */     builder.append(", thread=");
/* 232 */     builder.append(this.thread);
/* 233 */     builder.append(", t=");
/* 234 */     builder.append(this.t);
/* 235 */     builder.append(", label=");
/* 236 */     builder.append(this.label);
/* 237 */     builder.append(", verbose=");
/* 238 */     builder.append(this.verbose);
/* 239 */     builder.append(", pretrainedVectors=");
/* 240 */     builder.append(this.pretrainedVectors);
/* 241 */     builder.append("]");
/* 242 */     return builder.toString();
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public enum ModelType
/*     */   {
/* 249 */     cbow(1), sg(2), sup(3);
/*     */     
/*     */     private int value;
/*     */     
/*     */     public int getValue() {
/* 254 */       return this.value;
/*     */     }
/*     */     
/*     */     public static ModelType fromValue(int value) throws IllegalArgumentException {
/*     */       try {
/* 259 */         value--;
/* 260 */         return values()[value];
/* 261 */       } catch (ArrayIndexOutOfBoundsException e) {
/* 262 */         throw new IllegalArgumentException("Unknown model_name enum value :" + value);
/*     */       } 
/*     */     }
/*     */     
/*     */     ModelType(int value) {
/* 267 */       this.value = value;
/*     */     }
/*     */   }
/*     */   
/*     */   public enum LossType {
/* 272 */     hs(1), ns(2), softmax(3);
/*     */     
/*     */     private int value;
/*     */     
/*     */     public int getValue() {
/* 277 */       return this.value;
/*     */     }
/*     */     
/*     */     public static LossType fromValue(int value) throws IllegalArgumentException {
/*     */       try {
/* 282 */         value--;
/* 283 */         return values()[value];
/* 284 */       } catch (ArrayIndexOutOfBoundsException e) {
/* 285 */         throw new IllegalArgumentException("Unknown loss_name enum value :" + value);
/*     */       } 
/*     */     }
/*     */     
/*     */     LossType(int value) {
/* 290 */       this.value = value;
/*     */     }
/*     */   }
/*     */ }


/* Location:              /Users/davidgortega/Desktop/FastText4J.jar!/ai/searchbox/FastText4J/Args.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */