package fasttext;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.log4j.Logger;

import fasttext.Args.model_name;
import fasttext.Dictionary.entry_type;

public class FastText {

	private static Logger logger = Logger.getLogger(FastText.class);
	private static int SUPERVISED_LABEL_SIZE = 10;

	Args args = new Args();
	Dictionary dict = new Dictionary(args);
	Matrix input = new Matrix();
	Matrix output = new Matrix();

	class Info {
		long start = 0;
		AtomicLong allWords = new AtomicLong(0l);
		AtomicLong allN = new AtomicLong(0l);
		double allLoss = 0.0;
	}

	Info info = new Info();

	public void loadModel(String filename, Dictionary dict, Matrix input, Matrix output) throws IOException {
		DataInputStream dis = null;
		BufferedInputStream bis = null;
		try {
			File file = new File(filename);
			if (!(file.exists() && file.isFile() && file.canRead())) {
				throw new IOException("Model file cannot be opened for loading!");
			}
			bis = new BufferedInputStream(new FileInputStream(file));
			dis = new DataInputStream(bis);

			args.load(dis);
			dict.load(dis);
			input.load(dis);
			output.load(dis);

			logger.info("loadModel done!");
		} finally {
			bis.close();
			dis.close();
		}
	}

	public void getVector(Dictionary dict, Matrix input, Vector vec, String word) {
		final java.util.Vector<Integer> ngrams = dict.getNgrams(word);
		vec.zero();
		for (Integer it : ngrams) {
			vec.addRow(input, it);
		}
		if (ngrams.size() > 0) {
			vec.mul((float) (1.0 / ngrams.size()));
		}
	}

	public void printVectors(Dictionary dict, Matrix input) {
		Vector vec = new Vector(args.dim);
		@SuppressWarnings("resource")
		java.util.Scanner scanner = new java.util.Scanner(System.in);
		String word = scanner.nextLine();
		while (!Utils.isEmpty(word)) {
			getVector(dict, input, vec, word);
			System.out.println(word + " " + vec);
			word = scanner.nextLine();
		}
	}

	public void printInfo(Model model, float progress) {
		float loss = (float) (info.allLoss / info.allN.get());
		float t = (float) ((System.currentTimeMillis() - info.start) / 1000);
		float wst = (float) (info.allWords.get() / t);
		int eta = (int) (t / progress * (1 - progress) / args.thread);
		int etah = eta / 3600;
		int etam = (eta - etah * 3600) / 60;
		System.out.printf("\rProgress: %.1f%% words/sec/thread: %d lr: %.6f loss: %.6f eta: %d h %d m", 100 * progress,
				(int) wst, model.getLearningRate(), loss, etah, etam);
	}

	public int supervised(Model model, final java.util.Vector<Integer> line, final java.util.Vector<Integer> labels,
			double loss, UniformIntegerDistribution uid) {
		if (labels.size() == 0 || line.size() == 0)
			return 0;
		int i = uid.sample();
		loss += model.update(line, labels.get(i));
		return 1;
	}

	public int cbow(Dictionary dict, Model model, final java.util.Vector<Integer> line, double loss,
			UniformIntegerDistribution uid) {
		java.util.Vector<Integer> bow = new java.util.Vector<Integer>();
		int nexamples = 0;
		for (int w = 0; w < line.size(); w++) {
			int boundary = uid.sample();
			bow.clear();
			for (int c = -boundary; c <= boundary; c++) {
				if (c != 0 && w + c >= 0 && w + c < line.size()) {
					final java.util.Vector<Integer> ngrams = dict.getNgrams(line.get(w + c));
					bow.addAll(ngrams);
				}
			}
			loss += model.update(bow, line.get(w));
			nexamples++;
		}
		return nexamples;
	}

	public int skipgram(Dictionary dict, Model model, final java.util.Vector<Integer> line, double loss,
			UniformIntegerDistribution uid) {
		int nexamples = 0;
		for (int w = 0; w < line.size(); w++) {
			int boundary = uid.sample();
			final java.util.Vector<Integer> ngrams = dict.getNgrams(line.get(w));
			for (int c = -boundary; c <= boundary; c++) {
				if (c != 0 && w + c >= 0 && w + c < line.size()) {
					loss += model.update(ngrams, line.get(w + c));
					nexamples++;
				}
			}
		}
		return nexamples;
	}

