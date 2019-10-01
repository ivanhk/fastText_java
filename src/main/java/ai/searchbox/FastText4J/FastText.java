package ai.searchbox.FastText4J;

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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import ai.searchbox.FastText4J.io.BufferedLineReader;
import ai.searchbox.FastText4J.io.IOUtil;
import ai.searchbox.FastText4J.io.LineReader;
import ai.searchbox.FastText4J.math.Matrix;
import ai.searchbox.FastText4J.math.MatrixQ;
import ai.searchbox.FastText4J.math.Vector;
import org.apache.log4j.Logger;

public class FastText {
	private final static Logger logger = Logger.getLogger(FastText.class.getName());

	public static int FASTTEXT_VERSION = 12; /* Version 1b */
	public static int FASTTEXT_FILEFORMAT_MAGIC_INT = 793712314;

	private long start_;
	int threadCount;
	long threadFileSize;

	private Args args_;
	private Dictionary dict_;
	private Model model_;

	private Matrix input_;
	private Matrix output_;

	private AtomicLong tokenCount_;

	private String charsetName_ = "UTF-8";
	private Class<? extends LineReader> lineReaderClass_ = BufferedLineReader.class;

	public Args getArgs() {
		return args_;
	}

	public void setArgs(Args args) {
		args_ = args;
		dict_ = new Dictionary(args);
	}

	public Vector getWordVectorIn(final String word) {
		Vector vec = new Vector(args_.dim);
		vec.zero();

		final List<Integer> ngrams = dict_.getNgrams(word);

		for (Integer it : ngrams) {
			vec.addRow(input_, it);
		}

		if (ngrams.size() > 0) {
			vec.mul(1.0f / ngrams.size());
		}

		return vec;
	}

	public Vector getWordVectorOut(String word) {
		int id = dict_.getId(word);

		Vector vec = new Vector(args_.dim);
		vec.zero();
		vec.addRow(output_, id);

		return vec;
	}

    public Vector getSentenceVector(List<String> sentence) {
        Vector svec = new Vector(args_.dim);
        svec.zero();

        if (args_.model == Args.ModelType.sup) {
            List<Integer> tokens = new ArrayList<>();
            List<Integer> labels = new ArrayList<>();
            dict_.getLine(sentence, tokens, labels);
            for (int i = 0; i < tokens.size(); i++) {
                svec.addRow(input_, tokens.get(i));
            }

            if (!tokens.isEmpty()) {
                svec.mul(1.0f / (float) tokens.size());
            }

        } else {
            int count = 0;
            for (String word : sentence) {
                Vector vec = getWordVectorIn(word);

                svec.addVector(vec);
                count++;
            }

            if (count > 0) {
                svec.mul(1.0f / (float) count);
            }
        }

        return svec;
    }

    public Vector getSentenceVectorOut(List<String> sentence) {
        Vector svec = new Vector(args_.dim);
        svec.zero();

        int count = 0;
        for (String word : sentence) {
            Vector vec = getWordVectorOut(word);

            svec.addVector(vec);
            count++;
        }

        if (count > 0) {
            svec.mul(1.0f / (float) count);
        }

        return svec;
    }

	public List<Pair<Float, String>> predict(String[] lineTokens, int k) {
		List<Pair<Float, String>> predictions =  new ArrayList<>();

		List<Integer> words = new ArrayList<Integer>();
		List<Integer> labels = new ArrayList<Integer>();

		dict_.getLine(lineTokens, words, labels, model_.rng);
		dict_.addNgrams(words, args_.wordNgrams);

		if (words.isEmpty())  return predictions;

		List<Pair<Float, Integer>> modelPredictions = new ArrayList<Pair<Float, Integer>>(k + 1);
		model_.predict(words, k, modelPredictions);

		for (Pair<Float, Integer> pair : modelPredictions) {
			predictions.add(new Pair<Float, String>(pair.getKey(), dict_.getLabel(pair.getValue())));
		}

		return predictions;
	}

    public List<FastTextSynonym> findNNOut(java.util.Vector queryVec, int k, Set<String> banSet) {
        return findNN(wordVectorsOut, queryVec, k, banSet);
    }

    public List<FastTextSynonym> findNN(java.util.Vector queryVec, int k, Set<String> banSet) {
        return findNN(wordVectors, queryVec, k, banSet);
    }

