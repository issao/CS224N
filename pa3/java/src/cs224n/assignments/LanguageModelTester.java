package cs224n.assignments;

import cs224n.util.Counter;
import cs224n.langmodel.LanguageModel;
import cs224n.langmodel.EmpiricalUnigramLanguageModel;
import cs224n.util.CommandLineUtils;

import java.io.*;
import java.util.*;

/**
 * @author Dan Klein, Bill MacCartney
 */
public class LanguageModelTester {

  // =======================================================================

  /**
   * An edit distance is a measure of difference between two sentences.  We
   * assume that one sentence can be transformed into another through a
   * sequence of INSERT, DELETE, and SUBSTITUTE operations, each costing 1
   * unit.  For example, the sentence "The quick brown fox jumped over the
   * lazy dog." can be transformed into the sentence "The fox tripped over
   * the fat lazy dog." using 2 DELETEs, 1 SUBSTITUTE, and 1 INSERT, for a
   * cost of 4.  In general, given two sentences, there are multiple
   * sequences of operations which transform the first sentence into the
   * second; the edit distance is defined to be the cost of the
   * <i>cheapest</i> such sequence.
   */
  static class EditDistance {

    static double INSERT_COST = 1.0;
    static double DELETE_COST = 1.0;
    static double SUBSTITUTE_COST = 1.0;

    private double[][] initialize(double[][] d) {
      for (int i = 0; i < d.length; i++) {
        for (int j = 0; j < d[i].length; j++) {
          d[i][j] = Double.NaN;
        }
      }
      return d;
    }

    /**
     * Returns the edit distance between two sentences.  Operates by
     * calling a recursive (dynamic programming) helper method.
     */
    public double getDistance(List firstList, List secondList) {
      double[][] bestDistances =
        initialize(new double[firstList.size() + 1][secondList.size() + 1]);
      return getDistance(firstList, secondList, 0, 0, bestDistances);
    }

    /**
     * Computes the edit distance between a suffix of the first list and a
     * suffix of the second listusing dynamic programming.  The best
     * distances for small suffixes are computed first and memoized in the
     * bestDistances table, which is passed around through recursive calls.
     * Edit distances for longer suffixes are computed in terms of edit
     * distances for shorter suffixes by examining alternative operations.
     */
    private double getDistance(List firstList,
                               List secondList,
                               int firstPosition,
                               int secondPosition,
                               double[][] bestDistances) {
      
      // if either suffix has "negative" length...
      if (firstPosition > firstList.size() || secondPosition > secondList.size())
        return Double.POSITIVE_INFINITY;

      // if both suffixes are null, distance is 0 (base case for recursion)
      if (firstPosition == firstList.size() && secondPosition == secondList.size())
        return 0.0;

      // if this distance has not yet been memoized in table...
      if (Double.isNaN(bestDistances[firstPosition][secondPosition])) {

        // start distance high and try alternative ways to reduce it...
        double distance = Double.POSITIVE_INFINITY;

        distance =                      // insert a word?
          Math.min(distance, INSERT_COST +
                   getDistance(firstList, secondList, 
                               firstPosition + 1, secondPosition,
                               bestDistances));
        distance =                      // delete a word?
          Math.min(distance, DELETE_COST +
                   getDistance(firstList, secondList, 
                               firstPosition, secondPosition + 1, 
                               bestDistances));
        distance =                      // substitute a word?
          Math.min(distance, SUBSTITUTE_COST +
                   getDistance(firstList, secondList,
                               firstPosition + 1, secondPosition + 1,
                               bestDistances));
        
        // if first words match, no cost incurred here
        if (firstPosition < firstList.size() && secondPosition < secondList.size()) {
          if (firstList.get(firstPosition).equals(secondList.get(secondPosition))) {
            distance = 
              Math.min(distance, 
                       getDistance(firstList, secondList,
                                   firstPosition + 1, secondPosition + 1,
                                   bestDistances));
          }
        }

        // memoize for future reference
        bestDistances[firstPosition][secondPosition] = distance;
      }
      return bestDistances[firstPosition][secondPosition];
    }
  }

  // =======================================================================

  /** A <code>Sentences</code> object is a <code>Collection</code> of
   *  sentences backed by a text file containing one sentence per line.  It
   *  provides an iterator over the sentences.  Because the collection is
   *  disk-backed, any operation other than iterating is likely to be slow.
   */
  static class Sentences extends AbstractCollection<List<String>> {

