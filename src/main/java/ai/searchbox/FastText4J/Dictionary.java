package ai.searchbox.FastText4J;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.math.BigInteger;

import ai.searchbox.FastText4J.Args.ModelType;
import ai.searchbox.FastText4J.io.BufferedLineReader;
import ai.searchbox.FastText4J.io.IOUtil;
import ai.searchbox.FastText4J.io.LineReader;

public class Dictionary {

	private static final int MAX_VOCAB_SIZE = 30000000;
	private static final int MAX_LINE_SIZE = 1024;
	private static final Integer WORDID_DEFAULT = -1;

	private static final String EOS = "</s>";
	private static final String BOW = "<";
	private static final String EOW = ">";

	private int size = 0;
	private int nwords = 0;
	private long ntokens = 0;
	private int nlabels = 0;

	protected long pruneIdxSize = -1;
	Map<Integer, Integer> pruneIdx;

	private List<Entry> words;
	private Map<Long, Integer> word2int;
	private List<Float> pdiscard;

	private String charsetName_ = "UTF-8";
	private Class<? extends LineReader> lineReaderClass_ = BufferedLineReader.class;

	private Args args_;

	public Dictionary(Args args) {
		args_ = args;

		words = new ArrayList<Entry>(MAX_VOCAB_SIZE);
		word2int = new HashMap<Long, Integer>(MAX_VOCAB_SIZE);
	}

	public List<Entry> getWords() {
		return words;
	}

	public int nwords() {
		return nwords;
	}

	public int nlabels() {
		return nlabels;
	}

	public long ntokens() {
		return ntokens;
	}

	public Map<Long, Integer> getWord2int() {
		return word2int;
	}

	public List<Float> getPdiscard() {
		return pdiscard;
	}

	public int getSize() {
		return size;
	}

	public Args getArgs() {
		return args_;
	}

	public String getCharsetName() {
		return charsetName_;
	}

	public void setCharsetName(String charsetName) {
		this.charsetName_ = charsetName;
	}

	public Class<? extends LineReader> getLineReaderClass() {
		return lineReaderClass_;
	}

	public void setLineReaderClass(Class<? extends LineReader> lineReaderClass) {
		this.lineReaderClass_ = lineReaderClass;
	}

	public boolean isPruned() { return pruneIdxSize >= 0; }



	public long find(final String w) {
		long h = hash(w) % MAX_VOCAB_SIZE;
		Entry e = null;
		while (Utils.mapGetOrDefault(word2int, h, WORDID_DEFAULT) != WORDID_DEFAULT
				&& ((e = words.get(word2int.get(h))) != null
				&& !w.equals(e.word))) {
			h = (h + 1) % MAX_VOCAB_SIZE;
		}

		return h;
	}

	public void add(final String w) {
		long h = find(w);

		if (Utils.mapGetOrDefault(word2int, h, WORDID_DEFAULT) == WORDID_DEFAULT) {
			Entry e = new Entry();
			e.word = w;
			e.count = 1;
			e.type = w.startsWith(args_.label) ? EntryType.label : EntryType.word;
			words.add(e);
			word2int.put(h, size++);

		} else {
			words.get(word2int.get(h)).count++;
		}

		ntokens++;
	}

	public final List<Integer> getNgrams(final String word) {
		int id = getId(word);

		if (id > WORDID_DEFAULT) return getNgrams(id);

		return computeNgrams(BOW + word + EOW);
	}

	public final List<Integer> getNgrams(int i) {
		Utils.checkArgument(i >= 0);
		Utils.checkArgument(i < nwords);

		return words.get(i).subwords;
	}

	public void addNgrams(List<Integer> line, int n) {
		Utils.checkArgument(n > 0);

		int line_size = line.size();
		for (int i = 0; i < line_size; i++) {
			BigInteger h = BigInteger.valueOf(line.get(i));
			BigInteger r = BigInteger.valueOf(116049371l);
			BigInteger b = BigInteger.valueOf(args_.bucket);

			for (int j = i + 1; j < line_size && j < i + n; j++) {
				h = h.multiply(r).add(BigInteger.valueOf(line.get(j)));;
				line.add(nwords + h.remainder(b).intValue());
			}
		}
	}

	public int getLine(String[] tokens, List<Integer> words, List<Integer> labels, Random urd) {
		words.clear();
		labels.clear();

		int ntokens = 0;

		if (tokens != null) {
			for (int i = 0; i <= tokens.length; i++) {
				if (i < tokens.length && Utils.isEmpty(tokens[i])) {
					continue;
				}

				int wid = i == tokens.length ? getId(EOS) : getId(tokens[i]);
				if (wid < 0) {
					continue;
				}

				ntokens++;

				EntryType type = getType(wid);

				if (type == EntryType.word && !discard(wid, Utils.randomFloat(urd, 0, 1))) {
					words.add(wid);
				}

				if (type == EntryType.label) {
					labels.add(wid - nwords);
				}

				if (words.size() > MAX_LINE_SIZE && args_.model != ModelType.sup) {
					break;
				}
				// if (EOS == tokens[i]){
				// break;
				// }
			}
		}

		return ntokens;
	}

