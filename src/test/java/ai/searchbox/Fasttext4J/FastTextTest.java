package ai.searchbox.Fasttext4J;

import ai.searchbox.FastText4J.Args;
import ai.searchbox.FastText4J.FastText;
import ai.searchbox.FastText4J.Pair;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FastTextTest {

    @Ignore
    @Test
    public void train() throws Exception {
        Args args = new Args();
        args.input = "/Users/davidgortega/Projects/tmp/fastText-0.9.1/data/file9short";
        args.output = "/Users/davidgortega/Projects/tmp/fastText-0.9.1/result/fil9Java";
        args.thread = 4;

        FastText fasttext = new FastText();
        fasttext.setArgs(args);
        fasttext.train();
    }

    @Ignore
    @Test
    public void unsupervised() throws IOException {
        String path = "/Users/davidgortega/Projects/tmp/fastText-0.9.1/result/fil9_skip_ns_64.bin";
        path = "/Users/davidgortega/Projects/tmp/fastText-0.9.1/result/fil9Java.bin";
        FastText fasttext = new FastText();
        fasttext.loadModel(path);

        assertEquals( fasttext.getWordVectorIn("man").size(), fasttext.getArgs().dim);
        assertEquals( fasttext.getWordVectorOut("man").size(), fasttext.getArgs().dim);
    }

    @Ignore
    @Test
    public void vecsLoad() throws Exception {
        Args args = new Args();
        args.pretrainedVectors = "/Users/davidgortega/Projects/tmp/fastText-0.9.1/result/fil9Java.vec";
        args.dim = 100;

        FastText fasttext = new FastText();
        fasttext.setArgs(args);
        fasttext.loadVecFile();
    }

    @Test
    public void supervisedQuantized() throws IOException {
        String path = "src/test/resources/ftlang.ftz";
        FastText fasttext = new FastText();
        fasttext.loadModel(path);

        List<Pair<Float, String>> preds = fasttext.predict("this is a test".split(" "), 1);
        assertEquals( preds.size() , 1);
        assertEquals( preds.get(0).getValue() , "__label__en");

        preds = fasttext.predict("esto es un test".split(" "), 2);
        assertEquals( preds.size() , 2);
        assertEquals( preds.get(0).getValue() , "__label__es");
    }
}
