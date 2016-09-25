package fasttext;

import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import fasttext.Args.loss_name;
import fasttext.Args.model_name;
import com.google.common.base.Preconditions;

public class Model {

	static final int NEGATIVE_TABLE_SIZE = 10000000;
	static final float MIN_LR = 0.000001f;

	private static float lr_ = MIN_LR;

	public class Node {
		int parent;
		int left;
		int right;
		long count;
		boolean binary;
	}

	private Args args;

	private Matrix wi_; // input
	private Matrix wo_; // output
	private Vector hidden_;
	private Vector output_;
	private Vector grad_;
	private int hsz_; // dim
	private int isz_; // input vocabSize
	private int osz_; // output vocabSize
	private java.util.Vector<Integer> negatives;
	private int negpos;
	private java.util.Vector<java.util.Vector<Integer>> paths;
	private java.util.Vector<java.util.Vector<Boolean>> codes;
	private java.util.Vector<Node> tree;

	public RandomGenerator rng;

	public Model(Args args, Matrix wi, Matrix wo, int hsz, float lr, int seed) {
		this.args = args;
		wi_ = new Matrix(wi);
		wo_ = new Matrix(wo);
		hidden_ = new Vector(hsz);
		output_ = new Vector(wo.m_);
		grad_ = new Vector(hsz);
		rng = new Well19937c(seed);
		isz_ = wi.m_;
		osz_ = wo.m_;
		hsz_ = hsz;
		lr_ = lr;
		negpos = 0;
	}

	public void setLearningRate(float lr) {
		lr_ = (lr < MIN_LR) ? MIN_LR : lr;
	}

	public float getLearningRate() {
		return lr_;
	}

	public float binaryLogistic(int target, boolean label) {
		float score = Utils.sigmoid(wo_.dotRow(hidden_, target));
		float alpha = lr_ * (label ? 1.0f : 0.0f - score);
		grad_.addRow(wo_, target, alpha);
		wo_.addRow(hidden_, target, alpha);
		if (label) {
			return -Utils.log(score);
		} else {
			return -Utils.log((float) (1.0 - score));
		}
	}

	public float negativeSampling(int target) {
		float loss = 0.0f;
		grad_.zero();
		for (int n = 0; n <= args.neg; n++) {
			if (n == 0) {
				loss += binaryLogistic(target, true);
			} else {
				loss += binaryLogistic(getNegative(target), false);
			}
		}
		return loss;
	}

	public float hierarchicalSoftmax(int target) {
		float loss = 0.0f;
		grad_.zero();
		final java.util.Vector<Boolean> binaryCode = codes.get(target);
		final java.util.Vector<Integer> pathToRoot = paths.get(target);
		for (int i = 0; i < pathToRoot.size(); i++) {
			loss += binaryLogistic(pathToRoot.get(i), binaryCode.get(i));
		}
		return loss;
	}

	public float softmax(int target) {
		grad_.zero();
		output_.mul(wo_, hidden_);
		float max = 0.0f, z = 0.0f;
		for (int i = 0; i < osz_; i++) {
			max = Math.max(output_.get(i), max);
		}
		for (int i = 0; i < osz_; i++) {
			output_.set(i, (float) Math.exp(output_.get(i) - max));
			z += output_.get(i);
		}
		for (int i = 0; i < osz_; i++) {
			float label = (i == target) ? 1.0f : 0.0f;
			output_.set(i, output_.get(i) / z);
			float alpha = lr_ * (label - output_.get(i));
			grad_.addRow(wo_, i, alpha);
			wo_.addRow(hidden_, i, alpha);
		}
		return -Utils.log(output_.get(target));
	}

	public int getNegative(int target) {
		int negative;
		do {
			negative = negatives.get(negpos);
			negpos = (negpos + 1) % negatives.size();
		} while (target == negative);
		return negative;
	}

	public int predict(final java.util.Vector<Integer> input) {
		hidden_.zero();
		for (Integer it : input) {
			hidden_.addRow(wi_, it);
		}
		hidden_.mul((float) (1.0 / input.size()));

		if (args.loss == loss_name.hs) {
			float max = -1e10f;
			int argmax = -1;
			dfs(2 * osz_ - 2, 0.0f, max, argmax);
			return argmax;
		} else {
			output_.mul(wo_, hidden_);
			return output_.argmax();
		}
	}

