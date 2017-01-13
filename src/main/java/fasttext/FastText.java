package fasttext;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import fasttext.Args.model_name;
import fasttext.Dictionary.entry_type;

/**
 * FastText class, can be used as a lib in other projects
 * 
 * @author Ivan
 *
 */
public class FastText {

	private static int SUPERVISED_LABEL_SIZE = 10;

	private Args args_;
	private Dictionary dict_;
	private Matrix input_;
	private Matrix output_;
	private Model model_;

	private AtomicLong tokenCount;
	private long start;

	public void getVector(Vector vec, final String word) {
		final java.util.Vector<Integer> ngrams = dict_.getNgrams(word);
		vec.zero();
		for (Integer it : ngrams) {
			vec.addRow(input_, it);
		}
		if (ngrams.size() > 0) {
			vec.mul(1.0f / ngrams.size());
		}
	}

	public void saveVectors() throws IOException {
		File file = new File(args_.output + ".vec");
		if (file.exists()) {
			file.delete();
		}
		file.getParentFile().mkdirs();
		if (args_.verbose > 1) {
			System.out.println("Saving Vectors to " + file.getCanonicalPath().toString());
		}

		Writer writer = new BufferedWriter(new FileWriter(file));
		try {
			writer.write(dict_.nwords());
			writer.write(" ");
			writer.write(args_.dim);
			writer.write("\n");
			Vector vec = new Vector(args_.dim);
			DecimalFormat df = new DecimalFormat("0.#####");
			for (int i = 0; i < dict_.nwords(); i++) {
				String word = dict_.getWord(i);
				getVector(vec, word);
				writer.write(word);
				for (int j = 0; i < vec.m_; i++) {
					writer.write(" ");
					writer.write(df.format(vec.data_[j]));
				}
				writer.write("\n");
			}
		} finally {
			writer.flush();
			writer.close();
		}
	}

	public void saveModel() throws IOException {
		File file = new File(args_.output + ".bin");
		if (file.exists()) {
			file.delete();
		}
		file.getParentFile().mkdirs();
		if (args_.verbose > 1) {
			System.out.println("Saving model to " + file.getCanonicalPath().toString());
		}
		OutputStream ofs = new BufferedOutputStream(new FileOutputStream(file));
		try {
			args_.save(ofs);
			dict_.save(ofs);
			input_.save(ofs);
			output_.save(ofs);
		} finally {
			ofs.flush();
			ofs.close();
		}
	}

	public void loadModel(String filename) throws IOException {
		DataInputStream dis = null;
		BufferedInputStream bis = null;
		try {
			File file = new File(filename);
			if (!(file.exists() && file.isFile() && file.canRead())) {
				throw new IOException("Model file cannot be opened for loading!");
			}
			bis = new BufferedInputStream(new FileInputStream(file));
			dis = new DataInputStream(bis);

			args_ = new Args();
			dict_ = new Dictionary(args_);
			input_ = new Matrix();
			output_ = new Matrix();

			args_.load(dis);
			dict_.load(dis);
			input_.load(dis);
			output_.load(dis);

			model_ = new Model(input_, output_, args_, 0);
			if (args_.model == model_name.sup) {
				model_.setTargetCounts(dict_.getCounts(entry_type.label));
			} else {
				model_.setTargetCounts(dict_.getCounts(entry_type.word));
			}
		} finally {
			bis.close();
			dis.close();
		}
	}

	public void printInfo(float progress, float loss) {
		float t = (float) (System.currentTimeMillis() - start) / 1000;
		float wst = (float) (tokenCount.get()) / t;
		float lr = (float) (args_.lr * (1.0 - progress));
		int eta = (int) (t / progress * (1 - progress) / args_.thread);
		int etah = eta / 3600;
		int etam = (eta - etah * 3600) / 60;
		System.out.printf("\rProgress: %.1f%% words/sec/thread: %d lr: %.6f loss: %.6f eta: %d h %d m", 100 * progress,
				(int) wst, lr, loss, etah, etam);
	}

	public void supervised(Model model, float lr, final java.util.Vector<Integer> line,
			final java.util.Vector<Integer> labels, UniformIntegerDistribution uid) {
		if (labels.size() == 0 || line.size() == 0)
			return;
		// std::uniform_int_distribution<> uniform(0, labels.size() - 1);
		int i = uid.sample();
		model.update(line, labels.get(i), lr);
	}

