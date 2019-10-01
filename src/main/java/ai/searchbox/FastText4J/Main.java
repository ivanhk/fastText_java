package ai.searchbox.FastText4J;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Main {

	public static void printUsage() {
		System.out.print("usage: java -jar fasttext.jar <command> <args>\n\n"
				+ "The commands supported by fasttext are:\n\n"
				+ "  supervised          train a supervised classifier\n"
				+ "  skipgram            train a skipgram model\n"
				+ "  cbow                train a cbow model\n"
				+ "  predict             predict most likely labels\n"
				+ "  predict-prob        predict most likely labels with probabilities\n"
				+ "  test                evaluate a supervised classifier\n");
	}

	public static void printPredictUsage() {
		System.out.print("usage: java -jar fasttext.jar predict[-prob] <model> <test-data> [<k>]\n\n"
				+ "  <model>      model filename\n"
				+ "  <test-data>  test data filename (if -, read from stdin)\n"
				+ "  <k>          (optional; 1 by default) predict top k labels\n");
	}

	public static void printTestUsage() {
		System.out.print("usage: java -jar fasttext.jar test <model> <test-data> [<k>]\n\n"
				+ "  <model>      model filename\n" 
				+ "  <test-data>  test data filename (if -, read from stdin)\n"
				+ "  <k>          (optional; 1 by default) predict top k labels\n");
	}

	private void predict(String[] args) throws Exception {
		/*
		int k = 1;
		if (args.length == 3) {
			k = 1;
		} else if (args.length == 4) {
			k = Integer.parseInt(args[3]);
		} else {
			printPredictUsage();
			System.exit(1);
		}

		boolean print_prob = "predict-prob".equalsIgnoreCase(args[0]);

		FastText fasttext = new FastText();
		fasttext.loadModel(args[1]);

		String infile = args[2];
		if ("-".equals(infile)) {
			fasttext.predict(System.in, k);
		} else {
			File file = new File(infile);
			if (!(file.exists() && file.isFile() && file.canRead())) {
				throw new IOException("Input file cannot be opened!");
			}
			fasttext.predict(new FileInputStream(file), k);
		}

		 */
	}

	private void train(String[] args) throws Exception {
		Args a = new Args();
		a.parseArgs(args);

		FastText fasttext = new FastText();
		fasttext.setArgs(a);
		fasttext.train();
	}

	private void test(String[] args) throws Exception {
		int k = 1;
		if (args.length == 3) {
			k = 1;
		} else if (args.length == 4) {
			k = Integer.parseInt(args[3]);
		} else {
			printTestUsage();
			System.exit(1);
		}

		FastText fasttext = new FastText();
		fasttext.loadModel(args[1]);
		String infile = args[2];
		if ("-".equals(infile)) {
			fasttext.test(System.in, k);
		} else {
			File file = new File(infile);
			if (!(file.exists() && file.isFile() && file.canRead())) {
				throw new IOException("Test file cannot be opened!");
			}
			fasttext.test(new FileInputStream(file), k);
		}
	}

	public static void main(String[] args) {
		args = new String[]{"skipgram",
				"-input", "/Users/davidgortega/Projects/tmp/fastText-0.9.1/data/file9short",
				"-output", "/Users/davidgortega/Projects/tmp/fastText-0.9.1/result/fil9Java",
				"-thread", "8"};

		Main op = new Main();

		if (args.length == 0) {
			printUsage();
			System.exit(1);
		}

		try {
			String command = args[0];
			if ("predict".equalsIgnoreCase(command) || "predict-prob".equalsIgnoreCase(command)) {
				op.predict(args);
			}else if ("skipgram".equalsIgnoreCase(command)
					|| "cbow".equalsIgnoreCase(command)
					|| "supervised".equalsIgnoreCase(command)) {
				op.train(args);
			} else if ("test".equalsIgnoreCase(command)) {
				op.test(args);
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
