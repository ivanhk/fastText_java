package fasttext.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MappedByteBufferLineReader extends LineReader {

	private int string_buf_size_ = 1024;

	private RandomAccessFile raf;
	private FileChannel channel;
	private ByteBuffer byteBuffer; // MappedByteBuffer

	private byte[] bytes = new byte[string_buf_size_];
	private StringBuilder sb = new StringBuilder();
	private List<String> tokens = new ArrayList<String>();
	private byte default_byte = 0;

	public MappedByteBufferLineReader(String filename, String charsetName)
			throws IOException, UnsupportedEncodingException {
		super(filename, charsetName);
		raf = new RandomAccessFile(file_, "r");
		channel = raf.getChannel();
		byteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
	}

	public MappedByteBufferLineReader(InputStream inputStream, String charsetName) throws UnsupportedEncodingException {
		super(inputStream, charsetName);
		throw new UnsupportedOperationException("MappedLineReader InputStream not supported");
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
			ensureOpen();
			while (currentLine < n && (line = getLine()) != null) {
				readLine++;
				if (line == null || line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				currentLine++;
			}
		}
		return readLine;
	}

	@Override
	public String readLine() throws IOException {
		synchronized (lock) {
			ensureOpen();
			String lineString = getLine();
			while (lineString != null && (lineString.isEmpty() || lineString.startsWith("#"))) {
				lineString = getLine();
			}
			return lineString;
		}
	}

	@Override
	public String[] readLineTokens() throws IOException {
		synchronized (lock) {
			ensureOpen();
			String[] tokens = getLineTokens();
			while (tokens != null && ((tokens.length == 1 && tokens[0].isEmpty()) || tokens[0].startsWith("#"))) {
				tokens = getLineTokens();
			}
			return tokens;
		}
	}

	@Override
	public void rewind() throws IOException {
		synchronized (lock) {
			ensureOpen();
			raf.seek(0);
			channel.position(0);
			byteBuffer.position(0);
		}
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		synchronized (lock) {
			ensureOpen();
			if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0)) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return 0;
			}

			CharBuffer charBuffer = byteBuffer.asCharBuffer();
			int length = Math.min(len, charBuffer.remaining());
			charBuffer.get(cbuf, off, length);
			return length == len ? len : -1;
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (lock) {
			if (raf != null) {
				raf.close();
				channel = null;
				byteBuffer = null;
			}
		}
	}

	/** Checks to make sure that the stream has not been closed */
	private void ensureOpen() throws IOException {
		if (byteBuffer == null)
			throw new IOException("Stream closed");
	}

	protected String getLine() {
		if (!byteBuffer.hasRemaining()) {
			return null;
		}
		sb.setLength(0);
		int b = byteBuffer.get();
		int i = -1;
		while (byteBuffer.hasRemaining() && !(b >= 10 && b <= 13) && b != 0) {
			bytes[++i] = (byte) b;
			b = byteBuffer.get();
			if (i == string_buf_size_ - 1) {
				sb.append(new String(bytes, charset_));
				i = -1;
				Arrays.fill(bytes, default_byte);
			}
		}
		sb.append(new String(bytes, 0, i + 1, charset_));
		return sb.toString();
	}

	// " |\r|\t|\\v|\f|\0"
	// 32 ' ', 9 \t, 10 \n, 11 \\v, 12 \f, 13 \r, 0 \0
	protected String[] getLineTokens() {
		if (!byteBuffer.hasRemaining()) {
			return null;
		}
		tokens.clear();
		sb.setLength(0);
		int b = byteBuffer.get();
		int i = -1;
		while (byteBuffer.hasRemaining()) {
			if (b >= 10 && b <= 13) {
				break;
			} else if (b == 9 || b == 32 || b == 0) {
				sb.append(new String(bytes, 0, i + 1, charset_));
				tokens.add(sb.toString());
				sb.setLength(0);
				i = -1;
				b = byteBuffer.get();
			} else {
				bytes[++i] = (byte) b;
				b = byteBuffer.get();
				if (i == string_buf_size_ - 1) {
					sb.append(new String(bytes, charset_));
					i = -1;
					Arrays.fill(bytes, default_byte);
				}
			}
		}

		sb.append(new String(bytes, 0, i + 1, charset_));
		tokens.add(sb.toString());
		return tokens.isEmpty() && !byteBuffer.hasRemaining() ? null : tokens.toArray(new String[tokens.size()]);
	}

}
