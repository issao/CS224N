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
   * Returns the perplexity of the data in the specified sentence collection
   * according to the specified language model. The perplexity is defined to be
   * 2 to the power of the cross entropy, which in turn is defined as the
   * negative of the average (over the dataset) of the log (base 2) of the
   * probability, according to the model, of each datum. Lower perplexity
   * indicates a better fit.
   */
  public static double computePerplexity(LanguageModel languageModel,
      Collection<List<String>> sentences) {
    double logProbability = 0.0;
    double numSymbols = 0.0;
    for (List<String> sentence : sentences) {
      double sentenceLogProbability = languageModel
          .getSentenceProbability(sentence);
      logProbability += sentenceLogProbability;
      numSymbols += sentence.size();
    }
    assert numSymbols > 0;
    double avgLogProbability = logProbability / numSymbols;
    double perplexity = Math.pow(0.5, avgLogProbability);
    return perplexity;
  }

  // =======================================================================

  /**
   * The main method loads language model training, validation, and test data
   * files, along with files containing a set of jumbled sentences from the
   * Enron corpus. It trains the language model using the training data, and
   * then computes the perplexity of the test data w.r.t. the model, the
   * perplexity of the correct answers to the Enron problems w.r.t. to the
   * model, and the word error rate of unscrambling the jumbled sentences.
   * 
   * @throws ClassNotFoundException
   */
  public static void main(String[] args) throws IOException,
      ClassNotFoundException {

    // set up default options ..............................................
    Map<String, String> options = new HashMap<String, String>();
    options.put("-data", "/afs/ir/class/cs224n/pa1/data");
    options.put("-train", "europarl-train.sent.txt");
    options.put("-valid", "europarl-validate.sent.txt");
    options.put("-model",
        "cs224n.langmodel.ZipfChimeraInterpolatedTriGramModel");
    options.put("-generate", "true"); // generate some sentences?
    options.put("-check", "true"); // check probabilities sum to 1

    // let command-line options supersede defaults .........................
    options.putAll(CommandLineUtils.simpleCommandLineParser(args));
    System.out.println("LanguageModelTester options:");
    for (Map.Entry<String, String> entry : options.entrySet()) {
      System.out.printf("  %-12s: %s%n", entry.getKey(), entry.getValue());
    }
    System.out.println();

    // set up file locations ...............................................
    String dataPath = options.get("-data");
    String trainFile = dataPath + "/" + options.get("-train");
    String validFile = dataPath + "/" + options.get("-valid");

    // load sentence data ..................................................
    System.out.println("Training data will be read from " + trainFile);
    Collection<List<String>> trainSentences = Sentences.Reader
        .readSentences(trainFile);
    System.out.println("Validation data will be read from " + validFile);
    Collection<List<String>> validSentences = Sentences.Reader
        .readSentences(validFile);
    String serialName = "models/" + options.get("-model") + ":"
        + options.get("-train") + ":" + options.get("-valid");

    // construct model, using reflection ...................................
    System.out.println();
    LanguageModel model;
    if ("true".equals(options.get("-loadserial"))) {
      System.out.print("Deserializing model [" + serialName + "] ...");
      long now = System.currentTimeMillis();
      FileInputStream fis = new FileInputStream(serialName);
      ObjectInputStream in = new ObjectInputStream(fis);
      model = (LanguageModel) in.readObject();
      in.close();
      System.out
          .println(" Done! " + (System.currentTimeMillis() - now) + "ms ");

    } else {
      model = trainModel(options, trainFile, serialName, trainSentences,
          validSentences);
    }

    // check if the probability distribution of the model sums up properly
    if ("true".equals(options.get("-check"))) {
      double modelsum = model.checkModel();
      System.err.println("Checking model " + model + "...");
      System.err.println("checkModel() returns " + modelsum);
      if (Math.abs(1.0 - modelsum) > 1e-6) {
        System.err.println("WARNING: " + model + " does not sum up to one.");
        System.err
            .println("         Check your LM implementaion, or your checkModel() method.");
      } else {
        System.err.println("GOOD!");
      }
      System.err.println();
    }

    // evaluate on training and test data ..................................
    NumberFormat nf = new DecimalFormat("0.0000");
    System.out.printf("%-30s", "Training set perplexity: ");
    System.out.println(nf.format(computePerplexity(model, trainSentences)));

    System.out.printf("%-30s", "Validation set perplexity: ");
    System.out.println(nf.format(computePerplexity(model, validSentences)));

    // generate sentences from model .......................................
    if (options.get("-test") != null) {
      String testParam = options.get("-test");
      for (String oneTest : testParam.split(",")) {
        String testFile = dataPath + "/" + oneTest;
        System.out
            .println("Testing data will be read from  " + testFile + "\n");
        Collection<List<String>> testSentences = Sentences.Reader
            .readSentences(testFile);
        System.out.printf("%-30s", "Test set perplexity: ");
        System.out.println(nf.format(computePerplexity(model, testSentences)));

        if ("true".equals(options.get("-generate"))) {
          System.out.println();
          System.out.println("Generated sentences:");
          for (int i = 0; i < 10; i++) {
            List<String> sentence = model.generateSentence();
            System.out.println("  " + sentence);
            if (model instanceof NGram) {
              // SentencePrinter.print(sentence, (NGram) model);
            }
          }
        }
      }
    }
  }

  private static LanguageModel trainModel(Map<String, String> options,
      String trainFile, String serialName,
      Collection<List<String>> trainSentences,
      Collection<List<String>> validSentences) throws FileNotFoundException,
      IOException {
    LanguageModel model;
    try {
      @SuppressWarnings("rawtypes")
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
    long startTrain = System.currentTimeMillis();
    model.train(trainSentences);
    System.out.println("done\n");

    // tune model....
    if (model instanceof TunableModel) {
      System.out.println("Tuning model...");
      ((TunableModel) model).tune(validSentences);
      System.out.println("done\n");
    }

    System.out.println("Training done! "
        + (System.currentTimeMillis() - startTrain) + "ms ");

    if ("true".equals(options.get("-serialize"))) {
      System.out.print("Serializing model [" + serialName + "] ...");
      long now = System.currentTimeMillis();
      FileOutputStream fos = new FileOutputStream(serialName);
      ObjectOutputStream out = new ObjectOutputStream(fos);
      out.writeObject(model);
      out.close();
      System.out
          .println(" Done! " + (System.currentTimeMillis() - now) + "ms ");
    }
    return model;
  }

}
