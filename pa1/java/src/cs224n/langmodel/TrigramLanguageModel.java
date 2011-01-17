package cs224n.langmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs224n.util.Counter;

/**
 * A dummy language model -- uses empirical unigram counts, plus a single
 * ficticious count for unknown words. (That is, we pretend that there is a
 * single unknown word, and that we saw it just once during training.)
 * 
 * @author Dan Klein
 */
public class TrigramLanguageModel implements LanguageModel {

	private static final String START = "<S>";
	private static final String STOP = "</S>";

	private Set<String> lexicon;
	private Map<List<String>, Counter<String>> uniGram;
	private Map<List<String>, Counter<String>> biGram;
	private Map<List<String>, Counter<String>> triGram;
 
	// -----------------------------------------------------------------------

	/**
	 * Constructs a new, empty unigram language model.
	 */
	public TrigramLanguageModel() {
		uniGram = new HashMap<List<String>, Counter<String>>();
		biGram = new HashMap<List<String>, Counter<String>>();
		triGram = new HashMap<List<String>, Counter<String>>();
		lexicon = new HashSet<String>();
	}

	/**
	 * Constructs a unigram language model from a collection of sentences. A
	 * special stop token is appended to each sentence, and then the frequencies
	 * of all words (including the stop token) over the whole collection of
	 * sentences are compiled.
	 */
	public TrigramLanguageModel(Collection<List<String>> sentences) {
		this();
		train(sentences);
	}

	// -----------------------------------------------------------------------

	/**
	 * Constructs a unigram language model from a collection of sentences. A
	 * special stop token is appended to each sentence, and then the frequencies
	 * of all words (including the stop token) over the whole collection of
	 * sentences are compiled.
	 */
	public void train(Collection<List<String>> sentences) {
		for (List<String> sentence : sentences) {
			List<String> stoppedSentence = new ArrayList<String>(sentence);
			stoppedSentence.add(0, START);
			stoppedSentence.add(0, START);
			stoppedSentence.add(STOP); 
			for (int i = 2; i < stoppedSentence.size(); i++) {
				lexicon.add(stoppedSentence.get(i));
				addNgramCount(stoppedSentence, i, 1, uniGram);
				addNgramCount(stoppedSentence, i, 2, biGram);
				addNgramCount(stoppedSentence, i, 3, triGram);
			}
		}
	}
	
	private void addNgramCount(List<String> sentence, int index, int n,
			Map<List<String>, Counter<String>> ngram) {
		assert index + 1 >= n;
		List<String> prefix = getPrefix(sentence, index, n);
		if (!ngram.containsKey(prefix)) {
			ngram.put(prefix, new Counter<String>());
		}
		ngram.get(prefix).incrementCount(sentence.get(index), 1.0);
	}

	private List<String> getPrefix(List<String> sentence, int index, int n) {
		return sentence.subList(index - n + 1, index);
	}

	// -----------------------------------------------------------------------

	private double getWordProbability(List<String> prefix, String word) {
	  // Only needed when not smoothing
	  if (!triGram.containsKey(prefix)) {
	    return 0;
	  }
		return triGram.get(prefix).getCount(word)
				/ triGram.get(prefix).totalCount();
	}

	/**
	 * Returns the probability, according to the model, of the word specified by
	 * the argument sentence and index. Smoothing is used, so that all words get
	 * positive probability, even if they have not been seen before.
	 */
	public double getWordProbability(List<String> sentence, int index) {
		return getWordProbability(getPrefix(sentence, index, 3),
				sentence.get(index));
	}

	/**
	 * Returns the probability, according to the model, of the specified
	 * sentence. This is the product of the probabilities of each word in the
	 * sentence (including a final stop token).
	 */
	public double getSentenceProbability(List<String> sentence) {
		List<String> stoppedSentence = new ArrayList<String>(sentence);
		stoppedSentence.add(STOP);
		stoppedSentence.add(0, START);
		stoppedSentence.add(0, START);
		double probability = 1.0;
		for (int index = 2; index < stoppedSentence.size(); index++) {
			probability *= getWordProbability(stoppedSentence, index);
		}
		return probability;
	}

	/**
	 * checks if the probability distribution properly sums up to 1
	 */
	public double checkModel() {
		// since this is a unigram model,
		// the event space is everything in the vocabulary (including STOP)
		// and a UNK token
		int checked = 0;
		double sum = 0.0;
		for (List<String> prefix : triGram.keySet()) {
			double sample = Math.random();
			// We expect to check ~20 distributions
			if (sample < 20.0 / triGram.size()) {
				checked++;
				for (String word : lexicon) {
					sum += getWordProbability(prefix, word);
				}
			}
		}
		System.out.println("checked " + checked + " conditional probabilities");
		return sum / checked;
	}

	/**
	 * Returns a random word sampled according to the model. A simple
	 * "roulette-wheel" approach is used: first we generate a sample uniform on
	 * [0, 1]; then we step through the vocabulary eating up probability mass
	 * until we reach our sample.
	 */
	public String generateWord(List<String> prefix) {
		double sample = Math.random();
		double sum = 0.0;
		// This might be simpler if instead of startWordCounter, we had a list
		// of all words. This leaves no room for unknown.
		for (String word : lexicon) {
			sum += getWordProbability(prefix, word);
			if (sum > sample) {
				return word;
			}
		}
		assert false;
		return STOP; // a little probability mass was reserved for unknowns
	}

	/**
	 * Returns a random sentence sampled according to the model. We generate
	 * words until the stop token is generated, and return the concatenation.
	 */
	public List<String> generateSentence() {
		List<String> sentence = new ArrayList<String>();
		List<String> prefix = new ArrayList<String>();
		prefix.add(START);
		prefix.add(START);
		String word;
		do {
		  word = generateWord(prefix);
		  sentence.add(word);
      prefix.add(word);
      prefix.remove(0);
		} while (!word.equals(STOP));
		return sentence;
	}

}
