/*     */ package ai.searchbox.FastText4J;
/*     */ 
/*     */ import java.io.File;
/*     */ import java.io.FileInputStream;
/*     */ import java.io.IOException;
/*     */ 
/*     */ public class Main
/*     */ {
/*     */   public static void printUsage() {
/*  10 */     System.out.print("usage: java -jar fasttext.jar <command> <args>\n\nThe commands supported by fasttext are:\n\n  supervised          train a supervised classifier\n  skipgram            train a skipgram model\n  cbow                train a cbow model\n  predict             predict most likely labels\n  predict-prob        predict most likely labels with probabilities\n  test                evaluate a supervised classifier\n");
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public static void printPredictUsage() {
/*  21 */     System.out.print("usage: java -jar fasttext.jar predict[-prob] <model> <test-data> [<k>]\n\n  <model>      model filename\n  <test-data>  test data filename (if -, read from stdin)\n  <k>          (optional; 1 by default) predict top k labels\n");
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public static void printTestUsage() {
/*  28 */     System.out.print("usage: java -jar fasttext.jar test <model> <test-data> [<k>]\n\n  <model>      model filename\n  <test-data>  test data filename (if -, read from stdin)\n  <k>          (optional; 1 by default) predict top k labels\n");
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
/*     */   private void predict(String[] args) throws Exception {}
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
/*     */   private void train(String[] args) throws Exception {
/*  66 */     Args a = new Args();
/*  67 */     a.parseArgs(args);
/*     */     
/*  69 */     FastText fasttext = new FastText();
/*  70 */     fasttext.setArgs(a);
/*  71 */     fasttext.train();
/*     */   }
/*     */   
/*     */   private void test(String[] args) throws Exception {
/*  75 */     int k = 1;
/*  76 */     if (args.length == 3) {
/*  77 */       k = 1;
/*  78 */     } else if (args.length == 4) {
/*  79 */       k = Integer.parseInt(args[3]);
/*     */     } else {
/*  81 */       printTestUsage();
/*  82 */       System.exit(1);
/*     */     } 
/*     */     
/*  85 */     FastText fasttext = new FastText();
/*  86 */     fasttext.loadModel(args[1]);
/*  87 */     String infile = args[2];
/*  88 */     if ("-".equals(infile)) {
/*  89 */       fasttext.test(System.in, k);
/*     */     } else {
/*  91 */       File file = new File(infile);
/*  92 */       if (!file.exists() || !file.isFile() || !file.canRead()) {
/*  93 */         throw new IOException("Test file cannot be opened!");
/*     */       }
/*  95 */       fasttext.test(new FileInputStream(file), k);
/*     */     } 
/*     */   }
/*     */   
/*     */   public static void main(String[] args) {
/* 100 */     args = new String[] { "skipgram", 
/* 101 */         "-input", "/Users/davidgortega/Projects/tmp/fastText-0.9.1/data/file9short", 
/* 102 */         "-output", "/Users/davidgortega/Projects/tmp/fastText-0.9.1/result/fil9Java", 
/* 103 */         "-thread", "8" };
/*     */     
/* 105 */     Main op = new Main();
/*     */     
/* 107 */     if (args.length == 0) {
/* 108 */       printUsage();
/* 109 */       System.exit(1);
/*     */     } 
/*     */     
/*     */     try {
/* 113 */       String command = args[0];
/* 114 */       if ("predict".equalsIgnoreCase(command) || "predict-prob".equalsIgnoreCase(command)) {
/* 115 */         op.predict(args);
/* 116 */       } else if ("skipgram".equalsIgnoreCase(command) || 
/* 117 */         "cbow".equalsIgnoreCase(command) || 
/* 118 */         "supervised".equalsIgnoreCase(command)) {
/* 119 */         op.train(args);
/* 120 */       } else if ("test".equalsIgnoreCase(command)) {
/* 121 */         op.test(args);
/*     */       } else {
/* 123 */         printUsage();
/* 124 */         System.exit(1);
/*     */       } 
/* 126 */     } catch (Exception e) {
/* 127 */       e.printStackTrace();
/* 128 */       System.exit(1);
/*     */     } 
/*     */     
/* 131 */     System.exit(0);
/*     */   }
/*     */ }


/* Location:              /Users/davidgortega/Desktop/FastText4J.jar!/ai/searchbox/FastText4J/Main.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */