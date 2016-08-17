package fasttext;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

import org.apache.commons.math3.random.RandomGenerator;

public class Utils {

	static final int SIGMOID_TABLE_SIZE = 512;
	static final int MAX_SIGMOID = 8;
	static final int LOG_TABLE_SIZE = 512;

	static float[] t_sigmoid;
	static float[] t_log;

	static {
		initTables();
	}

	static void initTables() {
		initSigmoid();
		initLog();
	}

	static void initSigmoid() {
		t_sigmoid = new float[SIGMOID_TABLE_SIZE + 1];
		for (int i = 0; i < SIGMOID_TABLE_SIZE + 1; i++) {
			float x = (float) (i * 2 * MAX_SIGMOID) / SIGMOID_TABLE_SIZE - MAX_SIGMOID;
			t_sigmoid[i] = (float) (1.0 / (1.0 + Math.exp(-x)));
		}
	}

	static void initLog() {
		t_log = new float[LOG_TABLE_SIZE + 1];
		for (int i = 0; i < LOG_TABLE_SIZE + 1; i++) {
			float x = (float) (((float) (i) + 1e-5) / LOG_TABLE_SIZE);
			t_log[i] = (float) Math.log(x);
		}
	}

	public static float log(float x) {
		if (x > 1.0) {
			return 0.0f;
		}
		int i = (int) (x * LOG_TABLE_SIZE);
		return t_log[i];
	}

	public static float sigmoid(float x) {
		if (x < -MAX_SIGMOID) {
			return 0.0f;
		} else if (x > MAX_SIGMOID) {
			return 1.0f;
		} else {
			int i = (int) ((x + MAX_SIGMOID) * SIGMOID_TABLE_SIZE / MAX_SIGMOID / 2);
			return t_sigmoid[i];
		}
	}

	public static boolean isEmpty(String str) {
		return (str == null || str.isEmpty());
	}

	public static long sizeLine(String filename) throws IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(filename));
		try {
			byte[] c = new byte[1024];
			long count = 0;
			int readChars = 0;
			boolean endsWithoutNewLine = false;
			while ((readChars = is.read(c)) != -1) {
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n')
						++count;
				}
				endsWithoutNewLine = (c[readChars - 1] != '\n');
			}
			if (endsWithoutNewLine) {
				++count;
			}
			return count;
		} finally {
			is.close();
		}
	}

	/**
	 * 
	 * @param br
	 * @param pos line numbers start from 1
	 * @throws IOException
	 */
	public static void seek(BufferedReader br, long pos) throws IOException {
//		br.reset();
		String line;
		int currentLine = 1;
		while (currentLine < pos && (line = br.readLine()) != null) {
			if (Utils.isEmpty(line) || line.startsWith("#")) {
				continue;
			}
			currentLine++;
		}
	}

	private static final int SHUFFLE_THRESHOLD = 5;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void shuffle(List<?> list, RandomGenerator rnd) {
		int size = list.size();
		if (size < SHUFFLE_THRESHOLD || list instanceof RandomAccess) {
			for (int i = size; i > 1; i--)
				swap(list, i - 1, rnd.nextInt(i));
		} else {
			Object arr[] = list.toArray();

			// Shuffle array
			for (int i = size; i > 1; i--)
				swap(arr, i - 1, rnd.nextInt(i));

			// Dump array back into list
			// instead of using a raw type here, it's possible to capture
			// the wildcard but it will require a call to a supplementary
			// private method
			ListIterator it = list.listIterator();
			for (int i = 0; i < arr.length; i++) {
				it.next();
				it.set(arr[i]);
			}
		}
	}

	/**
	 * Swaps the two specified elements in the specified array.
	 */
	private static void swap(Object[] arr, int i, int j) {
		Object tmp = arr[i];
		arr[i] = arr[j];
		arr[j] = tmp;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void swap(List<?> list, int i, int j) {
		// instead of using a raw type here, it's possible to capture
		// the wildcard but it will require a call to a supplementary
		// private method
		final List l = list;
		l.set(i, l.set(j, l.get(i)));
	}

}