    /** 
     * An <code>Iterator</code> wrapped around a
     * <code>BufferedReader</code>.  Each call to <code>next()</code> reads
     * a line, lowercases it, splits it (on whitespace) into words, and
     * returns a list of the words.
     */
    static class SentenceIterator implements Iterator<List<String>> {

      BufferedReader reader;

      public boolean hasNext() {
        try {
          return reader.ready();
        } catch (IOException e) {
          return false;
        }
      }

      public List<String> next() {
        try {
          String line = reader.readLine();
          String[] words = line.split("\\s+");
          List<String> sentence = new ArrayList<String>();
          for (int i = 0; i < words.length; i++) {
            String word = words[i];
            sentence.add(word.toLowerCase());
          }
          return sentence;
        } catch (IOException e) {
          throw new NoSuchElementException();
        }
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }

      public SentenceIterator(BufferedReader reader) {
        this.reader = reader;
      }

    } // end SentenceIterator

    String filename;

    /** Returns an iterator over the sentences in this collection. 
     */
    public Iterator<List<String>> iterator() {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        return new SentenceIterator(reader);
      } catch (FileNotFoundException e) {
        throw new RuntimeException("File not found: " + filename);
      }
    }

    /** Returns the numbers of sentences in this collection.  (This
     *  iterates through all sentences, so it may be slow.)
     */
    public int size() {
      int size = 0;
      Iterator i = iterator();
      while (i.hasNext()) {
        size++;
        i.next();
      }
      return size;
    }

    /** Constructs a new sentence collection from the name of the file
     *  containing the sentences.
     */
    public Sentences(String filename) {
      this.filename = filename;
      iterator();                       // causes error to be thrown if file not readable
    }