	public void cbow(Model model, float lr, final java.util.Vector<Integer> line, UniformIntegerDistribution uid) {
		java.util.Vector<Integer> bow = new java.util.Vector<Integer>();
		for (int w = 0; w < line.size(); w++) {
			// std::uniform_int_distribution<> uniform(1, args_->ws);
			int boundary = uid.sample();
			bow.clear();
			for (int c = -boundary; c <= boundary; c++) {
				if (c != 0 && w + c >= 0 && w + c < line.size()) {
					final java.util.Vector<Integer> ngrams = dict_.getNgrams(line.get(w + c));
					bow.addAll(ngrams);
				}
			}
			model.update(bow, line.get(w), lr);
		}
	}

	public void skipgram(Model model, float lr, final java.util.Vector<Integer> line, UniformIntegerDistribution uid) {
		for (int w = 0; w < line.size(); w++) {
			// std::uniform_int_distribution<> uniform(1, args_->ws);
			int boundary = uid.sample();
			final java.util.Vector<Integer> ngrams = dict_.getNgrams(line.get(w));
			for (int c = -boundary; c <= boundary; c++) {
				if (c != 0 && w + c >= 0 && w + c < line.size()) {
					model.update(ngrams, line.get(w + c), lr);
				}
			}
		}
	}

	public void test(InputStream in, int k) throws IOException {
		int nexamples = 0, nlabels = 0;
		double precision = 0.0f;
		java.util.Vector<Integer> line = new java.util.Vector<Integer>();
		java.util.Vector<Integer> labels = new java.util.Vector<Integer>();

		UniformRealDistribution urd = new UniformRealDistribution(model_.rng, 0, 1);
		BufferedReader dis = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		try {
			String lineString;
			while ((lineString = dis.readLine()) != null) {
				if (Utils.isEmpty(lineString) || lineString.startsWith("#")) {
					continue;
				}
				if ("quit".equals(lineString)) {
					break;
				}
				dict_.getLine(lineString, line, labels, urd);
				dict_.addNgrams(line, args_.wordNgrams);
				if (labels.size() > 0 && line.size() > 0) {
					java.util.Vector<Pair<Float, Integer>> modelPredictions = new java.util.Vector<Pair<Float, Integer>>();
					model_.predict(line, k, modelPredictions);
					for (Pair<Float, Integer> pair : modelPredictions) {
						if (labels.contains(pair.getValue())) {
							precision += 1.0f;
						}
					}
					nexamples++;
					nlabels += labels.size();
				} else {
					System.out.println("FAIL Test line: " + lineString + "labels: " + labels + " line: " + line);
				}
			}
		} finally {
			dis.close();
			in.close();
		}

		System.out.printf("P@%d: %.3f%n", k, precision / (k * nexamples));
		System.out.printf("R@%d: %.3f%n", k, precision / nlabels);
		System.out.println("Number of examples: " + nexamples);
	}

	public void predict(String line, int k, java.util.Vector<Pair<Float, String>> predictions,
			UniformRealDistribution urd) throws IOException {
		java.util.Vector<Integer> words = new java.util.Vector<Integer>();
		java.util.Vector<Integer> labels = new java.util.Vector<Integer>();
		dict_.getLine(line, words, labels, urd);
		dict_.addNgrams(words, args_.wordNgrams);

		if (words.isEmpty()) {
			return;
		}
		Vector hidden = new Vector(args_.dim);
		Vector output = new Vector(dict_.nlabels());
		java.util.Vector<Pair<Float, Integer>> modelPredictions = new java.util.Vector<Pair<Float, Integer>>(k + 1);
		model_.predict(words, k, modelPredictions, hidden, output);
		predictions.clear();
		for (Pair<Float, Integer> pair : modelPredictions) {
			predictions.add(new Pair<Float, String>(pair.getKey(), dict_.getLabel(pair.getValue())));
		}
	}

	public void predict(InputStream in, int k, boolean print_prob) throws IOException {
		java.util.Vector<Pair<Float, String>> predictions = new java.util.Vector<Pair<Float, String>>(k);

		UniformRealDistribution urd = new UniformRealDistribution(model_.rng, 0, 1);
		BufferedReader dis = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		try {
			String lineString;
			while ((lineString = dis.readLine()) != null) {
				if (Utils.isEmpty(lineString) || lineString.startsWith("#")) {
					continue;
				}
				if ("quit".equals(lineString)) {
					break;
				}
				predictions.clear();
				predict(lineString, k, predictions, urd);
				if (predictions.isEmpty()) {
					System.out.println("n/a");
					continue;
				}
				for (Pair<Float, String> pair : predictions) {
					System.out.print(pair.getValue());
					if (print_prob) {
						System.out.printf(" %f", Math.exp(pair.getKey()));
					}
				}
				System.out.println();
			}
		} finally {
			dis.close();
			in.close();
		}
	}

