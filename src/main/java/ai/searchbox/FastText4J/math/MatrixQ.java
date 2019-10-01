package ai.searchbox.FastText4J.math;

import ai.searchbox.FastText4J.Utils;
import ai.searchbox.FastText4J.io.IOUtil;
import ai.searchbox.FastText4J.math.quant.ProductQuantizer;
import ai.searchbox.FastText4J.math.quant.QCodeArray;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MatrixQ extends Matrix {
    QCodeArray codes;
    ProductQuantizer pq = new ProductQuantizer();

    QCodeArray normCodes;
    ProductQuantizer npq = new ProductQuantizer();

    boolean qnorm;

    public void addToVector(Vector x, int t) {
        float norm = 1f;
        if (qnorm) {
            int cPosition = npq.getCentroidsPosition(0, normCodes.get(t));
            norm = npq.getCentroid(cPosition);
        }
        pq.addCode(x, codes, t, norm);
    }

    @Override
    public float dotRow(final Vector vec, int i) {
        Utils.checkArgument(i >= 0);
        Utils.checkArgument(i < m);
        Utils.checkArgument(vec.m == n);

        float norm = 1f;
        if (qnorm) {
            int cPosition = npq.getCentroidsPosition(0, normCodes.get(i));
            norm = npq.getCentroid(cPosition);
        }

        return pq.mulCode(vec, codes, i, norm);
    }

    public void load(InputStream is) throws IOException {
        IOUtil ioutil = new IOUtil();

        qnorm = ioutil.readBool(is);
        m = (int) ioutil.readLong(is);
        n = (int) ioutil.readLong(is);

        int codeSize = ioutil.readInt(is);

        int[] rawCodes = new int[codeSize];
        for (int i = 0; i < codeSize; i++) {
            int c = ioutil.readByteAsInt(is);
            rawCodes[i] = c;
        }

        codes = new QCodeArray(rawCodes);
        pq.load(is);

        if (qnorm) {
            int[] rawNormCodes = new int[m];
            for (int i = 0; i < m; i++) {
                int c = ioutil.readByteAsInt(is);
                rawNormCodes[i] = c;
            }

            normCodes = new QCodeArray(rawNormCodes);
            npq.load(is);
        }
    }

    public void save(OutputStream os) throws IOException {
        IOUtil ioutil = new IOUtil();

        os.write(ioutil.booleanToByteArray(qnorm));
        os.write(ioutil.longToByteArray(m));
        os.write(ioutil.longToByteArray(n));
        os.write(ioutil.intToByteArray(codes.size()));

        for (int i = 0; i < codes.size(); i++) {
            os.write(ioutil.intToByte(codes.get(i)));
        }

        pq.save(os);

        if (qnorm) {
            for (int i = 0; i < m; i++) {
                os.write(ioutil.intToByte(normCodes.get(i)));
            }

            npq.save(os);
        }
    }
}