	public void test(Dictionary dict, Model model, String filename) throws IOException {
		int nexamples = 0;
		double precision = 0.0f;
		java.util.Vector<Integer> line = new java.util.Vector<Integer>();
		java.util.Vector<Integer> labels = new java.util.Vector<Integer>();

		File file = new File(filename);
		if (!(file.exists() && file.isFile() && file.canRead())) {
			throw new IOException("Test file cannot be opened!");
		}
		UniformRealDistribution urd = new UniformRealDistribution(model.rng, 0, 1);
		FileInputStream fis = new FileInputStream(file);
		BufferedReader dis = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
		try {
			String lineString;
			while ((lineString = dis.readLine()) != null) {
				dict.getLine(lineString, line, labels, urd);
				dict.addNgrams(line, args.wordNgrams);
				if (labels.size() > 0 && line.size() > 0) {
					System.out.print("Test line: " + lineString);
					int i = model.predict(line);
					if (labels.contains(i)) {
						precision += 1.0;
						System.out.println(" [HIT]: " + dict.getLabel(i));
					} else {
						System.out.println(" [MISSED]: " + dict.getLabel(i));
					}
					nexamples++;
				} else {
					System.out.println("FAIL Test line: " + lineString + "labels: " + labels + " line: " + line);
				}
			}

		} finally {
			dis.close();
			fis.close();
		}

		System.out.printf("P@1: %.3f%n", precision / nexamples);
		System.out.println("Number of examples: " + nexamples);
	}

	public void predict(Dictionary dict, Model model, String filename) throws IOException {
		// int nexamples = 0;
		// double precision = 0.0;
		java.util.Vector<Integer> line = new java.util.Vector<Integer>();
		java.util.Vector<Integer> labels = new java.util.Vector<Integer>();

		File file = new File(filename);
		if (!(file.exists() && file.isFile() && file.canRead())) {
			throw new IOException("Test file cannot be opened!");
		}
		UniformRealDistribution urd = new UniformRealDistribution(model.rng, 0, 1);
		FileInputStream fis = new FileInputStream(file);
		BufferedReader dis = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
		try {
			String lineString;
			while ((lineString = dis.readLine()) != null) {
				dict.getLine(lineString, line, labels, urd);
				dict.addNgrams(line, args.wordNgrams);
				if (line.size() > 0) {
					int i = model.predict(line);
					System.out.println(lineString + "\t" + dict.getLabel(i));
				} else {
					System.out.println(lineString + "\tn/a");
				}
			}
		} finally {
			dis.close();
			fis.close();
		}
	}

	public void test(String binFile, String testFile) throws IOException {
		loadModel(binFile, dict, input, output);
		Model model = new Model(args, input, output, args.dim, (float) args.lr, 1);
		model.setTargetCounts(dict.getCounts(entry_type.label));
		test(dict, model, testFile);
	}

	public void predict(String binFile, String predictFile) throws IOException {
		loadModel(binFile, dict, input, output);
		Model model = new Model(args, input, output, args.dim, (float) args.lr, 1);
		model.setTargetCounts(dict.getCounts(entry_type.label));
		predict(dict, model, predictFile);
	}

	public void printVectors(String binFile) throws IOException {
		loadModel(binFile, dict, input, output);
		printVectors(dict, input);
	}

	int threadCount;

