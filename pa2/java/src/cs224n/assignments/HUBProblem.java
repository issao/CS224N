package cs224n.assignments;

import java.util.*;
import java.io.*;

/**
 * Represents a HUB speech recognition problem.  Each problem has a correct
 * sentence, a list of n-best guess sentences, and a score for each of the
 * guesses. <p/>
 *
 * The main purpose of this class is the enclosed Reader class, which
 * provides a front end to a directory full of HUB data. <p/>
 *
 * @author Dan Klein, Bill MacCartney
 */
public class HUBProblem {

  private static final int VERBOSE = 0;

  private List<String> correctSentence;
  private List<List<String>> nBestSentences;
  private Map<List<String>, Double> acousticScores;

  public HUBProblem(List<String> correctSentence,
                    List<List<String>> nBestSentences,
                    Map<List<String>, Double> acousticScores) {
    this.correctSentence = correctSentence;
    this.nBestSentences = nBestSentences;
    this.acousticScores = acousticScores;
  }

  public List<String> getCorrectSentence() {
    return correctSentence;
  }

  public List<List<String>> getNBestSentences() {
    return nBestSentences;
  }

  public double getAcousticScore(List<String> sentence) {
    return acousticScores.get(sentence);
  }

  // =======================================================================

  static class Reader {

    public static List<HUBProblem> readHUBProblems(String path, Set vocabulary) 
      throws IOException {
      List<HUBProblem> hubProblems = new ArrayList<HUBProblem>();
      BufferedReader correctSentenceReader = open(path + "/REF.HUB1");
      Map<String, List<String>> correctSentenceMap = 
        readCorrectSentences(correctSentenceReader);
      List<String> prefixList = getPrefixes(path);
      if (VERBOSE > 0) 
        System.err.println("There are " + prefixList.size() + " HUB problems to read");
      for (String prefix : prefixList) {
        if (VERBOSE > 0) System.err.println("Trying to read problem " + prefix);
        BufferedReader guessReader = open(path + "/" + prefix);
        BufferedReader scoreReader = open(path + "/" + prefix + ".acc");
        List<String> correctSentence = correctSentenceMap.get(prefix);
        HUBProblem hubProblem = 
          buildHUBProblem(correctSentence, guessReader, scoreReader, vocabulary);
        if (hubProblem == null) {
          if (VERBOSE > 0) System.err.println("Failed to read problem " + prefix);
        } else {
          hubProblems.add(hubProblem);
          if (VERBOSE > 0) System.err.println("Read problem " + prefix);
        }
	guessReader.close();
	scoreReader.close();
      }
      correctSentenceReader.close();
      return hubProblems;
    }

    private static HUBProblem buildHUBProblem(List<String> correct,
                                              BufferedReader guessReader,
                                              BufferedReader scoreReader,
                                              Set vocabulary) throws IOException {

      List<Double> scoreList = readScores(scoreReader);
      List<List<String>> guessList = readGuesses(guessReader);
      List<List<String>> uniqueSentenceList = new ArrayList<List<String>>();
      Map<List<String>, Double> sentencesToScores = new HashMap<List<String>, Double>();
      List<String> correctGuess = null;

      for (int i = 0; i < guessList.size(); i++) {
        List<String> guess = guessList.get(i);
        // skip any guesses containing words not in vocabulary
        if (!inVocabulary(guess, vocabulary)) {
          continue;
        }
        Double score = scoreList.get(i);
        Double bestScoreForGuess = sentencesToScores.get(guess);
        if (!sentencesToScores.containsKey(guess)) {
          uniqueSentenceList.add(guess);
          if (equalsIgnoreSpaces(correct, guess)) {
            if (correctGuess != null) {
              System.out.println("WARNING: SPEECH LATTICE ERROR");
            }
            correctGuess = guess;
          }
        }
        if (bestScoreForGuess == null || score > bestScoreForGuess) {
          sentencesToScores.put(guess, score);
        }
      }

      if (uniqueSentenceList.isEmpty()) {
        if (VERBOSE > 1)
          System.err.println("WARNING: failed to construct HUB problem: uniqueSentenceList is empty");
        return null;
      }
      if (correctGuess == null) {
        if (VERBOSE > 1)
          System.err.println("WARNING: failed to construct HUB problem: correctGuess is null");
        return null;
      }

      return new HUBProblem(correctGuess, uniqueSentenceList, sentencesToScores);
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

    private static boolean inVocabulary(List<String> sentence, Set vocabulary) {
      for (String word : sentence) {
        if (!vocabulary.contains(word)) return false;
      }
      return true;
    }

    private static List<List<String>> readGuesses(BufferedReader guessReader) throws IOException {
      List<List<String>> guessList = new ArrayList<List<String>>();
      while (guessReader.ready()) {
        String line = guessReader.readLine();
        String[] words = line.split("\\s+");
        List<String> sentence = new ArrayList<String>();
        for (int i = 0; i < words.length; i++) {
          String word = words[i];
          sentence.add(word.toLowerCase());
        }
        guessList.add(sentence);
      }
      return guessList;
    }

    private static List<Double> readScores(BufferedReader scoreReader) throws IOException {
      List<Double> scoreList = new ArrayList<Double>();
      while (scoreReader.ready()) {
        String line = scoreReader.readLine();
        String[] scoreStrings = line.split("\\s+");
        double totalScore = 0.0;
        for (int i = 0; i < scoreStrings.length; i++) {
          String scoreString = scoreStrings[i];
          totalScore += Double.parseDouble(scoreString);
        }
        scoreList.add(totalScore);
      }
      return scoreList;
    }

    // Returns a list of filename prefixes (e.g. "4oac0201") from the
    // directory of HUB problems at the specified path.
    private static List<String> getPrefixes(String path) {
      List<String> prefixList = new ArrayList<String>();
      Set<String> seenPrefixes = new HashSet<String>();
      File directory = new File(path);
      File[] files = directory.listFiles();
      for (int i = 0; i < files.length; i++) {
        File file = files[i];
        String fileName = file.getName();
        if (fileName.startsWith("RE"))  // REF.HUB1 or README
          continue;
        String prefix = fileName;
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
          prefix = fileName.substring(0, extensionIndex);
        }
        if (!seenPrefixes.contains(prefix)) {
          prefixList.add(prefix);
        }
        seenPrefixes.add(prefix);
      }
      return prefixList;
    }

    private static Map<String, List<String>> readCorrectSentences(BufferedReader reader)
      throws IOException {
      Map<String, List<String>> correctSentenceMap = new HashMap<String, List<String>>();
      while (reader.ready()) {
        String line = reader.readLine();
        String[] words = line.split("\\s+");
        List<String> sentence = new ArrayList<String>();
        for (int i = 0; i < words.length - 1; i++) {
          String word = words[i];
          sentence.add(word.toLowerCase());
        }
        String idToken = words[words.length - 1].toLowerCase();
        String sentenceID = idToken.substring(1, idToken.length() - 1);
        correctSentenceMap.put(sentenceID, sentence);
      }
      return correctSentenceMap;
    }

    private static BufferedReader open(String fileName) throws FileNotFoundException {
      return new BufferedReader(new FileReader(fileName));
    }

  }

}
