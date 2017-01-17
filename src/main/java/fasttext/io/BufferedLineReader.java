package fasttext.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class BufferedLineReader extends LineReader {

	public String LINE_SPLITTER = " |\r|\t|\\v|\f|\0";

	private BufferedReader br;

	public BufferedLineReader(String filename, String charsetName) throws IOException, UnsupportedEncodingException {
		super(filename, charsetName);
		FileInputStream fis = new FileInputStream(file);
		br = new BufferedReader(new InputStreamReader(fis, charset));
	}

	public BufferedLineReader(InputStream inputStream, String charsetName) throws UnsupportedEncodingException {
		super(inputStream, charsetName);
		br = new BufferedReader(new InputStreamReader(inputStream, charset));
	}

	@Override
	public long skipLine(long n) throws IOException {
		if (n < 0L) {
			throw new IllegalArgumentException("skip value is negative");
		}
		String line;
		long currentLine = 0;
		long readLine = 0;
		synchronized (lock) {
			while (currentLine < n && (line = br.readLine()) != null) {
				readLine++;
				if (line == null || line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				currentLine++;
			}
			return readLine;
		}
	}

	@Override
	public String readLine() throws IOException {
		synchronized (lock) {
			String lineString = br.readLine();
			while (lineString != null && (lineString.isEmpty() || lineString.startsWith("#"))) {
				lineString = br.readLine();
			}
			return lineString;
		}
	}

	@Override
	public String[] readLineTokens() throws IOException {
		String line = readLine();
		if (line == null)
			return null;
		else
			return line.split(LINE_SPLITTER, -1);
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		synchronized (lock) {
			return br.read(cbuf, off, len);
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (lock) {
			if (br != null) {
				br.close();
			}
		}
	}

	@Override
	public void rewind() throws IOException {
		synchronized (lock) {
			if (br != null) {
				br.close();
			}
			if (file != null) {
				FileInputStream fis = new FileInputStream(file);
				br = new BufferedReader(new InputStreamReader(fis, charset));
			} else {
				// br = new BufferedReader(new InputStreamReader(inputStream,
				// charset));
				throw new UnsupportedOperationException("InputStream rewind not supported");
			}
		}
	}
}