	public void train(String[] args_) throws IOException {
		args.parseArgs(args_);
		File file = new File(args.input);
		if (!(file.exists() && file.isFile() && file.canRead())) {
			throw new IOException("Input file cannot be opened! " + args.input);
		}

		dict.readFromFile(args.input);

		input = new Matrix(dict.nwords() + args.bucket, args.dim);
		if (args.model == model_name.sup) {
			output = new Matrix(dict.nlabels(), args.dim);
		} else {
			output = new Matrix(dict.nwords(), args.dim);
		}
		input.uniform((float) (1.0 / args.dim));
		output.zero();

		info.start = System.currentTimeMillis();
		long t0 = System.currentTimeMillis();

		threadCount = args.thread;
		long fileSize = Utils.sizeLine(args.input);
		for (int i = 0; i < args.thread; i++) {
			new TrainThread(this, dict, input, output, i, fileSize).start();
		}

		synchronized (this) {
			while (threadCount > 0) {
				try {
					wait();
				} catch (InterruptedException ignored) {
				}
			}
		}
		long trainTime = (System.currentTimeMillis() - t0) / 1000;
		System.out.printf("Train time: %d sec\n", trainTime);

		if (!Utils.isEmpty(args.output)) {
			saveModel(dict, input, output);
			saveVectors(dict, input, output);
		}
	}

	public class TrainThread extends Thread {
		final FastText ft;
		Dictionary dict;
		Matrix input;
		Matrix output;
		int threadId;
		long fileSize;

		public TrainThread(FastText ft, Dictionary dict, Matrix input, Matrix output, int threadId, long fileSize) {
			this.ft = ft;
			this.dict = dict;
			this.input = input;
			this.output = output;
			this.threadId = threadId;
			this.fileSize = fileSize;
		}

		public void run() {
			if (logger.isDebugEnabled()) {
				logger.debug("thread: " + threadId + " RUNNING!");
			}
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(args.input));
				Utils.seek(br, threadId * fileSize / args.thread);
				Model model = new Model(args, input, output, args.dim, (float) args.lr, threadId);
				if (args.model == model_name.sup) {
					model.setTargetCounts(dict.getCounts(entry_type.label));
				} else {
					model.setTargetCounts(dict.getCounts(entry_type.word));
				}
				float progress;
				final long ntokens = dict.ntokens();
				long tokenCount = 0, /** printCount = 0, */
						deltaCount = 0;
				double loss = 0.0;
				long nexamples = 0;

				java.util.Vector<Integer> line = new java.util.Vector<Integer>();
				java.util.Vector<Integer> labels = new java.util.Vector<Integer>();
				UniformRealDistribution urd = new UniformRealDistribution(model.rng, 0, 1);

				List<UniformIntegerDistribution> learnUid0 = new ArrayList<UniformIntegerDistribution>();
				UniformIntegerDistribution learnUid = null;
				if (args.model == model_name.sup) {
					for (int i = 0; i <= SUPERVISED_LABEL_SIZE; i++) {
						learnUid0.add(new UniformIntegerDistribution(model.rng, 0, i));
					}
				} else if (args.model == model_name.cbow) {
					learnUid = new UniformIntegerDistribution(model.rng, 1, args.ws);
				} else if (args.model == model_name.sg) {
					learnUid = new UniformIntegerDistribution(model.rng, 1, args.ws);
				}
				String lineString;
				while (info.allWords.get() < args.epoch * ntokens) {
					lineString = br.readLine();
					if (lineString == null) {
						try {
							br.close();
							br = new BufferedReader(new FileReader(args.input));
							if (logger.isDebugEnabled()) {
								logger.debug("Input file reloaded!");
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						lineString = br.readLine();
					}
					while (Utils.isEmpty(lineString) || lineString.startsWith("#")) {
						lineString = br.readLine();
					}
					deltaCount = dict.getLine(lineString, line, labels, urd);
					tokenCount += deltaCount;
					// printCount += deltaCount;
					if (args.model == model_name.sup) {
						dict.addNgrams(line, args.wordNgrams);
						if (labels.size() == 0 || line.size() == 0) {
							continue;
						}
						learnUid = learnUid0.get(labels.size() - 1);
						nexamples += supervised(model, line, labels, loss, learnUid);
					} else if (args.model == model_name.cbow) {
						nexamples += cbow(dict, model, line, loss, learnUid);
					} else if (args.model == model_name.sg) {
						nexamples += skipgram(dict, model, line, loss, learnUid);
					}
					if (tokenCount > args.lrUpdateRate) {
						info.allWords.addAndGet(tokenCount);
						info.allLoss += loss;
						info.allN.addAndGet(nexamples);
						tokenCount = 0;
						loss = 0.0;
						nexamples = 0;
						progress = (float) (info.allWords.get()) / (args.epoch * ntokens);
						model.setLearningRate((float) (args.lr * (1.0 - progress)));
						if (threadId == 0) {
							printInfo(model, progress);
						}
					}
				}
				if (threadId == 0) {
					printInfo(model, 1.0f);
					System.out.println();
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
				if (logger.isDebugEnabled()) {
					logger.debug("thread: " + threadId + " EXIT!");
				}
				ft.threadCount--;
				ft.notify();
			}
		}
	}

