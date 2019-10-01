package ai.searchbox.FastText4J.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class BufferedLineReader extends LineReader {

	private String lineDelimitingRegex_ = " |\r|\t|\\v|\f|\0";

	private BufferedReader br_;

	public BufferedLineReader(String filename, String charsetName) throws IOException, UnsupportedEncodingException {
		super(filename, charsetName);
		FileInputStream fis = new FileInputStream(file_);
		br_ = new BufferedReader(new InputStreamReader(fis, charset_));
	}

	public BufferedLineReader(InputStream inputStream, String charsetName) throws UnsupportedEncodingException {
		super(inputStream, charsetName);
		br_ = new BufferedReader(new InputStreamReader(inputStream, charset_));
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
			while (currentLine < n && (line = br_.readLine()) != null) {
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

			String lineString = br_.readLine();
			while (lineString != null && (lineString.isEmpty() || lineString.startsWith("#"))) {
				lineString = br_.readLine();
			}
			return lineString;
	}

	@Override
	public String[] readLineTokens() throws IOException {
		String line = readLine();
		if (line == null)
			return null;
		else
			return line.split(lineDelimitingRegex_, -1);
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		synchronized (lock) {
			return br_.read(cbuf, off, len);
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (lock) {
			if (br_ != null) {
				br_.close();
			}
		}
	}

	@Override
	public void rewind() throws IOException {
		synchronized (lock) {
			if (br_ != null) {
				br_.close();
			}
			if (file_ != null) {
				FileInputStream fis = new FileInputStream(file_);
				br_ = new BufferedReader(new InputStreamReader(fis, charset_));
			} else {
				// br = new BufferedReader(new InputStreamReader(inputStream,
				// charset));
				throw new UnsupportedOperationException("InputStream rewind not supported");
			}
		}
	}

	public String getLineDelimitingRege() {
		return lineDelimitingRegex_;
	}

	public void setLineDelimitingRegex(String lineDelimitingRegex) {
		this.lineDelimitingRegex_ = lineDelimitingRegex;
	}

}