    public List<FastTextSynonym> findNN(Matrix wordVectors, java.util.Vector queryVec, int k, Set<String> banSet) {
        MinMaxPriorityQueue<Pair<Float, String>> heap = MinMaxPriorityQueue
                .orderedBy(new HeapComparator<String>())
                .expectedSize(dict.nLabels())
                .create();

        float queryNorm = queryVec.norm();
        if (queryNorm > 0) {
            queryVec.mul(1.0f / queryNorm);
        }

        for (int i = 0; i < dict.nWords(); i++) {
            String word = dict.getWord(i);
            float dp = wordVectors.dotRow(queryVec, i);
            heap.add(new Pair<>(dp, word));
        }

        List<FastTextSynonym> syns = new ArrayList<>();
        int i = 0;
        while (i < k && heap.size() > 0) {
            Pair<Float, String> synonym = heap.pollFirst();
            boolean banned = banSet.contains(synonym.last());
            if (!banned) {
                syns.add(new FastTextSynonym(synonym.last(), synonym.first()));
                i++;
            }
        }

        return syns;
    }

	public void saveModel() throws IOException {
		if (Utils.isEmpty(args_.output)) {
			if (args_.verbose > 1) {
				System.out.println("output is empty, skip save model file");
			}
			return;
		}

		File file = new File(args_.output + ".bin");

		if (file.exists()) file.delete();
		if (file.getParentFile() != null) file.getParentFile().mkdirs();

		if (args_.verbose > 1) {
			System.out.println("Saving model to " + file.getCanonicalPath().toString());
		}

		try (OutputStream ofs = new BufferedOutputStream(new FileOutputStream(file))){
			IOUtil ioutil = new IOUtil();
			ofs.write(ioutil.intToByteArray(FASTTEXT_FILEFORMAT_MAGIC_INT));
			ofs.write(ioutil.intToByteArray(FASTTEXT_VERSION));

			args_.save(ofs);
			dict_.save(ofs);

			ofs.write(ioutil.booleanToByteArray(false));
			input_.save(ofs);

			ofs.write(ioutil.booleanToByteArray(false));
			output_.save(ofs);
		}
	}

