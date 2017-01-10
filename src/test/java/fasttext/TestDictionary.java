package fasttext;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestDictionary {

	Dictionary dictionary = new Dictionary(new Args());
	
	@Test
	public void testHash() {
		assertEquals(dictionary.hash(","), 688690635l);
		assertEquals(dictionary.hash("is"), 1312329493l);
		assertEquals(dictionary.hash("</s>"), 3617362777l);
	}

	@Test
	public void testFind() {
		assertEquals(dictionary.find(","), 28690635l);
		assertEquals(dictionary.find("is"), 22329493l);
		assertEquals(dictionary.find("</s>"), 17362777l);
	}
}
