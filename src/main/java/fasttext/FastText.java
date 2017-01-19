package fasttext;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import fasttext.Args.model_name;
import fasttext.Dictionary.entry_type;
import fasttext.io.*;

/**
 * FastText class, can be used as a lib in other projects
 * 
 * @author Ivan
 *
 */
public class FastText {

	private Args args_;
	private Dictionary dict_;
	private Matrix input_;
	private Matrix output_;
	private Model model_;

	private AtomicLong tokenCount_;
	private long start_;

	private String charsetName_ = "UTF-8";
	private Class<? extends LineReader> lineReaderClass_ = BufferedLineReader.class;

	public void getVector(Vector vec, final String word) {
		final List<Integer> ngrams = dict_.getNgrams(word);
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

		Vector vec = new Vector(args_.dim);
		DecimalFormat df = new DecimalFormat("0.#####");
		Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file)), "UTF-8");
		try {
			writer.write(dict_.nwords() + " " + args_.dim + "\n");
			for (int i = 0; i < dict_.nwords(); i++) {
				String word = dict_.getWord(i);
				getVector(vec, word);
				writer.write(word);
				for (int j = 0; j < vec.m_; j++) {
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

	/**
	 * Load binary model file.
	 * 
	 * @param filename
	 * @throws IOException
	 */
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
		float t = (float) (System.currentTimeMillis() - start_) / 1000;
		float wst = (float) (tokenCount_.get()) / t;
		float lr = (float) (args_.lr * (1.0f - progress));
		int eta = (int) (t / progress * (1 - progress) / args_.thread);
		int etah = eta / 3600;
		int etam = (eta - etah * 3600) / 60;
		System.out.printf("\rProgress: %.1f%% words/sec/thread: %d lr: %.6f loss: %.6f eta: %d h %d m", 100 * progress,
				(int) wst, lr, loss, etah, etam);
	}

	public void supervised(Model model, float lr, final List<Integer> line, final List<Integer> labels) {
		if (labels.size() == 0 || line.size() == 0)
			return;
		int i = Utils.randomInt(model.rng, 1, labels.size()) - 1;
		model.update(line, labels.get(i), lr);
	}

	public void cbow(Model model, float lr, final List<Integer> line) {
		List<Integer> bow = new ArrayList<Integer>();
		for (int w = 0; w < line.size(); w++) {
			int boundary = Utils.randomInt(model.rng, 1, args_.ws);
			bow.clear();
			for (int c = -boundary; c <= boundary; c++) {
				if (c != 0 && w + c >= 0 && w + c < line.size()) {
					final List<Integer> ngrams = dict_.getNgrams(line.get(w + c));
					bow.addAll(ngrams);
				}
			}
			model.update(bow, line.get(w), lr);
		}
	}

	public void skipgram(Model model, float lr, final List<Integer> line) {
		for (int w = 0; w < line.size(); w++) {
			int boundary = Utils.randomInt(model.rng, 1, args_.ws);
			final List<Integer> ngrams = dict_.getNgrams(line.get(w));
			for (int c = -boundary; c <= boundary; c++) {
				if (c != 0 && w + c >= 0 && w + c < line.size()) {
					model.update(ngrams, line.get(w + c), lr);
				}
			}
		}
	}

	public void test(InputStream in, int k) throws IOException, Exception {
		int nexamples = 0, nlabels = 0;
		double precision = 0.0f;
		List<Integer> line = new ArrayList<Integer>();
		List<Integer> labels = new ArrayList<Integer>();

		LineReader lineReader = null;
		try {
			lineReader = lineReaderClass_.getConstructor(InputStream.class, String.class).newInstance(in, charsetName_);
			String[] lineTokens;
			while ((lineTokens = lineReader.readLineTokens()) != null) {
				if (lineTokens.length == 1 && "quit".equals(lineTokens[0])) {
					break;
				}
				dict_.getLine(lineTokens, line, labels, model_.rng);
				dict_.addNgrams(line, args_.wordNgrams);
				if (labels.size() > 0 && line.size() > 0) {
					List<Pair<Float, Integer>> modelPredictions = new ArrayList<Pair<Float, Integer>>();
					model_.predict(line, k, modelPredictions);
					for (Pair<Float, Integer> pair : modelPredictions) {
						if (labels.contains(pair.getValue())) {
							precision += 1.0f;
						}
					}
					nexamples++;
					nlabels += labels.size();
					// } else {
					// System.out.println("FAIL Test line: " + lineTokens +
					// "labels: " + labels + " line: " + line);
				}
			}
		} finally {
			if (lineReader != null) {
				lineReader.close();
			}
		}

		System.out.printf("P@%d: %.3f%n", k, precision / (k * nexamples));
		System.out.printf("R@%d: %.3f%n", k, precision / nlabels);
		System.out.println("Number of examples: " + nexamples);
	}

	public void predict(String[] lineTokens, int k, List<Pair<Float, String>> predictions, Random urd)
			throws IOException {
		List<Integer> words = new ArrayList<Integer>();
		List<Integer> labels = new ArrayList<Integer>();
		dict_.getLine(lineTokens, words, labels, urd);
		dict_.addNgrams(words, args_.wordNgrams);

		if (words.isEmpty()) {
			return;
		}
		Vector hidden = new Vector(args_.dim);
		Vector output = new Vector(dict_.nlabels());
		List<Pair<Float, Integer>> modelPredictions = new ArrayList<Pair<Float, Integer>>(k + 1);
		model_.predict(words, k, modelPredictions, hidden, output);
		predictions.clear();
		for (Pair<Float, Integer> pair : modelPredictions) {
			predictions.add(new Pair<Float, String>(pair.getKey(), dict_.getLabel(pair.getValue())));
		}
	}

	public void predict(InputStream in, int k, boolean print_prob) throws IOException, Exception {
		List<Pair<Float, String>> predictions = new ArrayList<Pair<Float, String>>(k);

		LineReader lineReader = null;

		try {
			lineReader = lineReaderClass_.getConstructor(InputStream.class, String.class).newInstance(in, charsetName_);
			String[] lineTokens;
			while ((lineTokens = lineReader.readLineTokens()) != null) {
				if (lineTokens.length == 1 && "quit".equals(lineTokens[0])) {
					break;
				}
				predictions.clear();
				predict(lineTokens, k, predictions, model_.rng);
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
			if (lineReader != null) {
				lineReader.close();
			}
		}
	}

	public void wordVectors() {
		Vector vec = new Vector(args_.dim);
		LineReader lineReader = null;
		try {
			lineReader = lineReaderClass_.getConstructor(InputStream.class, String.class).newInstance(System.in,
					charsetName_);
			String word;
			while (!Utils.isEmpty((word = lineReader.readLine()))) {
				getVector(vec, word);
				System.out.println(word + " " + vec);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (lineReader != null) {
				try {
					lineReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void textVectors() {
		List<Integer> line = new ArrayList<Integer>();
		List<Integer> labels = new ArrayList<Integer>();
		Vector vec = new Vector(args_.dim);
		LineReader lineReader = null;
		try {
			lineReader = lineReaderClass_.getConstructor(InputStream.class, String.class).newInstance(System.in,
					charsetName_);
			String[] lineTokens;
			while ((lineTokens = lineReader.readLineTokens()) != null) {
				if (lineTokens.length == 1 && "quit".equals(lineTokens[0])) {
					break;
				}
				dict_.getLine(lineTokens, line, labels, model_.rng);
				dict_.addNgrams(line, args_.wordNgrams);
				vec.zero();
				for (Integer it : line) {
					vec.addRow(input_, it);
				}
				if (!line.isEmpty()) {
					vec.mul(1.0f / line.size());
				}
				System.out.println(vec);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (lineReader != null) {
				try {
					lineReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
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
			LineReader lineReader = null;
			try {
				lineReader = lineReaderClass_.getConstructor(String.class, String.class).newInstance(args_.input,
						charsetName_);
				lineReader.skipLine(threadId * threadFileSize / args_.thread);
				Model model = new Model(input_, output_, args_, threadId);
				if (args_.model == model_name.sup) {
					model.setTargetCounts(dict_.getCounts(entry_type.label));
				} else {
					model.setTargetCounts(dict_.getCounts(entry_type.word));
				}

				final long ntokens = dict_.ntokens();
				long localTokenCount = 0;

				List<Integer> line = new ArrayList<Integer>();
				List<Integer> labels = new ArrayList<Integer>();

				String[] lineTokens;
				while (tokenCount_.get() < args_.epoch * ntokens) {
					lineTokens = lineReader.readLineTokens();
					if (lineTokens == null) {
						try {
							lineReader.rewind();
							if (args_.verbose > 2) {
								System.out.println("Input file reloaded!");
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						lineTokens = lineReader.readLineTokens();
					}

					float progress = (float) (tokenCount_.get()) / (args_.epoch * ntokens);
					float lr = (float) (args_.lr * (1.0 - progress));
					localTokenCount += dict_.getLine(lineTokens, line, labels, model.rng);
					if (args_.model == model_name.sup) {
						dict_.addNgrams(line, args_.wordNgrams);
						if (labels.size() == 0 || line.size() == 0) {
							continue;
						}
						supervised(model, lr, line, labels);
					} else if (args_.model == model_name.cbow) {
						cbow(model, lr, line);
					} else if (args_.model == model_name.sg) {
						skipgram(model, lr, line);
					}
					if (localTokenCount > args_.lrUpdateRate) {
						tokenCount_.addAndGet(localTokenCount);
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
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			} finally {
				if (lineReader != null)
					try {
						lineReader.close();
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
		List<String> words;
		Matrix mat; // temp. matrix for pretrained vectors
		int n, dim;

		BufferedReader dis = null;
		String line;
		String[] lineParts;
		try {
			dis = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));

			line = dis.readLine();
			lineParts = line.split(" ");
			n = Integer.parseInt(lineParts[0]);
			dim = Integer.parseInt(lineParts[1]);

			words = new ArrayList<String>(n);

			if (dim != args_.dim) {
				System.err.println("Dimension of pretrained vectors does not match -dim option");
				System.exit(1);
			}

			mat = new Matrix(n, dim);
			for (int i = 0; i < n; i++) {
				line = dis.readLine();
				lineParts = line.split(" ");
				String word = lineParts[0];
				for (int j = 1; j <= dim; j++) {
					mat.data_[i][j - 1] = Float.parseFloat(lineParts[j]);
				}
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
				if (dis != null) {
					dis.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	int threadCount;
	long threadFileSize;

	public void train(Args args) throws IOException, Exception {
		args_ = args;
		dict_ = new Dictionary(args_);
		dict_.setCharsetName(charsetName_);
		dict_.setLineReaderClass(lineReaderClass_);

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

		start_ = System.currentTimeMillis();
		tokenCount_ = new AtomicLong(0);
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

	public Args getArgs() {
		return args_;
	}

	public Dictionary getDict() {
		return dict_;
	}

	public Matrix getInput() {
		return input_;
	}

	public Matrix getOutput() {
		return output_;
	}

	public Model getModel() {
		return model_;
	}

	public void setArgs(Args args) {
		this.args_ = args;
	}

	public void setDict(Dictionary dict) {
		this.dict_ = dict;
	}

	public void setInput(Matrix input) {
		this.input_ = input;
	}

	public void setOutput(Matrix output) {
		this.output_ = output;
	}

	public void setModel(Model model) {
		this.model_ = model;
	}

	public String getCharsetName() {
		return charsetName_;
	}

	public Class<? extends LineReader> getLineReaderClass() {
		return lineReaderClass_;
	}

	public void setCharsetName(String charsetName) {
		this.charsetName_ = charsetName;
	}

	public void setLineReaderClass(Class<? extends LineReader> lineReaderClass) {
		this.lineReaderClass_ = lineReaderClass;
	}

}