	public void loadModel(String filename) throws IOException {
		File file = new File(filename);

		if (!(file.exists() && file.isFile() && file.canRead())) {
			throw new IOException("Model file cannot be opened for loading!");
		}

		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
			IOUtil ioUtil = new IOUtil();

			logger.info("Checking model");
			int magic = ioUtil.readInt(dis);
			int version = ioUtil.readInt(dis);
			checkModel(magic, version);

			logger.info("Loading model arguments");
			args_ = new Args();
			args_.load(dis);
			if (version == 11) {
				// old supervised models do not use char ngrams.
				if (args_.model == Args.ModelType.sup) {
					args_.maxn = 0;
				}

				// TODO: max
				// use max vocabulary size as word2intSize.
				//args_.setUseMaxVocabularySize(true);
			}

			logger.info("Loading dictionary");
			dict_ = new Dictionary(args_);
			dict_.load(dis);

			logger.info("Loading input matrix");
			boolean qin = ioUtil.readBool(dis);

			if (!qin && dict_.isPruned()) {
				throw new IllegalArgumentException(
						"Invalid model file.\n Please download the updated model from www.fasttext.cc.\n");
			}

			input_ = qin ? new MatrixQ() : new Matrix();
			input_.load(dis);

			logger.info("Loading output matrix");
			boolean qout = ioUtil.readBool(dis);
			// args.setQOut(qout);
			// TODO

			output_ = qout ? new MatrixQ() : new Matrix();
			output_.load(dis);

			logger.info("Initiating model");
			Dictionary.EntryType entryType = args_.model == Args.ModelType.sup ? Dictionary.EntryType.label : Dictionary.EntryType.word;
			model_ = new Model(input_, output_, qin, args_, 0);
			model_.setTargetCounts(dict_.countType(entryType));
		}
	}

	public void train() throws Exception {
		dict_ = new Dictionary(args_);
		dict_.setCharsetName(charsetName_);
		dict_.setLineReaderClass(lineReaderClass_);

		if ("-".equals(args_.input)) {
			throw new IOException("Cannot use stdin for training!");
		}

		File file = new File(args_.input);
		boolean ex = file.exists();
		boolean isf = file.isFile();
		boolean rea = file.canRead();

		if (!(ex && isf && rea)) {
			throw new IOException("Input file cannot be opened! " + args_.input);
		}

		dict_.readFromFile(args_.input);
		threadFileSize = Utils.sizeLine(args_.input);

		if (!Utils.isEmpty(args_.pretrainedVectors)) {
			loadVecFile();
		} else {
			input_ = new Matrix(dict_.nwords() + args_.bucket, args_.dim);
			input_.uniform(1.0f / args_.dim);
		}

		if (args_.model == Args.ModelType.sup) {
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
			Thread t = new TrainThread(this, i);
			t.setUncaughtExceptionHandler(trainThreadExcpetionHandler);
			t.start();
		}

		synchronized (this) {
			while (threadCount > 0) {
				try {
					wait();
				} catch (InterruptedException ignored) {
				}
			}
		}

		model_ = new Model(input_, output_, false, args_, 0);

		if (args_.verbose > 1) {
			long trainTime = (System.currentTimeMillis() - t0) / 1000;
			System.out.printf("\nTrain time used: %d sec\n", trainTime);
		}

		saveModel();
		if (args_.model != Args.ModelType.sup) {
			saveVecFile();
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

	void cbow(Model model, float lr, final List<Integer> line) {
		List<Integer> bow = new ArrayList<Integer>();
		for (int w = 0; w < line.size(); w++) {
			bow.clear();

			int boundary = Utils.randomInt(model.rng, 1, args_.ws);
			for (int c = -boundary; c <= boundary; c++) {
				if (c != 0 && w + c >= 0 && w + c < line.size()) {
					final List<Integer> ngrams = dict_.getNgrams(line.get(w + c));
					bow.addAll(ngrams);
				}
			}

			model.update(bow, line.get(w), lr);
		}
	}

	void skipgram(Model model, float lr, final List<Integer> line) {
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

	void supervised(Model model, float lr, final List<Integer> line, final List<Integer> labels) {
		if (labels.size() == 0 || line.size() == 0)
			return;

		int i = Utils.randomInt(model.rng, 1, labels.size()) - 1;

		model.update(line, labels.get(i), lr);
	}

	void checkModel(int magic, int version) {
		if (magic != FASTTEXT_FILEFORMAT_MAGIC_INT) {
			throw new IllegalArgumentException("Unhandled file format");
		}

		if (version > FASTTEXT_VERSION) {
			throw new IllegalArgumentException(
					"Input model version (" + version + ") doesn't match current version (" + FASTTEXT_VERSION + ")");
		}
	}


	public void loadVecFile() throws IOException {
		loadVecFile(args_.pretrainedVectors);
	}

	public void loadVecFile(String path) throws IOException {
		try (BufferedReader dis = new BufferedReader(
				new InputStreamReader(new FileInputStream(path), "UTF-8"))) {

			String header = dis.readLine();
			String[] headerParts = header.split(" ");

			int vecsCount = Integer.parseInt(headerParts[0]);
			int dim = Integer.parseInt(headerParts[1]);

			if (dim != args_.dim) {
				throw new IllegalArgumentException("Dimension of pretrained vectors does not match args dim");
			}

			List<String> words = new ArrayList<String>(vecsCount);
			Matrix mat = new Matrix(vecsCount, dim);

			for (int i = 0; i < vecsCount; i++) {
				String line = dis.readLine();
				String[] lineParts = line.split(" ");
				String word = lineParts[0];

				for (int j = 1; j <= dim; j++) {
					mat.data[i][j - 1] = Float.parseFloat(lineParts[j]);
				}

				words.add(word);
				dict_.add(word);
			}

			dict_.threshold(1, 0);

			input_ = new Matrix(dict_.nwords() + args_.bucket, args_.dim);
			input_.uniform(1.0f / args_.dim);

			for (int i = 0; i < vecsCount; i++) {
				int idx = dict_.getId(words.get(i));
				if (idx < 0 || idx >= dict_.nwords())
					continue;

				for (int j = 0; j < dim; j++) {
					input_.data[idx][j] = mat.data[i][j];
				}
			}
		}
	}

	public void saveVecFile() throws IOException {
		saveVecFile(args_.output + ".vec");
	}

	public void saveVecFile(String path) throws IOException {
		File file = new File(path);

		if (file.exists()) file.delete();
		if (file.getParentFile() != null) file.getParentFile().mkdirs();

		if (args_.verbose > 1) {
			System.out.println("Saving Vectors to " + file.getCanonicalPath().toString());
		}

		try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file)), "UTF-8")) {
			writer.write(dict_.nwords() + " " + args_.dim + "\n");

			DecimalFormat df = new DecimalFormat("0.#####");

			for (int i = 0; i < dict_.nwords(); i++) {
				String word = dict_.getWord(i);
				writer.write(word);

				Vector vec = getWordVectorIn(word);
				for (int j = 0; j < vec.m; j++) {
					writer.write(" ");
					writer.write(df.format(vec.data[j]));
				}

				writer.write("\n");
			}
		}
	}



	Thread.UncaughtExceptionHandler trainThreadExcpetionHandler = new Thread.UncaughtExceptionHandler() {
		public void uncaughtException(Thread th, Throwable ex) {
			ex.printStackTrace();
		}
	};




    public static class HeapComparator<T> implements Comparator<Pair<Float, T>> {
        @Override
        public int compare(Pair<Float, T> p1, Pair<Float, T> p2) {
            if (p1.first().equals(p2.first())) {
                return 0;
            } else if (p1.first() < p2.first()) {
                return 1;
            } else {
                return -1;
            }
        }
    }

	public class TrainThread extends Thread {
		final FastText ft;
		int threadId;

		public TrainThread(FastText ft, int threadId) {
			super("FT-TrainThread-" + threadId);
			this.ft = ft;
			this.threadId = threadId;
		}

		public void run() {
			if (args_.verbose > 2) {
				System.out.println("thread: " + threadId + " RUNNING!");
			}

			Exception catchedException = null;
			LineReader lineReader = null;
			try {
				lineReader = lineReaderClass_.getConstructor(
						String.class, String.class).newInstance(args_.input, charsetName_);

				lineReader.skipLine(threadId * threadFileSize / args_.thread);
				Model model = new Model(input_, output_, false, args_, threadId);
				if (args_.model == Args.ModelType.sup) {
					model.setTargetCounts(dict_.countType(Dictionary.EntryType.label));
				} else {
					model.setTargetCounts(dict_.countType(Dictionary.EntryType.word));
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
					if (args_.model == Args.ModelType.sup) {
						dict_.addNgrams(line, args_.wordNgrams);
						if (labels.size() == 0 || line.size() == 0) {
							continue;
						}
						supervised(model, lr, line, labels);
					} else if (args_.model == Args.ModelType.cbow) {
						cbow(model, lr, line);
					} else if (args_.model == Args.ModelType.sg) {
						skipgram(model, lr, line);
					}
					if (localTokenCount > args_.lrUpdateRate) {
						tokenCount_.addAndGet(localTokenCount);
						localTokenCount = 0;
						if (threadId == 0 && args_.verbose > 1 && (System.currentTimeMillis() - start_) % 1000 == 0) {
							printInfo(progress, model.getLoss());
						}
					}
				}

				if (threadId == 0 && args_.verbose > 1) {
					printInfo(1.0f, model.getLoss());
				}
			} catch (Exception e) {
				catchedException = e;
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

				if (catchedException != null) {
					throw new RuntimeException(catchedException);
				}
			}
		}

		private void printInfo(float progress, float loss) {
			float t = (float) (System.currentTimeMillis() - start_) / 1000;
			float ws = (float) (tokenCount_.get()) / t;
			float wst = (float) (tokenCount_.get()) / t / args_.thread;
			float lr = (float) (args_.lr * (1.0f - progress));
			int eta = (int) (t / progress * (1 - progress));
			int etah = eta / 3600;
			int etam = (eta - etah * 3600) / 60;
			System.out.printf("\rProgress: %.1f%% words/sec: %d words/sec/thread: %d lr: %.6f loss: %.6f eta: %d h %d m",
					100 * progress, (int) ws, (int) wst, lr, loss, etah, etam);
		}
	}
}