	public void wordVectors() {
		Vector vec = new Vector(args_.dim);
		@SuppressWarnings("resource")
		java.util.Scanner scanner = new java.util.Scanner(System.in);
		String word = scanner.nextLine();
		while (!Utils.isEmpty(word)) {
			getVector(vec, word);
			System.out.println(word + " " + vec);
			word = scanner.nextLine();
		}
	}

	public void textVectors() {
		java.util.Vector<Integer> line = new java.util.Vector<Integer>();
		java.util.Vector<Integer> labels = new java.util.Vector<Integer>();
		UniformRealDistribution urd = new UniformRealDistribution(model_.rng, 0, 1);
		Vector vec = new Vector(args_.dim);
		@SuppressWarnings("resource")
		java.util.Scanner scanner = new java.util.Scanner(System.in);
		String word = scanner.nextLine();
		while (!Utils.isEmpty(word)) {
			dict_.getLine(word, line, labels, urd);
			dict_.addNgrams(line, args_.wordNgrams);
			vec.zero();
			for (Integer it : line) {
				vec.addRow(input_, it);
			}
			if (!line.isEmpty()) {
				vec.mul(1.0f / line.size());
			}
			System.out.println(vec);
			word = scanner.nextLine();
		}
	}

	public void printVectors() {
		if (args_.model == model_name.sup) {
			textVectors();
		} else {
			wordVectors();
		}
	}

	public class TrainThread extends Thread {
		final FastText ft;
		int threadId;

		public TrainThread(FastText ft, int threadId) {
			this.ft = ft;
			this.threadId = threadId;
		}

		public void run() {
			if (args_.verbose > 2) {
				System.out.println("thread: " + threadId + " RUNNING!");
			}
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(args_.input));
				Utils.seekLine(br, threadId * threadFileSize / args_.thread);
				Model model = new Model(input_, output_, args_, threadId);
				if (args_.model == model_name.sup) {
					model.setTargetCounts(dict_.getCounts(entry_type.label));
				} else {
					model.setTargetCounts(dict_.getCounts(entry_type.word));
				}

				final long ntokens = dict_.ntokens();
				long localTokenCount = 0;

				java.util.Vector<Integer> line = new java.util.Vector<Integer>();
				java.util.Vector<Integer> labels = new java.util.Vector<Integer>();
				UniformRealDistribution urd = new UniformRealDistribution(model.rng, 0, 1);

				UniformIntegerDistribution learnUid = null;
				List<UniformIntegerDistribution> learnUid0 = new ArrayList<UniformIntegerDistribution>();
				if (args_.model == model_name.sup) {
					for (int i = 0; i <= SUPERVISED_LABEL_SIZE; i++) {
						learnUid0.add(new UniformIntegerDistribution(model.rng, 0, i));
					}
				} else if (args_.model == model_name.cbow || args_.model == model_name.sg) {
					learnUid = new UniformIntegerDistribution(model.rng, 1, args_.ws);
				}

				String lineString;
				while (tokenCount.get() < args_.epoch * ntokens) {
					lineString = br.readLine();
					if (lineString == null) {
						try {
							br.close();
							br = new BufferedReader(new FileReader(args_.input));
							if (args_.verbose > 2) {
								System.out.println("Input file reloaded!");
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						lineString = br.readLine();
					}
					while (Utils.isEmpty(lineString) || lineString.startsWith("#")) {
						lineString = br.readLine();
					}

					float progress = (float) (tokenCount.get()) / (args_.epoch * ntokens);
					float lr = (float) (args_.lr * (1.0 - progress));
					localTokenCount += dict_.getLine(lineString, line, labels, urd);
					if (args_.model == model_name.sup) {
						dict_.addNgrams(line, args_.wordNgrams);
						if (labels.size() == 0 || line.size() == 0) {
							continue;
						}
						if (labels.size() > SUPERVISED_LABEL_SIZE) {
							learnUid = new UniformIntegerDistribution(model.rng, 0, labels.size() - 1);
						} else {
							learnUid = learnUid0.get(labels.size() - 1);
						}
						supervised(model, lr, line, labels, learnUid);
					} else if (args_.model == model_name.cbow) {
						cbow(model, lr, line, learnUid);
					} else if (args_.model == model_name.sg) {
						skipgram(model, lr, line, learnUid);
					}
					if (localTokenCount > args_.lrUpdateRate) {
						tokenCount.addAndGet(localTokenCount);
						localTokenCount = 0;
						if (threadId == 0 && args_.verbose > 1) {
							printInfo(progress, model.getLoss());
						}
					}
				}

				if (threadId == 0 && args_.verbose > 1) {
					printInfo(1.0f, model.getLoss());
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			} finally {
				if (br != null)
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}

			// exit from thread
			synchronized (ft) {
				if (args_.verbose > 2) {
					System.out.println("\nthread: " + threadId + " EXIT!");
				}
				ft.threadCount--;
				ft.notify();
			}
		}
	}

