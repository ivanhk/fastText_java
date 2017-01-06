package fasttext;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Read/write cpp primitive type
 * @author Ivan
 *
 */
public class IOUtil {

	private static final int MAX_STRING_SIZE = 50;

	public static int readInt(InputStream is) throws IOException {
		byte[] bytes = new byte[4];
		is.read(bytes);
		return getInt(bytes);
	}

	public static int getInt(byte[] b) {
		return (b[0] & 0xFF) << 0 | (b[1] & 0xFF) << 8 | (b[2] & 0xFF) << 16 | (b[3] & 0xFF) << 24;
	}

	public static long readLong(InputStream is) throws IOException {
		byte[] bytes = new byte[8];
		is.read(bytes);
		return getLong(bytes);
	}

	public static long getLong(byte[] b) {
		return (b[0] & 0xFFL) << 0 | (b[1] & 0xFFL) << 8 | (b[2] & 0xFFL) << 16 | (b[3] & 0xFFL) << 24
				| (b[4] & 0xFFL) << 32 | (b[5] & 0xFFL) << 40 | (b[6] & 0xFFL) << 48 | (b[7] & 0xFFL) << 56;
	}

	public static float readFloat(InputStream is) throws IOException {
		byte[] bytes = new byte[4];
		is.read(bytes);
		return getFloat(bytes);
	}

	public static float getFloat(byte[] b) {
		int accum = (b[0] & 0xFF) << 0 | (b[1] & 0xFF) << 8 | (b[2] & 0xFF) << 16 | (b[3] & 0xFF) << 24;
		return Float.intBitsToFloat(accum);
	}

	public static double readDouble(InputStream is) throws IOException {
		byte[] bytes = new byte[8];
		is.read(bytes);
		return getDouble(bytes);
	}

	public static double getDouble(byte[] b) {
		long accum = getLong(b);

		return Double.longBitsToDouble(accum);
	}

	public static String readString(DataInputStream dis) throws IOException {
		byte[] bytes = new byte[MAX_STRING_SIZE];
		byte b = dis.readByte();
		int i = -1;
		StringBuilder sb = new StringBuilder();
		while (b != 32 && b != 10 && b != 0) { // ascii
			i++;
			bytes[i] = b;
			b = dis.readByte();
			if (i == 49) {
				sb.append(new String(bytes));
				i = -1;
				bytes = new byte[MAX_STRING_SIZE];
			}
		}
		sb.append(new String(bytes, 0, i + 1));
		return sb.toString();
	}

	public static byte[] intToByteArray(int i) {
		return new byte[] { (byte) ((i >> 0) & 0xff), (byte) ((i >> 8) & 0xff), (byte) ((i >> 16) & 0xff),
				(byte) ((i >> 24) & 0xff) };
	}

	public static byte[] longToByteArray(long l) {
		return new byte[] { (byte) ((l >> 0) & 0xff), (byte) ((l >> 8) & 0xff), (byte) ((l >> 16) & 0xff),
				(byte) ((l >> 24) & 0xff), (byte) ((l >> 32) & 0xff), (byte) ((l >> 40) & 0xff),
				(byte) ((l >> 48) & 0xff), (byte) ((l >> 56) & 0xff) };
	}

	public static byte[] floatToByteArray(float f) {
		int i = Float.floatToIntBits(f);
		return intToByteArray(i);
	}

	public static byte[] doubleToByteArray(double d) {
		long l = Double.doubleToRawLongBits(d);
		return new byte[] { (byte) ((l >> 0) & 0xff), (byte) ((l >> 8) & 0xff), (byte) ((l >> 16) & 0xff),
				(byte) ((l >> 24) & 0xff), (byte) ((l >> 32) & 0xff), (byte) ((l >> 40) & 0xff),
				(byte) ((l >> 48) & 0xff), (byte) ((l >> 56) & 0xff) };
	}

}
