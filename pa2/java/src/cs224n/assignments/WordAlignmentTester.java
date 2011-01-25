package cs224n.assignments;

import cs224n.util.*;
import cs224n.wordaligner.*;
import cs224n.io.IOUtils;
import cs224n.langmodel.LanguageModel;

import java.util.*;
import java.io.*;

/**
 * Harness for testing word-level alignments.  The code is hard-wired for the
 * aligment source to be English and the alignment target to be French (recall
 * that's the direction for translating INTO English in the noisy channel
 * model).
 *
 * Your project will implement several methods of word-to-word alignment.
 *
 * @author Dan Klein
 */
public class WordAlignmentTester {

    public static final String ENGLISH = "english";
    public static final String FRENCH = "french";
    public static final String GERMAN = "german";
    public static final String SPANISH = "spanish";
    public static final String ENGLISH_EXT = "e";
    public static final String FRENCH_EXT = "f";
    public static final String GERMAN_EXT = "g";
    public static final String SPANISH_EXT = "s";
    
    public static final String DATA_PATH = "/afs/ir/class/cs224n/pa2/data/";
    
    static final String ENGLISH_EXTENSION = "e";
      
    static String FRENCH_EXTENSION = "f";
    
  public static String GetLanguageExtension(String language){
      if(language.equalsIgnoreCase(ENGLISH)){ return ENGLISH_EXT; }
      if(language.equalsIgnoreCase(GERMAN)){ return GERMAN_EXT; }
      if(language.equalsIgnoreCase(SPANISH)){ return SPANISH_EXT; }
      if(language.equalsIgnoreCase(FRENCH)){ return FRENCH_EXT; }
      return null;
    }
  public static void SetSourceLanguageExtension(String language){
      FRENCH_EXTENSION = GetLanguageExtension(language);
  }

