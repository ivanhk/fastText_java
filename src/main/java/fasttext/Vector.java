package fasttext;

import java.util.Arrays;

import com.google.common.base.Preconditions;

public class Vector {

	public int m_;
	public float[] data_;

	public Vector(int size) {
		m_ = size;
		data_ = new float[size];
	}
	
	public int size(){
		return m_;
	}

	public void zero() {
		for (int i = 0; i < m_; i++) {
			data_[i] = 0.0f;
		}
	}

	public void mul(float a) {
		for (int i = 0; i < m_; i++) {
			data_[i] *= a;
		}
	}

	public void addRow(final Matrix A, int i) {
		Preconditions.checkArgument(i >= 0);
		Preconditions.checkArgument(i < A.m_);
		Preconditions.checkArgument(m_ == A.n_);
		for (int j = 0; j < A.n_; j++) { // layer size
			data_[j] += A.data_[i][j];
		}
	}

	public void addRow(final Matrix A, int i, float a) {
		Preconditions.checkArgument(i >= 0);
		Preconditions.checkArgument(i < A.m_);
		Preconditions.checkArgument(m_ == A.n_);
		for (int j = 0; j < A.n_; j++) {
			data_[j] += a * A.data_[i][j];
		}
	}

	public void mul(final Matrix A, final Vector vec) {
		Preconditions.checkArgument(A.m_ == m_);
		Preconditions.checkArgument(A.n_ == vec.m_);
		for (int i = 0; i < m_; i++) {
			data_[i] = 0.0f;
			for (int j = 0; j < A.n_; j++) {
				data_[i] += A.data_[i][j] * vec.data_[j];
			}
		}
	}

	public int argmax() {
		float max = data_[0];
		int argmax = 0;
		for (int i = 1; i < m_; i++) {
			if (data_[i] > max) {
				max = data_[i];
				argmax = i;
			}
		}
		return argmax;
	}

	public float get(int i) {
		return data_[i];
	}

	public void set(int i, float value) {
		data_[i] = value;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Vector [data_=");
		builder.append(Arrays.toString(data_));
		builder.append("]");
		return builder.toString();
	}

}
