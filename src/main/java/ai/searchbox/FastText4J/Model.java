/*     */ package ai.searchbox.FastText4J;
/*     */ 
/*     */ import ai.searchbox.FastText4J.math.Matrix;
/*     */ import ai.searchbox.FastText4J.math.Vector;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collections;
/*     */ import java.util.Comparator;
/*     */ import java.util.List;
/*     */ import java.util.Random;
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
/*     */ public class Model
/*     */ {
/*     */   static final int SIGMOID_TABLE_SIZE = 512;
/*     */   static final int MAX_SIGMOID = 8;
/*     */   static final int LOG_TABLE_SIZE = 512;
/*     */   static final int NEGATIVE_TABLE_SIZE = 10000000;
/*     */   private Args args_;
/*     */   private Matrix wi_;
/*     */   private Matrix wo_;
/*     */   private Vector hidden_;
/*     */   private Vector output_;
/*     */   private Vector grad_;
/*     */   private int hsz_;
/*     */   private int isz_;
/*     */   private int osz_;
/*     */   private float loss_;
/*     */   private long nexamples_;
/*     */   private float[] t_sigmoid;
/*     */   private float[] t_log;
/*     */   private List<Integer> negatives;
/*     */   private int negpos;
/*     */   private List<List<Integer>> paths;
/*     */   private List<List<Boolean>> codes;
/*     */   private List<Node> tree;
/*     */   public transient Random rng;
/*     */   private Comparator<Pair<Float, Integer>> comparePairs;
/*     */   
/*     */   public Model(Matrix wi, Matrix wo, Args args, int seed) {
/* 364 */     this.comparePairs = new Comparator<Pair<Float, Integer>>()
/*     */       {
/*     */         public int compare(Pair<Float, Integer> o1, Pair<Float, Integer> o2) {
/* 367 */           return ((Float)o2.getKey()).compareTo(o1.getKey());
/*     */         }
/*     */       };
/*     */     this.negpos = 0;
/*     */     this.loss_ = 0.0F;
/*     */     this.nexamples_ = 1L;
/*     */     this.wi_ = wi;
/*     */     this.wo_ = wo;
/*     */     this.args_ = args;
/*     */     this.isz_ = wi.m;
/*     */     this.osz_ = wo.m;
/*     */     this.hsz_ = args.dim;
/*     */     this.hidden_ = new Vector(args.dim);
/*     */     this.output_ = new Vector(wo.m);
/*     */     this.grad_ = new Vector(args.dim);
/*     */     this.rng = new Random(seed);
/*     */     initSigmoid();
/*     */     initLog();
/*     */   }
/*     */   
/*     */   public float binaryLogistic(int target, boolean label, float lr) {
/*     */     float score = sigmoid(this.wo_.dotRow(this.hidden_, target));
/*     */     float alpha = lr * ((label ? 1.0F : 0.0F) - score);
/*     */     this.grad_.addRow(this.wo_, target, alpha);
/*     */     this.wo_.addRow(this.hidden_, target, alpha);
/*     */     if (label)
/*     */       return -log(score); 
/*     */     return -log(1.0F - score);
/*     */   }
/*     */   
/*     */   public float negativeSampling(int target, float lr) {
/*     */     float loss = 0.0F;
/*     */     this.grad_.zero();
/*     */     for (int n = 0; n <= this.args_.neg; n++) {
/*     */       if (n == 0) {
/*     */         loss += binaryLogistic(target, true, lr);
/*     */       } else {
/*     */         loss += binaryLogistic(getNegative(target), false, lr);
/*     */       } 
/*     */     } 
/*     */     return loss;
/*     */   }
/*     */   
/*     */   public float hierarchicalSoftmax(int target, float lr) {
/*     */     float loss = 0.0F;
/*     */     this.grad_.zero();
/*     */     List<Boolean> binaryCode = this.codes.get(target);
/*     */     List<Integer> pathToRoot = this.paths.get(target);
/*     */     for (int i = 0; i < pathToRoot.size(); i++)
/*     */       loss += binaryLogistic(((Integer)pathToRoot.get(i)).intValue(), ((Boolean)binaryCode.get(i)).booleanValue(), lr); 
/*     */     return loss;
/*     */   }
/*     */   
/*     */   public float softmax(int target, float lr) {
/*     */     this.grad_.zero();
/*     */     computeOutputSoftmax();
/*     */     for (int i = 0; i < this.osz_; i++) {
/*     */       float label = (i == target) ? 1.0F : 0.0F;
/*     */       float alpha = lr * (label - this.output_.get(i));
/*     */       this.grad_.addRow(this.wo_, i, alpha);
/*     */       this.wo_.addRow(this.hidden_, i, alpha);
/*     */     } 
/*     */     return -log(this.output_.get(target));
/*     */   }
/*     */   
/*     */   public void computeOutputSoftmax() {
/*     */     computeOutputSoftmax(this.hidden_, this.output_);
/*     */   }
/*     */   
/*     */   public void computeOutputSoftmax(Vector hidden, Vector output) {
/*     */     output.mul(this.wo_, hidden);
/*     */     float max = output.get(0), z = 0.0F;
/*     */     int i;
/*     */     for (i = 1; i < this.osz_; i++)
/*     */       max = Math.max(output.get(i), max); 
/*     */     for (i = 0; i < this.osz_; i++) {
/*     */       output.set(i, (float)Math.exp((output.get(i) - max)));
/*     */       z += output.get(i);
/*     */     } 
/*     */     for (i = 0; i < this.osz_; i++)
/*     */       output.set(i, output.get(i) / z); 
/*     */   }
/*     */   
/*     */   public void computeHidden(List<Integer> input, Vector hidden) {
/*     */     Utils.checkArgument((hidden.size() == this.hsz_));
/*     */     hidden.zero();
/*     */     for (Integer it : input)
/*     */       hidden.addRow(this.wi_, it.intValue()); 
/*     */     hidden.mul(1.0F / input.size());
/*     */   }
/*     */   
/*     */   public void predict(List<Integer> input, int k, List<Pair<Float, Integer>> heap, Vector hidden, Vector output) {
/*     */     Utils.checkArgument((k > 0));
/*     */     if (heap instanceof ArrayList)
/*     */       ((ArrayList)heap).ensureCapacity(k + 1); 
/*     */     computeHidden(input, hidden);
/*     */     if (this.args_.loss == Args.LossType.hs) {
/*     */       dfs(k, 2 * this.osz_ - 2, 0.0F, heap, hidden);
/*     */     } else {
/*     */       findKBest(k, heap, hidden, output);
/*     */     } 
/*     */     Collections.sort(heap, this.comparePairs);
/*     */   }
/*     */   
/*     */   public void predict(List<Integer> input, int k, List<Pair<Float, Integer>> heap) {
/*     */     predict(input, k, heap, this.hidden_, this.output_);
/*     */   }
/*     */   
/*     */   public void findKBest(int k, List<Pair<Float, Integer>> heap, Vector hidden, Vector output) {
/*     */     computeOutputSoftmax(hidden, output);
/*     */     for (int i = 0; i < this.osz_; i++) {
/*     */       if (heap.size() != k || log(output.get(i)) >= ((Float)((Pair)heap.get(heap.size() - 1)).getKey()).floatValue()) {
/*     */         heap.add(new Pair<>(Float.valueOf(log(output.get(i))), Integer.valueOf(i)));
/*     */         Collections.sort(heap, this.comparePairs);
/*     */         if (heap.size() > k) {
/*     */           Collections.sort(heap, this.comparePairs);
/*     */           heap.remove(heap.size() - 1);
/*     */         } 
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   public void dfs(int k, int node, float score, List<Pair<Float, Integer>> heap, Vector hidden) {
/*     */     if (heap.size() == k && score < ((Float)((Pair)heap.get(heap.size() - 1)).getKey()).floatValue())
/*     */       return; 
/*     */     if (((Node)this.tree.get(node)).left == -1 && ((Node)this.tree.get(node)).right == -1) {
/*     */       heap.add(new Pair<>(Float.valueOf(score), Integer.valueOf(node)));
/*     */       Collections.sort(heap, this.comparePairs);
/*     */       if (heap.size() > k) {
/*     */         Collections.sort(heap, this.comparePairs);
/*     */         heap.remove(heap.size() - 1);
/*     */       } 
/*     */       return;
/*     */     } 
/*     */     float f = sigmoid(this.wo_.dotRow(hidden, node - this.osz_));
/*     */     dfs(k, ((Node)this.tree.get(node)).left, score + log(1.0F - f), heap, hidden);
/*     */     dfs(k, ((Node)this.tree.get(node)).right, score + log(f), heap, hidden);
/*     */   }
/*     */   
/*     */   public void update(List<Integer> input, int target, float lr) {
/*     */     Utils.checkArgument((target >= 0));
/*     */     Utils.checkArgument((target < this.osz_));
/*     */     if (input.size() == 0)
/*     */       return; 
/*     */     computeHidden(input, this.hidden_);
/*     */     if (this.args_.loss == Args.LossType.ns) {
/*     */       this.loss_ += negativeSampling(target, lr);
/*     */     } else if (this.args_.loss == Args.LossType.hs) {
/*     */       this.loss_ += hierarchicalSoftmax(target, lr);
/*     */     } else {
/*     */       this.loss_ += softmax(target, lr);
/*     */     } 
/*     */     this.nexamples_++;
/*     */     if (this.args_.model == Args.ModelType.sup)
/*     */       this.grad_.mul(1.0F / input.size()); 
/*     */     for (Integer it : input)
/*     */       this.wi_.addRow(this.grad_, it.intValue(), 1.0F); 
/*     */   }
/*     */   
/*     */   public void setTargetCounts(List<Long> counts) {
/*     */     Utils.checkArgument((counts.size() == this.osz_));
/*     */     if (this.args_.loss == Args.LossType.ns)
/*     */       initTableNegatives(counts); 
/*     */     if (this.args_.loss == Args.LossType.hs)
/*     */       buildTree(counts); 
/*     */   }
/*     */   
/*     */   public void initTableNegatives(List<Long> counts) {
/*     */     this.negatives = new ArrayList<>(counts.size());
/*     */     float z = 0.0F;
/*     */     int i;
/*     */     for (i = 0; i < counts.size(); i++)
/*     */       z += (float)Math.pow(((Long)counts.get(i)).longValue(), 0.5D); 
/*     */     for (i = 0; i < counts.size(); i++) {
/*     */       float c = (float)Math.pow(((Long)counts.get(i)).longValue(), 0.5D);
/*     */       for (int j = 0; j < c * 1.0E7F / z; j++)
/*     */         this.negatives.add(Integer.valueOf(i)); 
/*     */     } 
/*     */     Utils.shuffle(this.negatives, this.rng);
/*     */   }
/*     */   
/*     */   public int getNegative(int target) {
/*     */     while (true) {
/*     */       int negative = ((Integer)this.negatives.get(this.negpos)).intValue();
/*     */       this.negpos = (this.negpos + 1) % this.negatives.size();
/*     */       if (target != negative)
/*     */         return negative; 
/*     */     } 
/*     */   }
/*     */   
/*     */   public void buildTree(List<Long> counts) {
/*     */     this.paths = new ArrayList<>(this.osz_);
/*     */     this.codes = new ArrayList<>(this.osz_);
/*     */     this.tree = new ArrayList<>(2 * this.osz_ - 1);
/*     */     int i;
/*     */     for (i = 0; i < 2 * this.osz_ - 1; i++) {
/*     */       Node node1 = new Node();
/*     */       node1.parent = -1;
/*     */       node1.left = -1;
/*     */       node1.right = -1;
/*     */       node1.count = 1000000000000000L;
/*     */       node1.binary = false;
/*     */       this.tree.add(i, node1);
/*     */     } 
/*     */     for (i = 0; i < this.osz_; i++)
/*     */       ((Node)this.tree.get(i)).count = ((Long)counts.get(i)).longValue(); 
/*     */     int leaf = this.osz_ - 1;
/*     */     int node = this.osz_;
/*     */     int j;
/*     */     for (j = this.osz_; j < 2 * this.osz_ - 1; j++) {
/*     */       int[] mini = new int[2];
/*     */       for (int k = 0; k < 2; k++) {
/*     */         if (leaf >= 0 && ((Node)this.tree.get(leaf)).count < ((Node)this.tree.get(node)).count) {
/*     */           mini[k] = leaf--;
/*     */         } else {
/*     */           mini[k] = node++;
/*     */         } 
/*     */       } 
/*     */       ((Node)this.tree.get(j)).left = mini[0];
/*     */       ((Node)this.tree.get(j)).right = mini[1];
/*     */       ((Node)this.tree.get(mini[0])).count += ((Node)this.tree.get(mini[1])).count;
/*     */       ((Node)this.tree.get(mini[0])).parent = j;
/*     */       ((Node)this.tree.get(mini[1])).parent = j;
/*     */       ((Node)this.tree.get(mini[1])).binary = true;
/*     */     } 
/*     */     for (j = 0; j < this.osz_; j++) {
/*     */       List<Integer> path = new ArrayList<>();
/*     */       List<Boolean> code = new ArrayList<>();
/*     */       int k = j;
/*     */       while (((Node)this.tree.get(k)).parent != -1) {
/*     */         path.add(Integer.valueOf(((Node)this.tree.get(k)).parent - this.osz_));
/*     */         code.add(Boolean.valueOf(((Node)this.tree.get(k)).binary));
/*     */         k = ((Node)this.tree.get(k)).parent;
/*     */       } 
/*     */       this.paths.add(path);
/*     */       this.codes.add(code);
/*     */     } 
/*     */   }
/*     */   
/*     */   public float getLoss() {
/*     */     return this.loss_ / (float)this.nexamples_;
/*     */   }
/*     */   
/*     */   public float log(float x) {
/*     */     if (x > 1.0F)
/*     */       return 0.0F; 
/*     */     int i = (int)(x * 512.0F);
/*     */     return this.t_log[i];
/*     */   }
/*     */   
/*     */   public float sigmoid(float x) {
/*     */     if (x < -8.0F)
/*     */       return 0.0F; 
/*     */     if (x > 8.0F)
/*     */       return 1.0F; 
/*     */     int i = (int)((x + 8.0F) * 512.0F / 8.0F / 2.0F);
/*     */     return this.t_sigmoid[i];
/*     */   }
/*     */   
/*     */   private void initSigmoid() {
/*     */     this.t_sigmoid = new float[513];
/*     */     for (int i = 0; i < 513; i++) {
/*     */       float x = (i * 2 * 8) / 512.0F - 8.0F;
/*     */       this.t_sigmoid[i] = (float)(1.0D / (1.0D + Math.exp(-x)));
/*     */     } 
/*     */   }
/*     */   
/*     */   private void initLog() {
/*     */     this.t_log = new float[513];
/*     */     for (int i = 0; i < 513; i++) {
/*     */       float x = (i + 1.0E-5F) / 512.0F;
/*     */       this.t_log[i] = (float)Math.log(x);
/*     */     } 
/*     */   }
/*     */   
/*     */   public class Node {
/*     */     int parent;
/*     */     int left;
/*     */     int right;
/*     */     long count;
/*     */     boolean binary;
/*     */   }
/*     */ }


/* Location:              /Users/davidgortega/Desktop/FastText4J.jar!/ai/searchbox/FastText4J/Model.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */