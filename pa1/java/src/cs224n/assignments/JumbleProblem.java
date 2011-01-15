package cs224n.assignments;

import java.util.*;
import java.io.*;

/**
 * Represents a Jumble text problem.  Each problem has a correct sentence, 
 * and a list of jumbled sentences. <p/>
 *
 * The main purpose of this class is the enclosed Reader class, which
 * provides a front end to a directory full of Jumble data. <p/>
 *
 * @author Nate Chambers (adapted from HUBProblem by Dan Klein, Bill MacCartney)
 */
public class JumbleProblem {

  private static final int VERBOSE = 0;

  private List<String> correctSentence;
  private List<List<String>> nBestSentences;

  /**
   * Constructor
   */
  public JumbleProblem(List<String> correctSentence,
		       List<List<String>> nBestSentences) {
    this.correctSentence = correctSentence;
    this.nBestSentences = nBestSentences;
  }

  /**
   * @return The original correct ordering of the sentence.
   */
  public List<String> getCorrectSentence() {
    return correctSentence;
  }

  /**
   * @return The list of jumbled sentences.
   */
  public List<List<String>> getNBestSentences() {
    return nBestSentences;
  }

  // =======================================================================

  static class Reader {

    public static List<JumbleProblem> readJumbleProblems(String path) 
      throws IOException {
      List<JumbleProblem> jumbleProblems = new ArrayList<JumbleProblem>();
      BufferedReader correctSentenceReader = open(path + "/gold");

      // Read in all of the correct sentences for each jumble problem
      Vector<List<String>> correctSentences = readCorrectSentences(correctSentenceReader);

      // Loop over the files and build a JumbleProblem for each
      for (int i = 0; i < correctSentences.size(); i++ ) {
	String file = "test" + i;
        BufferedReader jumbleReader = open(path + File.separator + file);
        List<String> correctSentence = correctSentences.elementAt(i);
        JumbleProblem jumbleProblem = 
	  buildJumbleProblem(correctSentence, jumbleReader);

        if (jumbleProblem == null)
          System.err.println("Failed to read problem " + file);
	else jumbleProblems.add(jumbleProblem);

	jumbleReader.close();
      }
      correctSentenceReader.close();
      return jumbleProblems;
    }

    private static JumbleProblem buildJumbleProblem(List<String> correct,
						    BufferedReader jumbleReader) throws IOException {
      List<List<String>> guessList = readJumbles(jumbleReader);

      if (guessList == null || guessList.size() == 0 ) {
	System.err.println("WARNING: failed to construct Jumble problem: guessList is empty");
        return null;
      }
      if (correct == null) {
	System.err.println("WARNING: failed to construct Jumble problem: correctGuess is null");
        return null;
      }

      return new JumbleProblem(correct, guessList);
    }

    private static boolean equalsIgnoreSpaces(List<String> sentence1, List<String> sentence2) {
      StringBuilder sb1 = new StringBuilder();
      StringBuilder sb2 = new StringBuilder();
      for (String word1 : sentence1) {
        sb1.append(word1);
      }
      for (String word2 : sentence2) {
        sb2.append(word2);
      }
      return sb1.toString().equalsIgnoreCase(sb2.toString());
    }

    private static List<List<String>> readJumbles(BufferedReader jumbleReader) throws IOException {
      List<List<String>> jumbleList = new ArrayList<List<String>>();
      while (jumbleReader.ready()) {
        String line = jumbleReader.readLine();
        String[] words = line.split("\\s+");
        List<String> sentence = new ArrayList<String>();
        for (int i = 0; i < words.length; i++) {
          String word = words[i];
          sentence.add(word.toLowerCase());
        }
        jumbleList.add(sentence);
      }
      return jumbleList;
    }

    // Returns a list of filenames (e.g. ".../test3")
    private static List<String> getTestFiles(String path) {
      List<String> fileList = new ArrayList<String>();
      File directory = new File(path);
      File[] files = directory.listFiles();
      for (int i = 0; i < files.length; i++) {
        File file = files[i];
        String fileName = file.getName();
        if (fileName.startsWith("test"))  // testX
	  fileList.add(fileName);
      }
      return fileList;
    }

    /**
     * Reads one sentence per line, returns them in a Vector
     */
    private static Vector<List<String>> readCorrectSentences(BufferedReader reader)
      throws IOException {
      Vector<List<String>> correctSentences = new Vector();
      while (reader.ready()) {
        String line = reader.readLine();
        String[] words = line.split("\\s+");
        List<String> sentence = new ArrayList<String>();
        for (int i = 0; i < words.length; i++) {
          String word = words[i];
          sentence.add(word.toLowerCase());
        }
        correctSentences.add(sentence);
      }
      return correctSentences;
    }

    private static BufferedReader open(String fileName) throws FileNotFoundException {
      return new BufferedReader(new FileReader(fileName));
    }
  }
}
