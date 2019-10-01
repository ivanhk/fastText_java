package ai.searchbox.FastText4J.math.quant;

/**
 * Interface for Quantization codes
 * so that it can be backed by an array of int,
 * as well as memory-mapped data structures
 */
public interface QCodes {

  int get(int i);

  int size();

}
