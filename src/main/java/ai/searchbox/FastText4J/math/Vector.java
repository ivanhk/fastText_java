package ai.searchbox.FastText4J.math;

import ai.searchbox.FastText4J.Utils;

public class Vector {

	public int m;
	public float[] data;

	public Vector(int size) {
		m = size;
		data = new float[size];
	}

	public int size() {
		return m;
	}

	public void zero() {
		for (int i = 0; i < m; i++) {
			data[i] = 0.0f;
		}
	}

	public void mul(float a) {
		for (int i = 0; i < m; i++) {
			data[i] *= a;
		}
	}

	public void addRow(final Matrix A, int i) {
		Utils.checkArgument(i >= 0);
		Utils.checkArgument(i < A.m);
		Utils.checkArgument(m == A.n);

		A.addToVector(this, i);
	}

	public void addRow(final Matrix A, int i, float a) {
		Utils.checkArgument(i >= 0);
		Utils.checkArgument(i < A.m);
		Utils.checkArgument(m == A.n);

		for (int j = 0; j < A.n; j++) {
			data[j] += a * A.data[i][j];
		}
	}

	public void mul(final Matrix A, final Vector vec) {
		Utils.checkArgument(A.m == m);
		Utils.checkArgument(A.n == vec.m);

		for (int i = 0; i < m; i++) {
			data[i] = 0.0f;
			for (int j = 0; j < A.n; j++) {
				// data[i] += A.data[i][j] * vec.data[j];
				data[i] = A.dotRow(vec, i);
			}
		}
	}

	public int argmax() {
		float max = data[0];
		int argmax = 0;
		for (int i = 1; i < m; i++) {
			if (data[i] > max) {
				max = data[i];
				argmax = i;
			}
		}

		return argmax;
	}

	public float get(int i) {
		return data[i];
	}

	public void set(int i, float value) {
		data[i] = value;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (float data : data) {
			builder.append(data).append(' ');
		}
		if (builder.length() > 1) {
			builder.setLength(builder.length() - 1);
		}
		return builder.toString();
	}

}
