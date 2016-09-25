package fasttext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Args {

	public enum model_name {
		cbow(1), sg(2), sup(3);

		private int value;

		private model_name(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}

		public static model_name fromValue(int value) throws IllegalArgumentException {
			try {
				value -= 1;
				return model_name.values()[value];
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new IllegalArgumentException("Unknown model_name enum value :" + value);
			}
		}
	}

	public enum loss_name {
		hs(1), ns(2), softmax(3);
		private int value;

		private loss_name(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}

		public static loss_name fromValue(int value) throws IllegalArgumentException {
			try {
				value -= 1;
				return loss_name.values()[value];
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new IllegalArgumentException("Unknown loss_name enum value :" + value);
			}
		}
	}

	public String input;
	public String test;
	public String output;
	public double lr = 0.05;
	public int lrUpdateRate = 100;
	public int dim = 100;
	public int ws = 5;
	public int epoch = 5;
	public int minCount = 5;
	public int neg = 5;
	public int wordNgrams = 1;
	public loss_name loss = loss_name.ns;
	public model_name model = model_name.sg;
	public int bucket = 2000000;
	public int minn = 3;
	public int maxn = 6;
	public int thread = 1;
	public double t = 1e-4;
	public String label = "__label__";

	public void load(InputStream input) throws IOException {
		dim = IOUtil.readInt(input);
		ws = IOUtil.readInt(input);
		epoch = IOUtil.readInt(input);
		minCount = IOUtil.readInt(input);
		neg = IOUtil.readInt(input);
		wordNgrams = IOUtil.readInt(input);
		loss = loss_name.fromValue(IOUtil.readInt(input));
		model = model_name.fromValue(IOUtil.readInt(input));
		bucket = IOUtil.readInt(input);
		minn = IOUtil.readInt(input);
		maxn = IOUtil.readInt(input);
		lrUpdateRate = IOUtil.readInt(input);
		t = IOUtil.readDouble(input);
	}

	public void save(OutputStream ofs) throws IOException {
		ofs.write(IOUtil.intToByteArray(dim));
		ofs.write(IOUtil.intToByteArray(ws));
		ofs.write(IOUtil.intToByteArray(epoch));
		ofs.write(IOUtil.intToByteArray(minCount));
		ofs.write(IOUtil.intToByteArray(neg));
		ofs.write(IOUtil.intToByteArray(wordNgrams));
		ofs.write(IOUtil.intToByteArray(loss.value));
		ofs.write(IOUtil.intToByteArray(model.value));
		ofs.write(IOUtil.intToByteArray(bucket));
		ofs.write(IOUtil.intToByteArray(minn));
		ofs.write(IOUtil.intToByteArray(maxn));
		ofs.write(IOUtil.intToByteArray(lrUpdateRate));
		ofs.write(IOUtil.doubleToByteArray(t));
	}

	public Options getOptions() {
		// create the Options
		Options options = new Options();
		options.addOption(Option.builder("input").desc("training file path").hasArg().required().build());
		options.addOption(Option.builder("output").desc("output file path").hasArg().required().build());
		options.addOption(Option.builder("lr").desc("learning rate[" + lr + "]").hasArg().build());
		options.addOption(Option.builder("lrUpdateRate")
				.desc("change the rate of updates for the learning rate [" + lrUpdateRate + "]").hasArg().build());
		options.addOption(Option.builder("dim").desc("size of word vectors [" + dim + "]").hasArg().build());
		options.addOption(Option.builder("ws").desc("size of the context window [" + lr + "]").hasArg().build());
		options.addOption(Option.builder("epoch").desc("number of epochs [" + epoch + "]").hasArg().build());
		options.addOption(Option.builder("minCount").desc("minimal number of word occurences [" + minCount + "]")
				.hasArg().build());
		options.addOption(
				Option.builder("neg").desc("number of negatives sampled [" + minCount + "]").hasArg().build());
		options.addOption(
				Option.builder("wordNgrams").desc("max length of word ngram [" + wordNgrams + "]").hasArg().build());
		options.addOption(Option.builder("loss").desc("loss function {ns, hs, softmax} [ns]").hasArg().build());
		options.addOption(Option.builder("bucket").desc("number of buckets [" + bucket + "]").hasArg().build());
		options.addOption(Option.builder("minn").desc("min length of char ngram [" + minn + "]").hasArg().build());
		options.addOption(Option.builder("maxn").desc("max length of char ngram [" + minn + "]").hasArg().build());
		options.addOption(Option.builder("thread").desc("number of threads [" + thread + "]").hasArg().build());
		options.addOption(Option.builder("t").desc("sampling threshold [" + t + "]").hasArg().build());
		options.addOption(Option.builder("label").desc("labels prefix [" + label + "]").hasArg().build());
		return options;
	}

	public void parseArgs(String[] args) {
		String command = args[0];
		if ("supervised".equalsIgnoreCase(command)) {
			model = model_name.sup;
			loss = loss_name.softmax;
			minCount = 1;
		} else if ("cbow".equalsIgnoreCase(command)) {
			model = model_name.cbow;
		}
		String[] fargs = new String[args.length - 1];
		System.arraycopy(args, 1, fargs, 0, args.length - 1);

		// create the command line parser
		CommandLineParser parser = new DefaultParser();
		Options options = getOptions();
		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, fargs);
			input = line.getOptionValue("input");
			output = line.getOptionValue("output");
			if (line.hasOption("lr")) {
				lr = Double.parseDouble(line.getOptionValue("lr"));
			}
			if (line.hasOption("lrUpdateRate")) {
				lrUpdateRate = Integer.parseInt(line.getOptionValue("lrUpdateRate"));
			}
			if (line.hasOption("dim")) {
				dim = Integer.parseInt(line.getOptionValue("dim"));
			}
			if (line.hasOption("ws")) {
				ws = Integer.parseInt(line.getOptionValue("ws"));
			}
			if (line.hasOption("epoch")) {
				epoch = Integer.parseInt(line.getOptionValue("epoch"));
			}
			if (line.hasOption("minCount")) {
				minCount = Integer.parseInt(line.getOptionValue("minCount"));
			}
			if (line.hasOption("neg")) {
				neg = Integer.parseInt(line.getOptionValue("neg"));
			}
			if (line.hasOption("wordNgrams")) {
				wordNgrams = Integer.parseInt(line.getOptionValue("wordNgrams"));
			}
			if (line.hasOption("loss")) {
				String lossName = line.getOptionValue("loss");
				if ("ns".equalsIgnoreCase(lossName)) {
					loss = loss_name.ns;
				} else if ("hs".equalsIgnoreCase(lossName)) {
					loss = loss_name.hs;
				} else if ("softmax".equalsIgnoreCase(lossName)) {
					loss = loss_name.softmax;
				}
			}
			if (line.hasOption("bucket")) {
				bucket = Integer.parseInt(line.getOptionValue("bucket"));
			}
			if (line.hasOption("minn")) {
				minn = Integer.parseInt(line.getOptionValue("minn"));
			}
			if (line.hasOption("maxn")) {
				maxn = Integer.parseInt(line.getOptionValue("maxn"));
			}
			if (line.hasOption("thread")) {
				thread = Integer.parseInt(line.getOptionValue("thread"));
			}
			if (line.hasOption("t")) {
				t = Double.parseDouble(line.getOptionValue("t"));
			}
			if (line.hasOption("label")) {
				label = line.getOptionValue("label");
			}
		} catch (ParseException exp) {
			System.out.println("Unexpected exception:" + exp.getMessage());
			printHelp(options);
			System.exit(1);
		}
	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("FastText", options);
	}

	// void Args::printHelp() {
	// std::cout
	// << "\n"
	// << "The following arguments are mandatory:\n"
	// << " -input training file path\n"
	// << " -output output file path\n\n"
	// << "The following arguments are optional:\n"
	// << " -lr learning rate [" << lr << "]\n"
	// << " -lrUpdateRate change the rate of updates for the learning rate [" <<
	// lrUpdateRate << "]\n"
	// << " -dim size of word vectors [" << dim << "]\n"
	// << " -ws size of the context window [" << ws << "]\n"
	// << " -epoch number of epochs [" << epoch << "]\n"
	// << " -minCount minimal number of word occurences [" << minCount << "]\n"
	// << " -neg number of negatives sampled [" << neg << "]\n"
	// << " -wordNgrams max length of word ngram [" << wordNgrams << "]\n"
	// << " -loss loss function {ns, hs, softmax} [ns]\n"
	// << " -bucket number of buckets [" << bucket << "]\n"
	// << " -minn min length of char ngram [" << minn << "]\n"
	// << " -maxn max length of char ngram [" << maxn << "]\n"
	// << " -thread number of threads [" << thread << "]\n"
	// << " -t sampling threshold [" << t << "]\n"
	// << " -label labels prefix [" << label << "]\n"
	// << std::endl;
	// }

}