	/**
	 * predict with probability
	 * @param input
	 * @param score
     * @return
     */
	public int predict(final java.util.Vector<Integer> input, Float score ) {
		hidden_.zero();
		for (Integer it : input) {
			hidden_.addRow(wi_, it);
		}
		hidden_.mul((float) (1.0 / input.size()));

		if (args.loss == loss_name.hs) {
			float max = -1e10f;
			int argmax = -1;
			dfs(2 * osz_ - 2, 0.0f, max, argmax);
			return argmax;
		} else {
			output_.mul(wo_, hidden_);
			int max_idx = 0;
			float max_val = output_.data_[0];
			for(int i = 1; i < osz_; i ++) {
				if(output_.data_[i] > max_val) {
					max_val = output_.data_[i];
					max_idx = i;
				}
			}
			float z = 0;
			for(int i = 0; i < osz_; i ++) {
				output_.data_[i] = (float) Math.exp(output_.data_[i] - max_val);
				z += output_.data_[i];
			}
			for(int i = 0; i < osz_; i ++) {
				output_.data_[i] /= z;
			}
			int idx = output_.argmax();
			score = Float.valueOf(output_.data_[idx]);
			return idx;
			//return output_.argmax();
		}
	}

	public void dfs(int node, float score, float max, int argmax) {
		if (score < max)
			return;
		if (tree.get(node).left == -1 && tree.get(node).right == -1) {
			max = score;
			argmax = node;
			return;
		}
		float f = Utils.sigmoid(wo_.dotRow(hidden_, node - osz_));
		dfs(tree.get(node).left, score + Utils.log(1.0f - f), max, argmax);
		dfs(tree.get(node).right, score + Utils.log(f), max, argmax);
	}

	public void initTableNegatives(final java.util.Vector<Long> counts) {
		negatives = new java.util.Vector<Integer>(counts.size());
		float z = 0.0f;
		for (int i = 0; i < counts.size(); i++) {
			z += (float) Math.pow(counts.get(i), 0.5f);
		}
		for (int i = 0; i < counts.size(); i++) {
			float c = (float) Math.pow(counts.get(i), 0.5f);
			for (int j = 0; j < c * NEGATIVE_TABLE_SIZE / z; j++) {
				negatives.add(i);
			}
		}
		Utils.shuffle(negatives, rng);
	}

	public float update(final java.util.Vector<Integer> input, int target) {
		Preconditions.checkArgument(target >= 0);
		Preconditions.checkArgument(target < osz_);
		if (input.size() == 0)
			return 0.0f;
		hidden_.zero();
		for (Integer it : input) {
			hidden_.addRow(wi_, it);
		}
		hidden_.mul((float) (1.0 / input.size()));

		float loss;
		if (args.loss == loss_name.ns) {
			loss = negativeSampling(target);
		} else if (args.loss == loss_name.hs) {
			loss = hierarchicalSoftmax(target);
		} else {
			loss = softmax(target);
		}

		if (args.model == model_name.sup) {
			grad_.mul((float) (1.0 / input.size()));
		}
		for (Integer it : input) {
			wi_.addRow(grad_, it, 1.0f);
		}
		return loss;
	}

	public void setTargetCounts(final java.util.Vector<Long> counts) {
		Preconditions.checkArgument(counts.size() == osz_);
		if (args.loss == loss_name.ns) {
			initTableNegatives(counts);
		}
		if (args.loss == loss_name.hs) {
			buildTree(counts);
		}
	}

	public void buildTree(final java.util.Vector<Long> counts) {
		paths = new java.util.Vector<java.util.Vector<Integer>>(osz_);
		codes = new java.util.Vector<java.util.Vector<Boolean>>(osz_);
		tree = new java.util.Vector<Node>(2 * osz_ - 1);

		// tree.setSize();
		for (int i = 0; i < 2 * osz_ - 1; i++) {
			Node node = tree.get(i);
			node.parent = -1;
			node.left = -1;
			node.right = -1;
			node.count = 1000000000000000L;// 1e15f;
			node.binary = false;
		}
		for (int i = 0; i < osz_; i++) {
			tree.get(i).count = counts.get(i);
		}
		int leaf = osz_ - 1;
		int node = osz_;
		for (int i = osz_; i < 2 * osz_ - 1; i++) {
			int[] mini = new int[2];
			for (int j = 0; j < 2; j++) {
				if (leaf >= 0 && tree.get(leaf).count < tree.get(node).count) {
					mini[j] = leaf--;
				} else {
					mini[j] = node++;
				}
			}
			tree.get(i).left = mini[0];
			tree.get(i).right = mini[1];
			tree.get(i).count = tree.get(mini[0]).count + tree.get(mini[1]).count;
			tree.get(mini[0]).parent = i;
			tree.get(mini[1]).parent = i;
			tree.get(mini[1]).binary = true;
		}
		for (int i = 0; i < osz_; i++) {
			java.util.Vector<Integer> path = new java.util.Vector<Integer>();
			java.util.Vector<Boolean> code = new java.util.Vector<Boolean>();
			int j = i;
			while (tree.get(j).parent != -1) {
				path.add(tree.get(j).parent - osz_);
				code.add(tree.get(j).binary);
				j = tree.get(j).parent;
			}
			paths.add(path);
			codes.add(code);
		}
	}
}
