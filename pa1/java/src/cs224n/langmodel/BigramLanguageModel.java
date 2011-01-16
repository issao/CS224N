package cs224n.langmodel;

import cs224n.util.Counter;
import cs224n.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A dummy language model -- uses empirical unigram counts, plus a single
 * ficticious count for unknown words.  (That is, we pretend that there is
 * a single unknown word, and that we saw it just once during training.)
 *
 * @author Dan Klein
 */
public class BigramLanguageModel implements LanguageModel {

  private static final String START = "<S>";
  private static final String STOP = "</S>";
  
  private Counter<String> startWordCounter;
  private Counter<Pair<String,String>> pairCounter;
  private double total;


  // -----------------------------------------------------------------------

  /**
   * Constructs a new, empty unigram language model.
   */
  public BigramLanguageModel() {
    startWordCounter = new Counter<String>();
    pairCounter = new Counter<Pair<String, String>>();
    total = Double.NaN;
  }

  /**
   * Constructs a unigram language model from a collection of sentences.  A
   * special stop token is appended to each sentence, and then the
   * frequencies of all words (including the stop token) over the whole
   * collection of sentences are compiled.
   */
  public BigramLanguageModel(Collection<List<String>> sentences) {
    this();
    train(sentences);
  }


  // -----------------------------------------------------------------------

  /**
   * Constructs a unigram language model from a collection of sentences.  A
   * special stop token is appended to each sentence, and then the
   * frequencies of all words (including the stop token) over the whole
   * collection of sentences are compiled.
   */
  public void train(Collection<List<String>> sentences) {
    for (List<String> sentence : sentences) {
      List<String> stoppedSentence = new ArrayList<String>(sentence);
      stoppedSentence.add(0, START);
      stoppedSentence.add(STOP);
      for (int i = 1; i < stoppedSentence.size(); i++) {
        startWordCounter.incrementCount(stoppedSentence.get(i - 1), 1.0);
        pairCounter.incrementCount(new Pair(stoppedSentence.get(i - 1), stoppedSentence.get(i)), 1.0);
      }
    }
    total = pairCounter.totalCount();
  }


  // -----------------------------------------------------------------------

  private double getWordProbability(String previousWord, String word) {
    return pairCounter.getCount(new Pair(previousWord, word)) / startWordCounter.getCount(previousWord);
  }

  /**
   * Returns the probability, according to the model, of the word specified
   * by the argument sentence and index.  Smoothing is used, so that all
   * words get positive probability, even if they have not been seen
   * before.
   */
  public double getWordProbability(List<String> sentence, int index) {
    String firstWord;
    if (index == 0) {
      firstWord = START;
    } else {
      firstWord = sentence.get(index - 1);
    }
    String secondWord = sentence.get(index);
    return getWordProbability(firstWord, secondWord); 
  }

  /**
   * Returns the probability, according to the model, of the specified
   * sentence.  This is the product of the probabilities of each word in
   * the sentence (including a final stop token).
   */
  public double getSentenceProbability(List<String> sentence) {
    List<String> stoppedSentence = new ArrayList<String>(sentence);
    stoppedSentence.add(STOP);
    double probability = 1.0;
    for (int index = 0; index < stoppedSentence.size(); index++) {
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
   for (String word : startWordCounter.keySet()) {
     double sample = Math.random();
     // We expect to check ~20 distributions
     if (sample < 20.0 / startWordCounter.size()) {
       checked++;
	     for (String secondWord : startWordCounter.keySet()) {
         sum += getWordProbability(word, secondWord);
       }
       sum += getWordProbablity(word, STOP);
	  }
	}
  return sum / checked;
  }
  
  /**
   * Returns a random word sampled according to the model.  A simple
   * "roulette-wheel" approach is used: first we generate a sample uniform
   * on [0, 1]; then we step through the vocabulary eating up probability
   * mass until we reach our sample.
   */
  public String generateWord(String previousWord) {
    double sample = Math.random();
    double sum = 0.0;
    // This might be simpler if instead of startWordCounter, we had a list of all words. This leaves no room for unknown.
    for (String word : startWordCounter.keySet()) {
      sum += getWordProbability(previousWord, word);
      if (sum > sample) {
        return word;
      }
    }
    return STOP;   // a little probability mass was reserved for unknowns
  }

  /**
   * Returns a random sentence sampled according to the model.  We generate
   * words until the stop token is generated, and return the concatenation.
   */
  public List<String> generateSentence() {
    List<String> sentence = new ArrayList<String>();
    String word = generateWord(START);
    while (!word.equals(STOP)) {
      sentence.add(word);
      word = generateWord(word);
    }
    return sentence;
  }

}