  public static void main(String[] args) {
    // Parse command line flags and arguments
    Map<String,String> argMap = CommandLineUtils.simpleCommandLineParser(args);

    // Set up default parameters and settings
    String basePath = DATA_PATH;
    int maxTrainingSentences = 0;
    boolean verbose = false;
    String dataset = "mini";
    String model = "baseline";
    String language = FRENCH;
    
    // Update defaults using command line specifications
    if (argMap.containsKey("-path")) {
      basePath = argMap.get("-path");
      System.out.println("Using base path: "+basePath);
    }
    if (argMap.containsKey("-sentences")) {
      maxTrainingSentences = Integer.parseInt(argMap.get("-sentences"));
      System.out.println("Using an additional "+maxTrainingSentences+" training sentences.");
    }
    if (argMap.containsKey("-data")) {
      dataset = argMap.get("-data");
      System.out.println("Running with data: "+dataset);
    } else {
      System.out.println("No data set specified.  Use -data [miniTest, validate, test].");
    }
    if (argMap.containsKey("-model")) {
      model = argMap.get("-model");
      System.out.println("Running with model: "+model);
    } else {
      System.out.println("No model specified.  Use -model modelname.");
    }
    if (argMap.containsKey("-language") && !dataset.equalsIgnoreCase("miniTest")) {
        language = argMap.get("-language");
        System.out.println("Running with language: "+language);
      } else if(!dataset.equalsIgnoreCase("miniTest")){
        System.out.println("No language specified.  Use -language languageName. Using default -> french");
      }
    if (argMap.containsKey("-verbose")) {
      verbose = true;
    }
    
    if(!dataset.equalsIgnoreCase("miniTest")){
        StringBuffer modBasePath = new StringBuffer(basePath+"/"+language);
        basePath = modBasePath.toString();
    }else{
        StringBuffer modBasePath = new StringBuffer(basePath+"/mini");
        basePath = modBasePath.toString();
    }
    SetSourceLanguageExtension(language);
    
    // Read appropriate training and testing sets.
    List<SentencePair> trainingSentencePairs = new ArrayList<SentencePair>();
    if (! dataset.equalsIgnoreCase("miniTest") && maxTrainingSentences > 0)
      trainingSentencePairs = readSentencePairs(basePath+"/training", maxTrainingSentences);
    List<SentencePair> testSentencePairs = new ArrayList<SentencePair>();
    Map<Integer,Alignment> testAlignments = new HashMap<Integer, Alignment>();
    if (dataset.equalsIgnoreCase("test")) {
     // only works for English-French, as corresponding data for other language combinations is not available
      testSentencePairs = readSentencePairs(basePath+"/test", Integer.MAX_VALUE);
      testAlignments = readAlignments(basePath+"/answers/test.wa.nonullalign");
    } else if (dataset.equalsIgnoreCase("validate")) {
     // only works for English-French, as corresponding data for other language combinations is not available
      testSentencePairs = readSentencePairs(basePath+"/trial", Integer.MAX_VALUE);
      testAlignments = readAlignments(basePath+"/trial/trial.wa");
    } else if (dataset.equalsIgnoreCase("miniTest")) {
      testSentencePairs = readSentencePairs(basePath, Integer.MAX_VALUE);
      testAlignments = readAlignments(basePath+"/mini.wa");
    } else {
      throw new RuntimeException("Bad data set mode: "+ dataset+", use test, validate, or miniTest.");
    }
    trainingSentencePairs.addAll(testSentencePairs);

    // Build model
    WordAligner wordAligner = null;
    if (model.equalsIgnoreCase("baseline")) {
      wordAligner = new BaselineWordAligner();
    }
    else{
        try {
            @SuppressWarnings("unchecked")
            Class modelClass = Class.forName(model);
            wordAligner = (WordAligner)modelClass.newInstance();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
        
    // TODO : build other alignment models

    wordAligner.train(trainingSentencePairs);

    // Test model
    test(wordAligner, testSentencePairs, testAlignments, verbose);
  }

  private static void test(WordAligner wordAligner, List<SentencePair> testSentencePairs, Map<Integer, Alignment> testAlignments, boolean verbose) {
    int proposedSureCount = 0;
    int proposedPossibleCount = 0;
    int sureCount = 0;
    int proposedCount = 0;
    for (SentencePair sentencePair : testSentencePairs) {
      Alignment proposedAlignment = wordAligner.alignSentencePair(sentencePair);
      Alignment referenceAlignment = testAlignments.get(sentencePair.getSentenceID());
      if (referenceAlignment == null)
        throw new RuntimeException("No reference alignment found for sentenceID "+sentencePair.getSentenceID());
      if (verbose) System.out.println("Alignment:\n"+Alignment.render(referenceAlignment,proposedAlignment,sentencePair));
      for (int frenchPosition = 0; frenchPosition < sentencePair.getFrenchWords().size(); frenchPosition++) {
        for (int englishPosition = 0; englishPosition < sentencePair.getEnglishWords().size(); englishPosition++) {
          boolean proposed = proposedAlignment.containsSureAlignment(englishPosition, frenchPosition);
          boolean sure = referenceAlignment.containsSureAlignment(englishPosition, frenchPosition);
          boolean possible = referenceAlignment.containsPossibleAlignment(englishPosition, frenchPosition);
          if (proposed && sure) proposedSureCount += 1;
          if (proposed && possible) proposedPossibleCount += 1;
          if (proposed) proposedCount += 1;
          if (sure) sureCount += 1;
        }
      }
    }
    System.out.println("Precision: "+proposedPossibleCount/(double)proposedCount);
    System.out.println("Recall: "+proposedSureCount/(double)sureCount);
    System.out.println("AER: "+(1.0-(proposedSureCount+proposedPossibleCount)/(double)(sureCount+proposedCount)));
  }


  // BELOW HERE IS IO CODE

  private static Map<Integer, Alignment> readAlignments(String fileName) {
    Map<Integer,Alignment> alignments = new HashMap<Integer, Alignment>();
    //String encoding = "iso-9959-1";
    try {
        //BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), encoding));
        BufferedReader in = new BufferedReader(new FileReader(fileName));
        while (in.ready()) {
        String line = in.readLine();
        String[] words = line.split("\\s+");
        if (words.length != 4)
          throw new RuntimeException("Bad alignment file "+fileName+", bad line was "+line);
        Integer sentenceID = Integer.parseInt(words[0]);
        Integer englishPosition = Integer.parseInt(words[1])-1;
        Integer frenchPosition = Integer.parseInt(words[2])-1;
        String type = words[3];
        Alignment alignment = alignments.get(sentenceID);
        if (alignment == null) {
          alignment = new Alignment();
          alignments.put(sentenceID, alignment);
        }
        alignment.addAlignment(englishPosition, frenchPosition, type.equals("S"));
      }
      in.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return alignments;
  }

  private static List<SentencePair> readSentencePairs(String path, int maxSentencePairs) {
    List<SentencePair> sentencePairs = new ArrayList<SentencePair>();
    List<String> baseFileNames = getBaseFileNames(path);
    for (String baseFileName : baseFileNames) {
      if (sentencePairs.size() >= maxSentencePairs)
        break;
      sentencePairs.addAll(readSentencePairs(baseFileName));
    }
    return sentencePairs;
  }

  private static List<SentencePair> readSentencePairs(String baseFileName) {
    List<SentencePair> sentencePairs = new ArrayList<SentencePair>();
    String englishFileName = baseFileName + "." + ENGLISH_EXTENSION;
    String frenchFileName = baseFileName + "." + FRENCH_EXTENSION;
    //String encoding = "iso-9959-1";
    try {
      //BufferedReader englishIn = new BufferedReader(new InputStreamReader(new FileInputStream(englishFileName), encoding));
      BufferedReader englishIn = new BufferedReader(new FileReader(englishFileName));
      //BufferedReader frenchIn = new BufferedReader(new InputStreamReader(new FileInputStream(frenchFileName), encoding));
      BufferedReader frenchIn = new BufferedReader(new FileReader(frenchFileName));
      while (englishIn.ready() && frenchIn.ready()) {
        String englishLine = englishIn.readLine();
        String frenchLine = frenchIn.readLine();
        Pair<Integer,List<String>> englishSentenceAndID = readSentence(englishLine);
        Pair<Integer,List<String>> frenchSentenceAndID = readSentence(frenchLine);
        if (! englishSentenceAndID.getFirst().equals(frenchSentenceAndID.getFirst()))
          throw new RuntimeException("Sentence ID confusion in file "+baseFileName+", lines were:\n\t"+englishLine+"\n\t"+frenchLine);
        sentencePairs.add(new SentencePair(englishSentenceAndID.getFirst(), baseFileName, englishSentenceAndID.getSecond(), frenchSentenceAndID.getSecond()));
      }
      englishIn.close();
      frenchIn.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return sentencePairs;
  }

  private static Pair<Integer, List<String>> readSentence(String line) {
    int id = -1;
    List<String> words = new ArrayList<String>();
    String[] tokens = line.split("\\s+");
    for (int i = 0; i < tokens.length; i++) {
      String token = tokens[i];
      if (token.equals("<s")) continue;
      if (token.equals("</s>")) continue;
      if (token.startsWith("snum=")) {
        String idString = token.substring(5,token.length()-1);
        id = Integer.parseInt(idString);
        continue;
      }
      words.add(token.intern());
    }
    return new Pair<Integer, List<String>>(id, words);
  }

  private static List<String> getBaseFileNames(String path) {
    List<File> englishFiles = IOUtils.getFilesUnder(path, new FileFilter() {
      public boolean accept(File pathname) {
        if (pathname.isDirectory())
          return true;
        String name = pathname.getName();
        return name.endsWith(ENGLISH_EXTENSION);
      }
    });
    List<String> baseFileNames = new ArrayList<String>();
    for (File englishFile : englishFiles) {
      String baseFileName = chop(englishFile.getAbsolutePath(), "."+ENGLISH_EXTENSION);
      baseFileNames.add(baseFileName);
    }
    return baseFileNames;
  }

  private static String chop(String name, String extension) {
    if (! name.endsWith(extension)) return name;
    return name.substring(0, name.length()-extension.length());
  }

}





