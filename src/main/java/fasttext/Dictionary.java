package fasttext;

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

import fasttext.Args.model_name;
import fasttext.io.BufferedLineReader;
import fasttext.io.LineReader;

public class Dictionary {

	private static final int MAX_VOCAB_SIZE = 30000000;
	private static final int MAX_LINE_SIZE = 1024;
	private static final Integer WORDID_DEFAULT = -1;

	private static final String EOS = "</s>";
	private static final String BOW = "<";
	private static final String EOW = ">";

	public enum entry_type {
		word(0), label(1);

		private int value;

		private entry_type(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}

		public static entry_type fromValue(int value) throws IllegalArgumentException {
			try {
				return entry_type.values()[value];
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new IllegalArgumentException("Unknown entry_type enum value :" + value);
			}
		}

		@Override
		public String toString() {
			return value == 0 ? "word" : value == 1 ? "label" : "unknown";
		}
	}

	public class entry {
		String word;
		long count;
		entry_type type;
		List<Integer> subwords;

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

	private List<entry> words_;
	private List<Float> pdiscard_;
	private Map<Long, Integer> word2int_;
	private int size_;
	private int nwords_;
	private int nlabels_;
	private long ntokens_;

	private Args args_;

	private String charsetName = "UTF-8";
	private Class<? extends LineReader> lineReaderClass = BufferedLineReader.class;

	public Dictionary(Args args) {
		args_ = args;
		size_ = 0;
		nwords_ = 0;
		nlabels_ = 0;
		ntokens_ = 0;
		word2int_ = new HashMap<Long, Integer>(MAX_VOCAB_SIZE);
		words_ = new ArrayList<entry>(MAX_VOCAB_SIZE);
	}

	public long find(final String w) {
		long h = hash(w) % MAX_VOCAB_SIZE;
		entry e = null;
		while (Utils.mapGetOrDefault(word2int_, h, WORDID_DEFAULT) != WORDID_DEFAULT
				&& ((e = words_.get(word2int_.get(h))) != null && !w.equals(e.word))) {
			h = (h + 1) % MAX_VOCAB_SIZE;
		}
		return h;
	}

	public void add(final String w) {
		long h = find(w);
		ntokens_++;
		if (Utils.mapGetOrDefault(word2int_, h, WORDID_DEFAULT) == WORDID_DEFAULT) {
			entry e = new entry();
			e.word = w;
			e.count = 1;
			e.type = w.contains(args_.label) ? entry_type.label : entry_type.word;
			words_.add(e);
			word2int_.put(h, size_++);
		} else {
			words_.get(word2int_.get(h)).count++;
		}
	}

	public int nwords() {
		return nwords_;
	}

	public int nlabels() {
		return nlabels_;
	}

	public long ntokens() {
		return ntokens_;
	}

	public final List<Integer> getNgrams(int i) {
		Utils.checkArgument(i >= 0);
		Utils.checkArgument(i < nwords_);
		return words_.get(i).subwords;
	}

	public final List<Integer> getNgrams(final String word) {
		List<Integer> ngrams = new ArrayList<Integer>();
		int i = getId(word);
		if (i >= 0) {
			ngrams = words_.get(i).subwords;
		} else {
			computeNgrams(BOW + word + EOW, ngrams);
		}
		return ngrams;
	}

	public boolean discard(int id, float rand) {
		Utils.checkArgument(id >= 0);
		Utils.checkArgument(id < nwords_);
		if (args_.model == model_name.sup)
			return false;
		return rand > pdiscard_.get(id);
	}

	public int getId(final String w) {
		long h = find(w);
		return Utils.mapGetOrDefault(word2int_, h, WORDID_DEFAULT);
	}

	public entry_type getType(int id) {
		Utils.checkArgument(id >= 0);
		Utils.checkArgument(id < size_);
		return words_.get(id).type;
	}

