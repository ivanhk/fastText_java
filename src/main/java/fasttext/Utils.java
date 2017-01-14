package fasttext;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.RandomAccess;

public class Utils {

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * @param expression
	 *            a boolean expression
	 * @throws IllegalArgumentException
	 *             if {@code expression} is false
	 */
	public static void checkArgument(boolean expression) {
		if (!expression) {
			throw new IllegalArgumentException();
		}
	}

	public static void checkArgument(boolean expression, String message) {
		if (!expression) {
			throw new IllegalArgumentException(message);
		}
	}

	public static boolean isEmpty(String str) {
		return (str == null || str.isEmpty());
	}

	public static <K, V> V mapGetOrDefault(Map<K, V> map, K key, V defaultValue) {
		return map.containsKey(key) ? map.get(key) : defaultValue;
	}

	public static int randomInt(Random rnd, int lower, int upper) {
		checkArgument(lower <= upper & lower > 0, "randomInt lower=" + lower + ", upper=" + upper);
		if (lower == upper) {
			return lower;
		}
		return rnd.nextInt(upper - lower) + lower;
	}

	public static float randomFloat(Random rnd, float lower, float upper) {
		checkArgument(lower <= upper, "randomFloat lower=" + lower + ", upper=" + upper);
		if (lower == upper) {
			return lower;
		}
		return (rnd.nextFloat() * (upper - lower)) + lower;
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
	 * @param pos
	 *            line numbers start from 1
	 * @throws IOException
	 */
	public static void seekLine(BufferedReader br, long pos) throws IOException {
		// br.reset();
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
	public static void shuffle(List<?> list, Random rnd) {
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
	public static void swap(Object[] arr, int i, int j) {
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
