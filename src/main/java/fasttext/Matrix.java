package fasttext;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;


import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.Well19937c;
import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

public class Matrix {

	private static Logger logger = Logger.getLogger(Matrix.class);

	public float[][] data_ = null;
	public int m_ = 0; // vocabSize
	public int n_ = 0; // layer1Size

	public Matrix() {
	}

	public Matrix(int m, int n) {
		m_ = m;
		n_ = n;
		data_ = new float[m][n];
	}

	public Matrix(final Matrix other) {
		m_ = other.m_;
		n_ = other.n_;
		data_ = new float[m_][n_];
		for (int i = 0; i < m_; i++) {
			for (int j = 0; j < n_; j++) {
				data_[i][j] = other.data_[i][j];
			}
		}
	}

	public void zero() {
		for (int i = 0; i < m_; i++) {
			for (int j = 0; j < n_; j++) {
				data_[i][j] = 0.0f;
			}
		}
	}

	public void uniform(float a) {
		UniformRealDistribution urd = new UniformRealDistribution(new Well19937c(1), -a, a);
		for (int i = 0; i < m_; i++) {
			for (int j = 0; j < n_; j++) {
				data_[i][j] = (float) urd.sample();
			}
		}
	}

	public void addRow(final Vector vec, int i, float a) {
		Preconditions.checkArgument(i >= 0);
		Preconditions.checkArgument(i < m_);
		Preconditions.checkArgument(vec.m_ == n_);
		for (int j = 0; j < n_; j++) {
			data_[i][j] += a * vec.data_[j];
		}
	}

	public float dotRow(final Vector vec, int i) {
		Preconditions.checkArgument(i >= 0);
		Preconditions.checkArgument(i < m_);
		Preconditions.checkArgument(vec.m_ == n_);
		float d = 0.0f;
		for (int j = 0; j < n_; j++) {
			d += data_[i][j] * vec.data_[j];
		}
		return d;
	}

	public void load(InputStream input) throws IOException {
		m_ = (int) IOUtil.readLong(input);
		n_ = (int) IOUtil.readLong(input);

		data_ = new float[m_][n_];
		for (int i = 0; i < m_; i++) {
			for (int j = 0; j < n_; j++) {
				data_[i][j] = IOUtil.readFloat(input);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Matrix loal m_: " + m_);
			logger.debug("Matrix loal n_: " + n_);
			StringBuilder strBuilder = new StringBuilder("line1:");
			for (int j = 0; j < n_; j++) {
				strBuilder.append(" ").append(data_[0][j]);
			}
			logger.debug(strBuilder.toString());
		}
	}

	public void loadNIO(InputStream input) throws IOException {
		m_ = (int) IOUtil.readLong(input);
		n_ = (int) IOUtil.readLong(input);

		data_ = new float[m_][n_];
		for (int i = 0; i < m_; i++) {
			byte []  buf = new byte[n_ * 4];
			int nread = input.read(buf);
			final FloatBuffer fb = ByteBuffer.wrap(buf).asFloatBuffer();
			fb.get(data_[i]); // Copy the contents of the FloatBuffer into dst


			//for (int j = 0; j < n_; j++) {
			//	data_[i][j] = IOUtil.readFloat(input);
			//}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Matrix loal m_: " + m_);
			logger.debug("Matrix loal n_: " + n_);
			StringBuilder strBuilder = new StringBuilder("line1:");
			for (int j = 0; j < n_; j++) {
				strBuilder.append(" ").append(data_[0][j]);
			}
			logger.debug(strBuilder.toString());
		}
	}

	public void save(OutputStream ofs) throws IOException {
		ofs.write(IOUtil.longToByteArray(m_));
		ofs.write(IOUtil.longToByteArray(n_));
		for (int i = 0; i < m_; i++) {
			for (int j = 0; j < n_; j++) {
				ofs.write(IOUtil.floatToByteArray(data_[i][j]));
			}
		}
	}

	// Matrix& Matrix::operator=(const Matrix& other) {
	// Matrix temp(other);
	// m_ = temp.m_;
	// n_ = temp.n_;
	// std::swap(data_, temp.data_);
	// return *this;
	// }
}