	public String getWord(int id) {
		Utils.checkArgument(id >= 0);
		Utils.checkArgument(id < size_);
		return words_.get(id).word;
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

	public void computeNgrams(final String word, List<Integer> ngrams) {
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
					int h = (int) (nwords_ + (hash(ngram.toString()) % args_.bucket));
					if (h < 0) {
						System.err.println("computeNgrams h<0: " + h + " on word: " + word);
					}
					ngrams.add(h);
				}
			}
		}
	}

	private boolean charMatches(char ch) {
		if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\f' || ch == '\r') {
			return true;
		}
		return false;
	}

	public void initNgrams() {
		for (int i = 0; i < size_; i++) {
			String word = BOW + words_.get(i).word + EOW;
			entry e = words_.get(i);
			if (e.subwords == null) {
				e.subwords = new ArrayList<Integer>();
			}
			e.subwords.add(i);
			computeNgrams(word, e.subwords);
		}
	}

	public void readFromFile(String file) throws IOException, Exception {
		LineReader lineReader = null;

		try {
			lineReader = lineReaderClass.getConstructor(String.class, String.class).newInstance(file, charsetName);
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
					if (ntokens_ % 1000000 == 0 && args_.verbose > 1) {
						System.out.printf("\rRead %dM words", ntokens_ / 1000000);
					}
					if (size_ > 0.75 * MAX_VOCAB_SIZE) {
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
		if (model_name.cbow == args_.model || model_name.sg == args_.model) {
			initNgrams();
		}
		if (args_.verbose > 0) {
			System.out.printf("\rRead %dM words\n", ntokens_ / 1000000);
			System.out.println("Number of words:  " + nwords_);
			System.out.println("Number of labels: " + nlabels_);
		}
		if (size_ == 0) {
			System.err.println("Empty vocabulary. Try a smaller -minCount value.");
			System.exit(1);
		}
	}

	public void threshold(long t, long tl) {
		Collections.sort(words_, entry_comparator);
		Iterator<entry> iterator = words_.iterator();
		while (iterator.hasNext()) {
			entry _entry = iterator.next();
			if ((entry_type.word == _entry.type && _entry.count < t)
					|| (entry_type.label == _entry.type && _entry.count < tl)) {
				iterator.remove();
			}
		}
		((ArrayList<entry>) words_).trimToSize();
		size_ = 0;
		nwords_ = 0;
		nlabels_ = 0;
		// word2int_.clear();
		word2int_ = new HashMap<Long, Integer>(words_.size());
		for (entry _entry : words_) {
			long h = find(_entry.word);
			word2int_.put(h, size_++);
			if (entry_type.word == _entry.type) {
				nwords_++;
			}
			if (entry_type.label == _entry.type) {
				nlabels_++;
			}
		}
	}

	private transient Comparator<entry> entry_comparator = new Comparator<entry>() {
		@Override
		public int compare(entry o1, entry o2) {
			int cmp = o1.type.value > o2.type.value ? +1 : o1.type.value < o2.type.value ? -1 : 0;
			if (cmp == 0) {
				cmp = o2.count > o1.count ? +1 : o2.count < o1.count ? -1 : 0;
			}
			return cmp;
		}
	};

	public void initTableDiscard() {
		pdiscard_ = new ArrayList<Float>(size_);
		for (int i = 0; i < size_; i++) {
			float f = (float) (words_.get(i).count) / (float) ntokens_;
			pdiscard_.add((float) (Math.sqrt(args_.t / f) + args_.t / f));
		}
	}

	public List<Long> getCounts(entry_type type) {
		List<Long> counts = entry_type.label == type ? new ArrayList<Long>(nlabels()) : new ArrayList<Long>(nwords());
		for (entry w : words_) {
			if (w.type == type)
				counts.add(w.count);
		}
		return counts;
	}

	public void addNgrams(List<Integer> line, int n) {
		int line_size = line.size();
		for (int i = 0; i < line_size; i++) {
			long h = (long) line.get(i);
			for (int j = i + 1; j < line_size && j < i + n; j++) {
				h = (h * 116049371l + line.get(j)) & 0xffffffffl;
				line.add(nwords_ + (int) (h % args_.bucket));
			}
		}
	}

	public int getLine(String[] tokens, List<Integer> words, List<Integer> labels, Random urd) {
		int ntokens = 0;
		words.clear();
		labels.clear();
		if (tokens != null) {
			for (int i = 0; i <= tokens.length; i++) {
				if (i < tokens.length && Utils.isEmpty(tokens[i])) {
					continue;
				}
				int wid = i == tokens.length ? getId(EOS) : getId(tokens[i]);
				if (wid < 0) {
					continue;
				}
				entry_type type = getType(wid);
				ntokens++;
				if (type == entry_type.word && !discard(wid, Utils.randomFloat(urd, 0, 1))) {
					words.add(wid);
				}
				if (type == entry_type.label) {
					labels.add(wid - nwords_);
				}
				if (words.size() > MAX_LINE_SIZE && args_.model != model_name.sup) {
					break;
				}
				// if (EOS == tokens[i]){
				// break;
				// }
			}
		}
		return ntokens;
	}

	public String getLabel(int lid) {
		Utils.checkArgument(lid >= 0);
		Utils.checkArgument(lid < nlabels_);
		return words_.get(lid + nwords_).word;
	}

	public void save(OutputStream ofs) throws IOException {
		IOUtil ioutil = new IOUtil();
		ofs.write(ioutil.intToByteArray(size_));
		ofs.write(ioutil.intToByteArray(nwords_));
		ofs.write(ioutil.intToByteArray(nlabels_));
		ofs.write(ioutil.longToByteArray(ntokens_));
		// Charset charset = Charset.forName("UTF-8");
		for (int i = 0; i < size_; i++) {
			entry e = words_.get(i);
			ofs.write(e.word.getBytes());
			ofs.write(0);
			ofs.write(ioutil.longToByteArray(e.count));
			ofs.write(ioutil.intToByte(e.type.value));
		}
	}

	public void load(InputStream ifs) throws IOException {
		// words_.clear();
		// word2int_.clear();
		IOUtil ioutil = new IOUtil();
		size_ = ioutil.readInt(ifs);
		nwords_ = ioutil.readInt(ifs);
		nlabels_ = ioutil.readInt(ifs);
		ntokens_ = ioutil.readLong(ifs);

		word2int_ = new HashMap<Long, Integer>(size_);
		words_ = new ArrayList<entry>(size_);

		for (int i = 0; i < size_; i++) {
			entry e = new entry();
			e.word = ioutil.readString(ifs);
			e.count = ioutil.readLong(ifs);
			e.type = entry_type.fromValue(ioutil.readByte(ifs));
			words_.add(e);
			word2int_.put(find(e.word), i);
		}
		initTableDiscard();
		if (model_name.cbow == args_.model || model_name.sg == args_.model) {
			initNgrams();
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Dictionary [words_=");
		builder.append(words_);
		builder.append(", pdiscard_=");
		builder.append(pdiscard_);
		builder.append(", word2int_=");
		builder.append(word2int_);
		builder.append(", size_=");
		builder.append(size_);
		builder.append(", nwords_=");
		builder.append(nwords_);
		builder.append(", nlabels_=");
		builder.append(nlabels_);
		builder.append(", ntokens_=");
		builder.append(ntokens_);
		builder.append("]");
		return builder.toString();
	}

	public List<entry> getWords_() {
		return words_;
	}

	public List<Float> getPdiscard_() {
		return pdiscard_;
	}

	public Map<Long, Integer> getWord2int_() {
		return word2int_;
	}

	public int getSize_() {
		return size_;
	}

	public Args getArgs_() {
		return args_;
	}

	public String getCharsetName() {
		return charsetName;
	}

	public Class<? extends LineReader> getLineReaderClass() {
		return lineReaderClass;
	}

	public void setCharsetName(String charsetName) {
		this.charsetName = charsetName;
	}

	public void setLineReaderClass(Class<? extends LineReader> lineReaderClass) {
		this.lineReaderClass = lineReaderClass;
	}

}