	public void saveModel(Dictionary dict, Matrix input, Matrix output) throws IOException {
		File file = new File(args.output + ".bin");
		if (file.exists()) {
			file.delete();
		}
		logger.info("Saving model to " + file.getAbsolutePath().toString());
		FileOutputStream fos = new FileOutputStream(file);
		OutputStream ofs = new DataOutputStream(fos);
		try {
			logger.debug("writing args");
			args.save(ofs);
			logger.debug("writing dict");
			dict.save(ofs);
			logger.debug("writing input");
			input.save(ofs);
			logger.debug("writing output");
			output.save(ofs);
		} finally {
			ofs.flush();
			ofs.close();
		}
	}

	public void saveVectors(Dictionary dict, Matrix input, Matrix output) throws IOException {
		File file = new File(args.output + ".vec");
		if (file.exists()) {
			file.delete();
		}
		logger.info("Saving Vectors to " + file.getAbsolutePath().toString());
		Writer writer = new FileWriter(file);
		try {
			writer.write(dict.nwords());
			writer.write(" ");
			writer.write(args.dim);
			writer.write("\n");
			Vector vec = new Vector(args.dim);
			DecimalFormat df = new DecimalFormat("0.#####");
			for (int i = 0; i < dict.nwords(); i++) {
				String word = dict.getWord(i);
				getVector(dict, input, vec, word);
				writer.write(word);
				writer.write(" ");
				writer.write(" ");
				for (int j = 0; i < vec.m_; i++) {
					writer.write(df.format(vec.data_[j]));
					writer.write(" ");
				}
				writer.write("\n");
			}
		} finally {
			writer.flush();
			writer.close();
		}
	}

	public static void printUsage() {
		System.out.print(
				"usage: java -jar fasttext.jar <command> <args>\n\n" + "The commands supported by fasttext are:\n\n"
						+ " supervised train a supervised classifier\n" + " test evaluate a supervised classifier\n"
						+ " predict predict most likely label\n" + " skipgram train a skipgram model\n"
						+ " cbow train a cbow model\n" + " print-vectors print vectors given a trained model\n");
	}

	public static void printTestUsage() {
		System.out.print("usage: java -jar fasttext.jar test <model> <test-data>\n\n" + " <model> model filename\n"
				+ " <test-data> test data filename\n");
	}

	public static void printPredictUsage() {
		System.out.print("usage: java -jar fasttext.jar predict <model> <test-data>\n\n" + " <model> model filename\n"
				+ " <test-data> test data filename\n");
	}

	public static void printPrintVectorsUsage() {
		System.out.print("usage: java -jar fasttext.jar print-vectors <model>\n\n" + " <model> model filename\n");
	}

	public static void main(String[] args) {
		org.apache.log4j.PropertyConfigurator.configure("log4j.properties");
		FastText op = new FastText();

		if (args.length == 0) {
			printUsage();
			System.exit(1);
		}

		try {
			String command = args[0];
			if ("skipgram".equalsIgnoreCase(command) || "cbow".equalsIgnoreCase(command)
					|| "supervised".equalsIgnoreCase(command)) {
				op.train(args);
			} else if ("test".equalsIgnoreCase(command)) {
				if (args.length != 3) {
					printTestUsage();
					System.exit(1);
				}
				op.test(args[1], args[2]);
			} else if ("print-vectors".equalsIgnoreCase(command)) {
				op.printVectors(args[1]);
			} else if ("predict".equalsIgnoreCase(command)) {
				if (args.length != 3) {
					printPredictUsage();
					System.exit(1);
				}
				op.predict(args[1], args[2]);
			} else {
				printUsage();
				System.exit(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.exit(0);
	}

}