	public int getId(final String w) {
		long h = find(w);
		return Utils.mapGetOrDefault(word2int, h, WORDID_DEFAULT);
	}

	public EntryType getType(int id) {
		Utils.checkArgument(id >= 0);
		Utils.checkArgument(id < size);

		return words.get(id).type;
	}

	public String getWord(int id) {
		Utils.checkArgument(id >= 0);
		Utils.checkArgument(id < size);

		return words.get(id).word;
	}

	public String getLabel(int lid) {
		Utils.checkArgument(lid >= 0);
		Utils.checkArgument(lid < nlabels);
		return words.get(lid + nwords).word;
	}

	// TODO: count can be stored as var
	public List<Long> countType(EntryType type) {
		int size = (EntryType.label == type ) ? nlabels() : nwords();
		List<Long> counts = new ArrayList<Long>(size);

		for (Entry w : words) {
			if (w.type == type) counts.add(w.count);
		}

		return counts;
	}

	public void readFromFile(String file) throws Exception {
		LineReader lineReader = null;

		try {
			lineReader = lineReaderClass_
					.getConstructor(String.class, String.class)
					.newInstance(file, charsetName_);

			long minThreshold = 1;

			String[] lineTokens;
			while ((lineTokens = lineReader.readLineTokens()) != null) {
				for (int i = 0; i <= lineTokens.length; i++) {
					if (i == lineTokens.length) {
						add(EOS);
					} else {
						if (Utils.isEmpty(lineTokens[i])) {
							continue;
						}
						add(lineTokens[i]);
					}

					if (ntokens % 1000000 == 0 && args_.verbose > 1) {
						System.out.printf("\rRead %dM words", ntokens / 1000000);
					}

					if (size > 0.75 * MAX_VOCAB_SIZE) {
						minThreshold++;
						threshold(minThreshold, minThreshold);
					}
				}
			}
		} finally {
			if (lineReader != null) {
				lineReader.close();
			}
		}

		threshold(args_.minCount, args_.minCountLabel);
		initTableDiscard();

		if (ModelType.cbow == args_.model || ModelType.sg == args_.model) {
			initNgrams();
		}

		if (args_.verbose > 0) {
			System.out.printf("\rRead %dM words\n", ntokens / 1000000);
			System.out.println("Number of words:  " + nwords);
			System.out.println("Number of labels: " + nlabels);
		}

		if (size == 0) {
			//TODO: throw exception
			System.err.println("Empty vocabulary. Try a smaller -minCount value.");
			System.exit(1);
		}
	}


	public void load(InputStream is) throws IOException {
		IOUtil io = new IOUtil();
		size = io.readInt(is);
		nwords = io.readInt(is);
		nlabels = io.readInt(is);
		ntokens = io.readLong(is);
		pruneIdxSize = io.readLong(is);

		words = new ArrayList<Entry>(size);
		word2int = new HashMap<Long, Integer>(size);

		for (int i = 0; i < size; i++) {
			Entry e = new Entry();
			e.word = io.readString(is);
			e.count = io.readLong(is);
			e.type = EntryType.fromValue(io.readByteAsInt(is));
			words.add(e);

			word2int.put(find(e.word), i);
		}

		pruneIdx = new HashMap<>((int)Math.max(0, pruneIdxSize));
		if (pruneIdxSize > 0) {
			for (int i = 0; i < pruneIdxSize; i++) {
				int first = io.readInt(is);
				int second = io.readInt(is);
				pruneIdx.put(first, second);
			}
		}

		initTableDiscard();
		initNgrams();
	}

	public void save(OutputStream ofs) throws IOException {
		IOUtil ioutil = new IOUtil();
		ofs.write(ioutil.intToByteArray(size));
		ofs.write(ioutil.intToByteArray(nwords));
		ofs.write(ioutil.intToByteArray(nlabels));
		ofs.write(ioutil.longToByteArray(ntokens));
 		ofs.write(ioutil.longToByteArray(pruneIdxSize));

		for (int i = 0; i < size; i++) {
			Entry e = words.get(i);
			ofs.write(e.word.getBytes());
			ofs.write(0);
			ofs.write(ioutil.longToByteArray(e.count));
			ofs.write(ioutil.intToByte(e.type.value));
		}
	}

