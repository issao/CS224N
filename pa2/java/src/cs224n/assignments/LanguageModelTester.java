package cs224n.assignments;

import cs224n.util.*;
import cs224n.langmodel.*;

import java.io.*;
import java.util.*;

/**
 * @author Dan Klein, Bill MacCartney
 */
public class LanguageModelTester {

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
