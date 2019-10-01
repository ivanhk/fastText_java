package ai.searchbox.FastText4J.math;

import ai.searchbox.FastText4J.io.IOUtil;
import ai.searchbox.FastText4J.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

public class Matrix {

	public int m = 0;
	public int n = 0;
	public float[][] data = null;

	public Matrix() {}

	public Matrix(int m, int n) {
		this.m = m;
		this.n = n;
		data = new float[m][n];
	}

	public void zero() {
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				data[i][j] = 0.0f;
			}
		}
	}

	public void uniform(float a) {
		Random random = new Random(1l);
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				data[i][j] = Utils.randomFloat(random, -a, a);
			}
		}
	}

	public void addToVector(Vector x, int t) {
		for (int j = 0; j < this.n; j++) { // layer size
			x.data[j] += data[t][j];
		}
	}

	public void addRow(final Vector vec, int i, float a) {
		Utils.checkArgument(i >= 0);
		Utils.checkArgument(i < m);
		Utils.checkArgument(vec.m == n);

		for (int j = 0; j < n; j++) {
			data[i][j] += a * vec.data[j];
		}
	}

	public float dotRow(final Vector vec, int i) {
		Utils.checkArgument(i >= 0);
		Utils.checkArgument(i < m);
		Utils.checkArgument(vec.m == n);

		float d = 0.0f;
		for (int j = 0; j < n; j++) {
			d += data[i][j] * vec.data[j];
		}
		return d;
	}

	public void load(InputStream input) throws IOException {
		IOUtil ioutil = new IOUtil();

		m = (int) ioutil.readLong(input);
		n = (int) ioutil.readLong(input);

		ioutil.setFloatArrayBufferSize(n);
		data = new float[m][n];
		for (int i = 0; i < m; i++) {
			ioutil.readFloat(input, data[i]);
		}
	}

	public void save(OutputStream ofs) throws IOException {
		IOUtil ioutil = new IOUtil();

		ioutil.setFloatArrayBufferSize(n);
		ofs.write(ioutil.longToByteArray(m));
		ofs.write(ioutil.longToByteArray(n));
		for (int i = 0; i < m; i++) {
			ofs.write(ioutil.floatToByteArray(data[i]));
		}
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Matrix [data_=");
		if (data != null) {
			builder.append("[");
			for (int i = 0; i < m && i < 10; i++) {
				for (int j = 0; j < n && j < 10; j++) {
					builder.append(data[i][j]).append(",");
				}
			}
			builder.setLength(builder.length() - 1);
			builder.append("]");
		} else {
			builder.append("null");
		}
		builder.append(", m_=");
		builder.append(m);
		builder.append(", n_=");
		builder.append(n);
		builder.append("]");
		return builder.toString();
	}

}