	public void threshold(long t, long tl) {
		Collections.sort(words, entry_comparator);
		Iterator<Entry> iterator = words.iterator();
		while (iterator.hasNext()) {
			Entry _entry = iterator.next();
			if ((EntryType.word == _entry.type && _entry.count < t)
					|| (EntryType.label == _entry.type && _entry.count < tl)) {
				iterator.remove();
			}
		}

		((ArrayList<Entry>) words).trimToSize();
		size = 0;
		nwords = 0;
		nlabels = 0;
		word2int = new HashMap<Long, Integer>(words.size());
		for (Entry _entry : words) {
			long h = find(_entry.word);
			word2int.put(h, size++);

			if (EntryType.word == _entry.type) {
				nwords++;
			} else {
				nlabels++;
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Dictionary [words_=");
		builder.append(words);
		builder.append(", pdiscard_=");
		builder.append(pdiscard);
		builder.append(", word2int_=");
		builder.append(word2int);
		builder.append(", size_=");
		builder.append(size);
		builder.append(", nwords_=");
		builder.append(nwords);
		builder.append(", nlabels_=");
		builder.append(nlabels);
		builder.append(", ntokens_=");
		builder.append(ntokens);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * String FNV-1a Hash
	 *
	 * @param str
	 * @return
	 */
	public long hash(final String str) {
		int h = (int) 2166136261L;// 0xffffffc5;
		for (byte strByte : str.getBytes()) {
			h = (h ^ strByte) * 16777619; // FNV-1a
			// h = (h * 16777619) ^ strByte; //FNV-1
		}
		return h & 0xffffffffL;
	}

	private void initTableDiscard() {
		pdiscard = new ArrayList<Float>(size);
		for (int i = 0; i < size; i++) {
			float f = (float) (words.get(i).count) / (float) ntokens;
			pdiscard.add((float) (Math.sqrt(args_.t / f) + args_.t / f));
		}
	}

	private void initNgrams() {
		for (int i = 0; i < size; i++) {
			String word = BOW + words.get(i).word + EOW;

			Entry e = words.get(i);
			e.subwords = computeNgrams(word);
			e.subwords.add(i);
		}
	}

	private boolean charMatches(char ch) {
		return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\f' || ch == '\r';
	}

	private boolean discard(int id, float rand) {
		Utils.checkArgument(id >= 0);
		Utils.checkArgument(id < nwords);

		return  args_.model == ModelType.sup ? false
				: rand > pdiscard.get(id);
	}

	private List<Integer> computeNgrams(final String word) {
		List<Integer> ngrams = new ArrayList<Integer>();

		for (int i = 0; i < word.length(); i++) {
			StringBuilder ngram = new StringBuilder();

			if (charMatches(word.charAt(i))) {
				continue;
			}

			for (int j = i, n = 1; j < word.length() && n <= args_.maxn; n++) {
				ngram.append(word.charAt(j++));
				while (j < word.length() && charMatches(word.charAt(j))) {
					ngram.append(word.charAt(j++));
				}
				if (n >= args_.minn && !(n == 1 && (i == 0 || j == word.length()))) {
					int h = (int) (nwords + (hash(ngram.toString()) % args_.bucket));

					if (h < 0) {
						//TODO: throw exception
						System.err.println("computeNgrams h<0: " + h + " on word: " + word);
					}

					ngrams.add(h);
				}
			}
		}

		return ngrams;
	}

	private transient Comparator<Entry> entry_comparator = new Comparator<Entry>() {
		@Override
		public int compare(Entry o1, Entry o2) {
		int cmp = (o1.type.value < o2.type.value) ? -1 : ((o1.type.value == o2.type.value) ? 0 : 1);
		if (cmp == 0) {
			cmp = (o2.count < o1.count) ? -1 : ((o2.count == o1.count) ? 0 : 1);
		}

		return cmp;
		}
	};



	public enum EntryType {
		word(0), label(1);

		private int value;

		public int getValue() {
			return this.value;
		}

		public static Dictionary.EntryType fromValue(int value) throws IllegalArgumentException {
			try {
				return EntryType.values()[value];
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new IllegalArgumentException("Unknown entry_type enum value :" + value);
			}
		}

		@Override
		public String toString() {
			return value == 0 ? "word" : value == 1 ? "label" : "unknown";
		}

		private EntryType(int value) {
			this.value = value;
		}
	}

	public class Entry {
		public String word;
		public EntryType type;
		public long count;
		public List<Integer> subwords;

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("entry [word=");
			builder.append(word);
			builder.append(", count=");
			builder.append(count);
			builder.append(", type=");
			builder.append(type);
			builder.append(", subwords=");
			builder.append(subwords);
			builder.append("]");
			return builder.toString();
		}
	}
}