    /** Takes the name of a file containing sentences and returns a new
     *  <code>SentenceCollection</code> backed by that file.
     */
    public static class Reader {
      static Collection<List<String>> readSentences(String filename)
        throws FileNotFoundException {
        return new Sentences(filename);
      }
    }

  } // end SentenceCollection

  // =======================================================================

  /**
   * Returns the perplexity of the data in the specified sentence
   * collection according to the specified language model.  The perplexity
   * is defined to be 2 to the power of the cross entropy, which in turn is
   * defined as the negative of the average (over the dataset) of the log
   * (base 2) of the probability, according to the model, of each datum.
   * Lower perplexity indicates a better fit.
   */
  static double computePerplexity(LanguageModel languageModel, 
                                  Collection<List<String>> sentences) {
    double logProbability = 0.0;
    double numSymbols = 0.0;
    for (List<String> sentence : sentences) {
      logProbability += 
        Math.log(languageModel.getSentenceProbability(sentence)) / 
        Math.log(2.0);
      numSymbols += sentence.size();
    }
    double avgLogProbability = logProbability / numSymbols;
    double perplexity = Math.pow(0.5, avgLogProbability);
    return perplexity;
  }

  /**
   * Computes the word error rate obtained using the specified language
   * model to help predict correct answers to the specified list of HUB
   * speech recognition problems.  Each HUB problem includes a correct
   * answer, a set of candidate answers, and scores from the acoustic model
   * for each of those candidates.  Here we combine the score from the
   * acoustic model with the score from the language model, select the
   * candidate answer with the highest overall score, and report the edit
   * distance (roughly, the number of words it got wrong -- see above)
   * between the selected answer and the correct answer.  (If multiple
   * candidate answers tie for the best score, we report their average edit
   * distance from the correct answer.)
   */
  static double computeWordErrorRate(LanguageModel languageModel, 
                                     List<HUBProblem> hubProblems) {
    double totalDistance = 0.0;
    double totalWords = 0.0;
    EditDistance editDistance = new EditDistance();
    for (HUBProblem hubProblem : hubProblems) {
      List<String> correctSentence = hubProblem.getCorrectSentence();
      List<String> bestGuess = null;
      double bestScore = Double.NEGATIVE_INFINITY;
      double numWithBestScores = 0.0;
      double distanceForBestScores = 0.0;
      for (List<String> guess : hubProblem.getNBestSentences()) {
        double score = 
          Math.log(languageModel.getSentenceProbability(guess)) +
          (hubProblem.getAcousticScore(guess) / 16.0);
        double distance = editDistance.getDistance(correctSentence, guess);
        if (score == bestScore) {
          numWithBestScores += 1.0;
          distanceForBestScores += distance;
        }
        if (score > bestScore || bestGuess == null) {
          bestScore = score;
          bestGuess = guess;
          distanceForBestScores = distance;
          numWithBestScores = 1.0;
        }
      }
      totalDistance += distanceForBestScores / numWithBestScores;
      totalWords += correctSentence.size();
    }
    return totalDistance / totalWords;
  }

  /**
   * Computes a lower bound for the word error rate by assuming that, for
   * each HUB speech recognition problem, the candidate answer with the
   * smallest edit distance to the correct answer is selected.  The average
   * edit distance between the selected answer and the correct answer over
   * all HUB problems is returned.
   */
  static double computeWordErrorRateLowerBound(List<HUBProblem> hubProblems) {
    double totalDistance = 0.0;
    double totalWords = 0.0;
    EditDistance editDistance = new EditDistance();
    for (HUBProblem hubProblem : hubProblems) {
      List<String> correctSentence = hubProblem.getCorrectSentence();
      double bestDistance = Double.POSITIVE_INFINITY;
      for (List<String> guess : hubProblem.getNBestSentences()) {
        double distance = editDistance.getDistance(correctSentence, guess);
        if (distance < bestDistance)
          bestDistance = distance;
      }
      totalDistance += bestDistance;
      totalWords += correctSentence.size();
    }
    return totalDistance / totalWords;
  }

  /**
   * Computes an upper bound for the word error rate by assuming that, for
   * each HUB speech recognition problem, the candidate answer with the
   * greatest edit distance to the correct answer is selected.  The average
   * edit distance between the selected answer and the correct answer over
   * all HUB problems is returned.
   */
  static double computeWordErrorRateUpperBound(List<HUBProblem> hubProblems) {
    double totalDistance = 0.0;
    double totalWords = 0.0;
    EditDistance editDistance = new EditDistance();
    for (HUBProblem hubProblem : hubProblems) {
      List<String> correctSentence = hubProblem.getCorrectSentence();
      double worstDistance = Double.NEGATIVE_INFINITY;
      for (List<String> guess : hubProblem.getNBestSentences()) {
        double distance = editDistance.getDistance(correctSentence, guess);
        if (distance > worstDistance)
          worstDistance = distance;
      }
      totalDistance += worstDistance;
      totalWords += correctSentence.size();
    }
    return totalDistance / totalWords;
  }

  /**
   * Computes the word error rate for a model which guesses randomly.  For
   * each HUB speech recognition problem, a random candidate answer is
   * selected.  The average edit distance between the selected answer and
   * the correct answer over all HUB problems is returned.
   */
  static double computeWordErrorRateRandomChoice(List<HUBProblem> hubProblems) {
    double totalDistance = 0.0;
    double totalWords = 0.0;
    EditDistance editDistance = new EditDistance();
    for (HUBProblem hubProblem : hubProblems) {
      List<String> correctSentence = hubProblem.getCorrectSentence();
      double sumDistance = 0.0;
      double numGuesses = 0.0;
      for (List<String> guess : hubProblem.getNBestSentences()) {
        double distance = editDistance.getDistance(correctSentence, guess);
        sumDistance += distance;
        numGuesses += 1.0;
      }
      totalDistance += sumDistance / numGuesses;
      totalWords += correctSentence.size();
    }
    return totalDistance / totalWords;
  }

  /** 
   * Takes a list of HUB speech recognition problems, extracts the correct
   * answer from each problem, and returns a collection of the correct
   * sentences.
   */
  static Collection<List<String>> getCorrectSentences(List<HUBProblem> hubProblems) {
    Collection<List<String>> correctSentences = new ArrayList<List<String>>();
    for (HUBProblem hubProblem : hubProblems) {
      correctSentences.add(hubProblem.getCorrectSentence());
    }
    return correctSentences;
  }

  /**
   * Takes a collection of sentences and returns a <code>Set</code>
   * containing all the words in that collection.
   */
  static Set<String> extractVocabulary(Collection<List<String>> sentences) {
    Set<String> vocabulary = new HashSet<String>();
    for (List<String> sentence : sentences) {
      for (String word : sentence) {
        vocabulary.add(word);
      }
    }
    return vocabulary;
  }

  // =======================================================================

  /**
   * The main method loads language model training, validation, and test
   * data files, along with files describing a set of HUB speech
   * recognition problems.  It trains the language model using the training
   * data, and then computes the perplexity of the test data w.r.t. the
   * model, the perplexity of the correct answers to the HUB speech
   * recognition problem w.r.t. to the model, and the word error rate of a
   * joint model which combines the language model with an acoustic model.
   */
  public static void main(String[] args) throws IOException {

    // set up default options ..............................................
    Map<String, String> options = new HashMap<String, String>();
    options.put("-data",      "/afs/ir/class/cs224n/pa1/data");
    options.put("-train",     "treebank-train.sent.txt");
    options.put("-valid",     "treebank-valid.sent.txt");
    options.put("-test",      "treebank-test.sent.txt");
    options.put("-model",     "cs224n.langmodel.EmpiricalUnigramLanguageModel");
    options.put("-hub",       "true");  // run HUB evaluation?
    options.put("-baselines", "true");  // compute HUB WER baselines?
    options.put("-generate",  "true");  // generate some sentences?

    // let command-line options supersede defaults .........................
    options.putAll(CommandLineUtils.simpleCommandLineParser(args));
    System.out.println("LanguageModelTester options:");
    for (Map.Entry<String, String> entry: options.entrySet()) {
      System.out.printf("  %-12s: %s%n", entry.getKey(), entry.getValue());
    }
    System.out.println();

    // set up file locations ...............................................
    String dataPath  = options.get("-data");
    String trainFile = dataPath + "/" + options.get("-train");
    String validFile = dataPath + "/" + options.get("-valid");
    String testFile  = dataPath + "/" + options.get("-test");
    String hubPath   = dataPath + "/hub";

    // load sentence data ..................................................
    Collection<List<String>> trainSentences = Sentences.Reader.readSentences(trainFile);
    System.out.println("Training data will be read from " + trainFile);
    // Collection<List<String>> validSentences = Sentences.Reader.readSentences(validFile);
    // System.out.println("Validation data will be read from " + validFile);
    Collection<List<String>> testSentences = Sentences.Reader.readSentences(testFile);
    System.out.println("Testing  data will be read from " + testFile + "\n");

    // load HUB speech recognition problems ................................
    List<HUBProblem> hubProblems = null;
    if ("true".equals(options.get("-hub"))) {

      System.out.print("Extracting training vocab from " + trainFile + " ...");
      Set<String> vocab = extractVocabulary(trainSentences);
      System.out.println();
      System.out.println("Training vocabulary has size " + vocab.size() + "\n");

      System.out.print("Loading HUB problems from " + hubPath + " ...");
      hubProblems = HUBProblem.Reader.readHUBProblems(hubPath, vocab);
      System.out.println();
      if (hubProblems.size() == 0) {
        System.out.println("WARNING: failed to read HUB problems -- training vocab too small?");
      } else {
        System.out.println("Read " + hubProblems.size() + " HUB problems");
      }
    }
    
    // construct model, using reflection ...................................
    System.out.println();
    LanguageModel model;
    try {
      Class modelClass = Class.forName(options.get("-model"));
      model = (LanguageModel) modelClass.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    System.out.println("Created model: " + model);

    // train model .........................................................
    System.out.print("Training model" +
                     // trainSentences.size() is slow, because disk-backed!!!
                     // " on " + trainSentences.size() + " sentences" +
                     " from " + trainFile + " ... ");
    model.train(trainSentences);
    System.out.println("done\n");

    // evaluate on training and test data ..................................
    System.out.printf("%-30s %10.4f %n", "Training set perplexity:", 
                      computePerplexity(model, trainSentences));
    System.out.printf("%-30s %10.4f %n", "Test set perplexity:", 
                      computePerplexity(model, testSentences));

    // evaluate on HUB data ................................................
    if ("true".equals(options.get("-hub"))) {
      System.out.printf("%-30s %10.4f %n", "HUB Perplexity:",
                        computePerplexity(model, getCorrectSentences(hubProblems)));
      System.out.printf("%-30s %10.4f %n", "HUB Word Error Rate:", 
                        computeWordErrorRate(model, hubProblems));
      if ("true".equals(options.get("-baselines"))) {
        System.out.println();
        System.out.println("Word Error Rate Baselines:");
        System.out.printf("  %-28s %10.4f %n", "Best Path:", 
                          computeWordErrorRateLowerBound(hubProblems));
        System.out.printf("  %-28s %10.4f %n", "Worst Path:", 
                          computeWordErrorRateUpperBound(hubProblems));
        System.out.printf("  %-28s %10.4f %n", "Avg Path:", 
                          computeWordErrorRateRandomChoice(hubProblems));
      }
    }
    
    // generate sentences from model .......................................
    if ("true".equals(options.get("-generate"))) {
      System.out.println();
      System.out.println("Generated sentences:");
      for (int i = 0; i < 10; i++) {
        System.out.println("  " + model.generateSentence());
      }
    }

  }
  
}
