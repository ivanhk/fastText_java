package fasttext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Read/write cpp primitive type
 * 
 * @author Ivan
 *
 */
public class IOUtil {

	private static final byte byteDefaultValue = 0;

	public IOUtil() {
	}

	private int STRING_BUF_SIZE = 50;
	private byte[] int_bytes = new byte[4];
	private byte[] long_bytes = new byte[8];
	private byte[] float_bytes = new byte[4];
	private byte[] double_bytes = new byte[8];
	private byte[] string_bytes = new byte[STRING_BUF_SIZE];
	private StringBuilder stringBuilder = new StringBuilder();
	private ByteBuffer float_array_bytebuffer = null;
	private byte[] float_array_bytes = null;

	public void setStringBufferSize(int size) {
		STRING_BUF_SIZE = size;
		string_bytes = new byte[STRING_BUF_SIZE];
	}

	public void setFloatArrayBufferSize(int itemSize) {
		float_array_bytebuffer = ByteBuffer.allocate(itemSize * 4).order(ByteOrder.LITTLE_ENDIAN);
		float_array_bytes = new byte[itemSize * 4];
	}

	public int readByte(InputStream is) throws IOException {
		return is.read() & 0xFF;
	}

	public int readInt(InputStream is) throws IOException {
		is.read(int_bytes);
		return getInt(int_bytes);
	}

	public int getInt(byte[] b) {
		return (b[0] & 0xFF) << 0 | (b[1] & 0xFF) << 8 | (b[2] & 0xFF) << 16 | (b[3] & 0xFF) << 24;
	}

	public long readLong(InputStream is) throws IOException {
		is.read(long_bytes);
		return getLong(long_bytes);
	}

	public long getLong(byte[] b) {
		return (b[0] & 0xFFL) << 0 | (b[1] & 0xFFL) << 8 | (b[2] & 0xFFL) << 16 | (b[3] & 0xFFL) << 24
				| (b[4] & 0xFFL) << 32 | (b[5] & 0xFFL) << 40 | (b[6] & 0xFFL) << 48 | (b[7] & 0xFFL) << 56;
	}

	public float readFloat(InputStream is) throws IOException {
		is.read(float_bytes);
		return getFloat(float_bytes);
	}

	public void readFloat(InputStream is, float[] data) throws IOException {
		is.read(float_array_bytes);
		float_array_bytebuffer.clear();
		((ByteBuffer) float_array_bytebuffer.put(float_array_bytes).flip()).asFloatBuffer().get(data);
	}

	public float getFloat(byte[] b) {
		return Float
				.intBitsToFloat((b[0] & 0xFF) << 0 | (b[1] & 0xFF) << 8 | (b[2] & 0xFF) << 16 | (b[3] & 0xFF) << 24);
	}

	public double readDouble(InputStream is) throws IOException {
		is.read(double_bytes);
		return getDouble(double_bytes);
	}

	public double getDouble(byte[] b) {
		return Double.longBitsToDouble(getLong(b));
	}

	public String readString(InputStream is) throws IOException {
		int b = is.read();
		if (b < 0) {
			return null;
		}
		int i = -1;
		stringBuilder.setLength(0);
		// ascii space, \n, \0
		while (b > -1 && b != 32 && b != 10 && b != 0) {
			string_bytes[++i] = (byte) b;
			b = is.read();
			if (i == STRING_BUF_SIZE - 1) {
				stringBuilder.append(new String(string_bytes));
				i = -1;
				Arrays.fill(string_bytes, byteDefaultValue);
			}
		}
		stringBuilder.append(new String(string_bytes, 0, i + 1));
		return stringBuilder.toString();
	}

	public int intToByte(int i) {
		return (i & 0xFF);
	}

	public byte[] intToByteArray(int i) {
		int_bytes[0] = (byte) ((i >> 0) & 0xff);
		int_bytes[1] = (byte) ((i >> 8) & 0xff);
		int_bytes[2] = (byte) ((i >> 16) & 0xff);
		int_bytes[3] = (byte) ((i >> 24) & 0xff);
		return int_bytes;
	}

	public byte[] longToByteArray(long l) {
		long_bytes[0] = (byte) ((l >> 0) & 0xff);
		long_bytes[1] = (byte) ((l >> 8) & 0xff);
		long_bytes[2] = (byte) ((l >> 16) & 0xff);
		long_bytes[3] = (byte) ((l >> 24) & 0xff);
		long_bytes[4] = (byte) ((l >> 32) & 0xff);
		long_bytes[5] = (byte) ((l >> 40) & 0xff);
		long_bytes[6] = (byte) ((l >> 48) & 0xff);
		long_bytes[7] = (byte) ((l >> 56) & 0xff);

		return long_bytes;
	}

	public byte[] floatToByteArray(float f) {
		return intToByteArray(Float.floatToIntBits(f));
	}

	public byte[] floatToByteArray(float[] f) {
		float_array_bytebuffer.clear();
		float_array_bytebuffer.asFloatBuffer().put(f);
		return float_array_bytebuffer.array();
	}

	public byte[] doubleToByteArray(double d) {
		return longToByteArray(Double.doubleToRawLongBits(d));
	}

}
