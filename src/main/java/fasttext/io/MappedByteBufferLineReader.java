package fasttext.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class MappedByteBufferLineReader extends LineReader {

	private static int DEFAULT_BUFFER_SIZE = 1024;

	private volatile ByteBuffer byteBuffer_ = null; // MappedByteBuffer
	private RandomAccessFile raf_ = null;
	private FileChannel channel_ = null;
	private byte[] bytes_ = null;

	private int string_buf_size_ = DEFAULT_BUFFER_SIZE;
	private boolean fillLine_ = false;

	private StringBuilder sb_ = new StringBuilder();
	private List<String> tokens_ = new ArrayList<String>();

	public MappedByteBufferLineReader(String filename, String charsetName)
			throws IOException, UnsupportedEncodingException {
		super(filename, charsetName);
		raf_ = new RandomAccessFile(file_, "r");
		channel_ = raf_.getChannel();
		byteBuffer_ = channel_.map(FileChannel.MapMode.READ_ONLY, 0, channel_.size());
		bytes_ = new byte[string_buf_size_];
	}

	public MappedByteBufferLineReader(InputStream inputStream, String charsetName) throws UnsupportedEncodingException {
		this(inputStream, charsetName, DEFAULT_BUFFER_SIZE);
	}

	public MappedByteBufferLineReader(InputStream inputStream, String charsetName, int buf_size)
			throws UnsupportedEncodingException {
		super(inputStream instanceof BufferedInputStream ? inputStream : new BufferedInputStream(inputStream),
				charsetName);
		string_buf_size_ = buf_size;
		byteBuffer_ = ByteBuffer.allocateDirect(string_buf_size_); // ByteBuffer.allocate(string_buf_size_);
		bytes_ = new byte[string_buf_size_];
		if (inputStream == System.in) {
			fillLine_ = true;
		}
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
			if (raf_ != null) {
				raf_.seek(0);
				channel_.position(0);
			}
			byteBuffer_.position(0);
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

			CharBuffer charBuffer = byteBuffer_.asCharBuffer();
			int length = Math.min(len, charBuffer.remaining());
			charBuffer.get(cbuf, off, length);

			if (inputStream_ != null) {
				off += length;

				while (off < len) {
					fillByteBuffer();
					if (!byteBuffer_.hasRemaining()) {
						break;
					}
					charBuffer = byteBuffer_.asCharBuffer();
					length = Math.min(len, charBuffer.remaining());
					charBuffer.get(cbuf, off, length);
					off += length;
				}
			}
			return length == len ? len : -1;
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (lock) {
			if (raf_ != null) {
				raf_.close();
			} else if (inputStream_ != null) {
				inputStream_.close();
			}
			channel_ = null;
			byteBuffer_ = null;
		}
	}

	/** Checks to make sure that the stream has not been closed */
	private void ensureOpen() throws IOException {
		if (byteBuffer_ == null)
			throw new IOException("Stream closed");
	}

	protected String getLine() throws IOException {
		fillByteBuffer();
		if (!byteBuffer_.hasRemaining()) {
			return null;
		}
		sb_.setLength(0);
		int b = -1;
		int i = -1;
		do {
			b = byteBuffer_.get();
			if ((b >= 10 && b <= 13) || b == 0) {
				break;
			}
			bytes_[++i] = (byte) b;
			if (i == string_buf_size_ - 1) {
				sb_.append(new String(bytes_, charset_));
				i = -1;
			}
			fillByteBuffer();
		} while (byteBuffer_.hasRemaining());

		sb_.append(new String(bytes_, 0, i + 1, charset_));
		return sb_.toString();
	}

	// " |\r|\t|\\v|\f|\0"
	// 32 ' ', 9 \t, 10 \n, 11 \\v, 12 \f, 13 \r, 0 \0
	protected String[] getLineTokens() throws IOException {
		fillByteBuffer();
		if (!byteBuffer_.hasRemaining()) {
			return null;
		}
		tokens_.clear();
		sb_.setLength(0);

		int b = -1;
		int i = -1;
		do {
			b = byteBuffer_.get();

			if ((b >= 10 && b <= 13) || b == 0) {
				break;
			} else if (b == 9 || b == 32) {
				sb_.append(new String(bytes_, 0, i + 1, charset_));
				tokens_.add(sb_.toString());
				sb_.setLength(0);
				i = -1;
			} else {
				bytes_[++i] = (byte) b;
				if (i == string_buf_size_ - 1) {
					sb_.append(new String(bytes_, charset_));
					i = -1;
				}
			}
			fillByteBuffer();
		} while (byteBuffer_.hasRemaining());

		sb_.append(new String(bytes_, 0, i + 1, charset_));
		tokens_.add(sb_.toString());
		return tokens_.toArray(new String[tokens_.size()]);
	}

	private void fillByteBuffer() throws IOException {
		if (inputStream_ == null || byteBuffer_.hasRemaining()) {
			return;
		}

		byteBuffer_.clear();

		int b;
		for (int i = 0; i < string_buf_size_; i++) {
			b = inputStream_.read();
			if (b < 0) { // END OF STREAM
				break;
			}
			byteBuffer_.put((byte) b);
			if (fillLine_) {
				if ((b >= 10 && b <= 13) || b == 0) {
					break;
				}
			}
		}

		byteBuffer_.flip();
	}

}
