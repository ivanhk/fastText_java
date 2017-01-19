package fasttext;

import static org.junit.Assert.*;

import java.util.Map;

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

	@Test
	public void testAdd() {
		dictionary.add(",");
		dictionary.add("is");
		dictionary.add("is");
		String w = "";
		dictionary.add(w);
		dictionary.add(w);
		dictionary.add(w);
		Map<Long, Integer> word2int = dictionary.getWord2int();
		assertEquals(3, dictionary.getWords().get(word2int.get(dictionary.find(w))).count);
		assertEquals(2, dictionary.getWords().get(word2int.get(dictionary.find("is"))).count);
		assertEquals(1, dictionary.getWords().get(word2int.get(dictionary.find(","))).count);
	}
}