	public void loadVectors(String filename) throws IOException {
		java.util.Vector<String> words = new java.util.Vector<String>();
		Matrix mat; // temp. matrix for pretrained vectors
		int n, dim;
		FileInputStream fis = null;
		BufferedReader dis = null;

		try {
			fis = new FileInputStream(filename);
			dis = new BufferedReader(new InputStreamReader(fis, "UTF-8"));

			n = dis.read();
			dis.read(); // read ' '
			dim = dis.read();
			dis.read(); // read '\n'

			if (dim != args_.dim) {
				System.err.println("Dimension of pretrained vectors does not match -dim option");
				System.exit(1);
			}

			mat = new Matrix(n, dim);
			for (int i = 0; i < n; i++) {
				String line = dis.readLine();
				for (int j = dim - 1; j >= 0; j--) {
					mat.data_[i][j] = Float.parseFloat(line.substring(line.lastIndexOf(" ") + 1, line.length()));
					line = line.substring(0, line.lastIndexOf(" "));
				}
				String word = line.substring(0, line.lastIndexOf(" "));
				words.add(word);
				dict_.add(word);
			}

			dict_.threshold(1, 0);
			input_ = new Matrix(dict_.nwords() + args_.bucket, args_.dim);
			input_.uniform(1.0f / args_.dim);
			for (int i = 0; i < n; i++) {
				int idx = dict_.getId(words.get(i));
				if (idx < 0 || idx >= dict_.nwords())
					continue;
				for (int j = 0; j < dim; j++) {
					input_.data_[idx][j] = mat.data_[i][j];
				}
			}

		} catch (IOException e) {
			throw new IOException("Pretrained vectors file cannot be opened!", e);
		} finally {
			try {
				if (dis != null)
					dis.close();
				if (fis != null)
					fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	int threadCount;
	long threadFileSize;

	public void train(Args args) throws IOException {
		args_ = args;
		dict_ = new Dictionary(args_);

		if ("-".equals(args_.input)) {
			throw new IOException("Cannot use stdin for training!");
		}

		File file = new File(args_.input);
		if (!(file.exists() && file.isFile() && file.canRead())) {
			throw new IOException("Input file cannot be opened! " + args_.input);
		}

		dict_.readFromFile(args_.input);
		threadFileSize = Utils.sizeLine(args_.input);

		if (!Utils.isEmpty(args_.pretrainedVectors)) {
			loadVectors(args_.pretrainedVectors);
		} else {
			input_ = new Matrix(dict_.nwords() + args_.bucket, args_.dim);
			input_.uniform(1.0f / args_.dim);
		}

		if (args_.model == model_name.sup) {
			output_ = new Matrix(dict_.nlabels(), args_.dim);
		} else {
			output_ = new Matrix(dict_.nwords(), args_.dim);
		}
		output_.zero();

		start = System.currentTimeMillis();
		tokenCount = new AtomicLong(0);
		long t0 = System.currentTimeMillis();
		threadCount = args_.thread;
		for (int i = 0; i < args_.thread; i++) {
			new TrainThread(this, i).start();
		}

		synchronized (this) {
			while (threadCount > 0) {
				try {
					wait();
				} catch (InterruptedException ignored) {
				}
			}
		}

		model_ = new Model(input_, output_, args_, 0);

		long trainTime = (System.currentTimeMillis() - t0) / 1000;
		System.out.printf("\nTrain time used: %d sec\n", trainTime);

		saveModel();
		if (args_.model != model_name.sup) {
			saveVectors();
		}
	}

	public Args getArgs_() {
		return args_;
	}

	public Dictionary getDict_() {
		return dict_;
	}

	public Matrix getInput_() {
		return input_;
	}

	public Matrix getOutput_() {
		return output_;
	}

	public Model getModel_() {
		return model_;
	}

}
