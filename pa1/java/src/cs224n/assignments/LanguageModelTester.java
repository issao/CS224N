package cs224n.assignments;

import cs224n.util.*;
import cs224n.langmodel.*;

import java.io.*;
import java.util.*;
import java.text.NumberFormat;
import java.text.DecimalFormat;

/**
 * @author Dan Klein
 * @author Bill MacCartney
 * @author Nate Chambers
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
      //      System.out.println("logp=" + logProbability + " size=" + numSymbols);
    }
    double avgLogProbability = logProbability / numSymbols;
    //    System.out.println("avglogp=" + avgLogProbability);
    double perplexity = Math.pow(0.5, avgLogProbability);
    //    System.out.println("perplexity=" + perplexity);
    return perplexity;
  }

  /**
   * Computes the word error rate obtained using the specified language
   * model to help predict correct answers to the specified list of Jumble
   * Enron problems.  Each problem includes a correct
   * answer and a set of candidate answers.  Here we compute the score from the language model, select the candidate answer with the highest probability, and report the edit
   * distance (roughly, the number of words it got wrong -- see above)
   * between the selected answer and the correct answer.  (If multiple
   * candidate answers tie for the best score, we report their average edit
   * distance from the correct answer.)
   * This also computers the "% correct" score which is the number of sentences
   * you choose exactly correct.
   * @param showGuesses True if you want to print the highest scoring sentences
   * @return A pair of scores: (1) WER and (2) % correct
   */
  static Pair<Double,Double> computeWordErrorRate(LanguageModel languageModel, 
						  List<JumbleProblem> jumbleProblems,
						  boolean showGuesses) {
    double totalDistance = 0.0;
    double totalWords = 0.0;
    double totalWER = 0.0;
    int absoluteCorrect = 0;
    EditDistance editDistance = new EditDistance();

    if( showGuesses ) System.out.println("***Rebuilt Enron Emails***");
    // Loop over each jumbled sentence.
    for (JumbleProblem jProblem : jumbleProblems) {
      List<String> correctSentence = jProblem.getCorrectSentence();
      List<String> bestGuess = null;
      double bestScore = Double.NEGATIVE_INFINITY;
      double numWithBestScores = 0.0;
      double distanceForBestScores = 0.0;
      for (List<String> guess : jProblem.getNBestSentences()) {
        double score = languageModel.getSentenceProbability(guess);
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
      // Debugging, showing the best guess sentences
      if( showGuesses ) {
	for( String token : bestGuess ) System.out.print(token + " ");
	System.out.println();
      }
      // If exactly correct
      if( distanceForBestScores == 0 ) {
	absoluteCorrect++;
      }
      totalDistance += distanceForBestScores / numWithBestScores;
      totalWords += correctSentence.size();
      totalWER += distanceForBestScores / (numWithBestScores * correctSentence.size());
    } // end jumbleproblem loop

    if( showGuesses ) System.out.println("******");

    //    return totalDistance / totalWords;
    return new Pair<Double,Double>(totalWER / (double)jumbleProblems.size(), 
				   (double)absoluteCorrect / (double)jumbleProblems.size());
  }

  /**
   * Computes an upper bound for the word error rate by assuming that, for
   * each Enron problem, the candidate answer with the
   * greatest edit distance to the correct answer is selected.  The average
   * edit distance between the selected answer and the correct answer over
   * all problems is returned.
   */
  static double computeWordErrorRateUpperBound(List<JumbleProblem> problems) {
    double totalDistance = 0.0;
    double totalWords = 0.0;
    EditDistance editDistance = new EditDistance();
    for (JumbleProblem problem : problems) {
      List<String> correctSentence = problem.getCorrectSentence();
      double worstDistance = Double.NEGATIVE_INFINITY;
      for (List<String> guess : problem.getNBestSentences()) {
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
   * each Enron problem, a random candidate answer is
   * selected.  The average edit distance between the selected answer and
   * the correct answer over all problems is returned.
   */
  static double computeWordErrorRateRandomChoice(List<JumbleProblem> problems) {
    double totalDistance = 0.0;
    double totalWords = 0.0;
    EditDistance editDistance = new EditDistance();
    for (JumbleProblem problem : problems) {
      List<String> correctSentence = problem.getCorrectSentence();
      double sumDistance = 0.0;
      double numGuesses = 0.0;
      for (List<String> guess : problem.getNBestSentences()) {
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
   * Takes a list of Enron problems, extracts the correct
   * answer from each problem, and returns a collection of the correct
   * sentences.
   */
  static Collection<List<String>> getCorrectSentences(List<JumbleProblem> jumbleProblems) {
    Collection<List<String>> correctSentences = new ArrayList<List<String>>();
    for (JumbleProblem jProblem : jumbleProblems) {
      correctSentences.add(jProblem.getCorrectSentence());
    }
    return correctSentences;
  }

  // =======================================================================

  /**
   * The main method loads language model training, validation, and test
   * data files, along with files containing a set of jumbled sentences from
   * the Enron corpus.  It trains the language model using the training
   * data, and then computes the perplexity of the test data w.r.t. the
   * model, the perplexity of the correct answers to the Enron problems
   * w.r.t. to the model, and the word error rate of unscrambling the jumbled
   * sentences.
   */
  public static void main(String[] args) throws IOException {

    // set up default options ..............................................
    Map<String, String> options = new HashMap<String, String>();
    options.put("-data",      "/afs/ir/class/cs224n/pa1/data");
    options.put("-train",     "europarl-train.sent.txt");
    options.put("-valid",     "europarl-valid.sent.txt");
    options.put("-test",      "europarl-test.sent.txt");
    options.put("-model",     "cs224n.langmodel.EmpiricalUnigramLanguageModel");
    options.put("-showguesses",       "false");  // show rebuilt Enron emails?
    options.put("-jumble",       "false");  // run Jumble (Enron) evaluation?
    options.put("-baselines", "true");  // compute WER baselines?
    options.put("-generate",  "true");  // generate some sentences?
    options.put("-check",     "true");  // check probabilities sum to 1

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
    //String validFile = dataPath + "/" + options.get("-valid");
    String testFile  = dataPath + "/" + options.get("-test");
    String jumblePath   = dataPath + "/jumble";

    // load sentence data ..................................................
    System.out.println("Training data will be read from " + trainFile);
    Collection<List<String>> trainSentences = Sentences.Reader.readSentences(trainFile);
    // System.out.println("Validation data will be read from " + validFile);
    // Collection<List<String>> validSentences = Sentences.Reader.readSentences(validFile);
    System.out.println("Testing data will be read from  " + testFile + "\n");
    Collection<List<String>> testSentences = Sentences.Reader.readSentences(testFile);

    // load jumbled sentence problems ................................
    List<JumbleProblem> jumbleProblems = null;
    if ("true".equals(options.get("-jumble"))) {

      System.out.print("Loading Jumble problems from " + jumblePath + " ...");
      jumbleProblems = JumbleProblem.Reader.readJumbleProblems(jumblePath);
      System.out.println();
      if (jumbleProblems.size() == 0) {
        System.out.println("WARNING: failed to read Jumble problems");
      } else {
        System.out.println("Read " + jumbleProblems.size() + " Jumble problems");
      }
    }
    
    // construct model, using reflection ...................................
    System.out.println();
    LanguageModel model;
    try {
      @SuppressWarnings("unchecked")
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

    // check if the probability distribution of the model sums up properly
    if ("true".equals(options.get("-check"))) {
      double modelsum = model.checkModel();
      System.err.println("Checking model "+model+"...");
      System.err.println("checkModel() returns "+modelsum);
      if (Math.abs(1.0-modelsum) > 1e-6) {
        System.err.println("WARNING: "+model+" does not sum up to one.");
        System.err.println("         Check your LM implementaion, or your checkModel() method.");
      } else {
        System.err.println("GOOD!");
      }
      System.err.println();
    }
    
    // evaluate on training and test data ..................................
    NumberFormat nf = new DecimalFormat("0.0000");
    System.out.printf("%-30s","Training set perplexity: ");
    System.out.println(nf.format(computePerplexity(model, trainSentences)));
    System.out.printf("%-30s","Test set perplexity: ");
    System.out.println(nf.format(computePerplexity(model, testSentences)));

    // evaluate on Jumble data ................................................
    if ("true".equals(options.get("-jumble"))) {
      System.out.printf("%-30s","Enron Jumble Perplexity: ");
      System.out.println(nf.format(computePerplexity(model, getCorrectSentences(jumbleProblems))));

      // If we want to print the guessed email
      boolean showGuesses = false;
      if ("true".equals(options.get("-showguesses"))) 
	showGuesses = true;

      // Get the WER and % correct scores.
      Pair<Double,Double> results = computeWordErrorRate(model, jumbleProblems, showGuesses);
      System.out.printf("%-30s","Enron Word Error Rate: ");
      System.out.println(nf.format(results.getFirst()));
      System.out.printf("%-30s","Enron Percent Correct: ");
      System.out.println(nf.format(100*results.getSecond()) + "%");

      if ("true".equals(options.get("-baselines"))) {
        System.out.println();
        System.out.println("Enron WER Baselines: ");
        System.out.printf("%-30s"," Worst Path: ");
	System.out.println(nf.format(computeWordErrorRateUpperBound(jumbleProblems)));
        System.out.printf("%-30s"," Random Path: ");
	System.out.println(nf.format(computeWordErrorRateRandomChoice(jumbleProblems)));
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
