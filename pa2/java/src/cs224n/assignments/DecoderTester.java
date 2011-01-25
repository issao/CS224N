package cs224n.assignments;

import cs224n.util.*;
import cs224n.metrics.*;
import cs224n.wordaligner.*;
import cs224n.langmodel.*;
import cs224n.decoder.*;
import cs224n.io.IOUtils;

import java.util.*;
import java.io.*;

/**
 * Harness for training and testing a decoder. 
 *
 * @author Sushant Prakash
 */
public class DecoderTester {

  public static final String ENGLISH = "english";
  public static final String FRENCH = "french";
  public static final String GERMAN = "german";
  public static final String SPANISH = "spanish";
  public static final String ENGLISH_EXT = "e";
  public static final String FRENCH_EXT = "f";
  public static final String GERMAN_EXT = "g";
  public static final String SPANISH_EXT = "s";

  public static final String DATA_PATH = "/afs/ir/class/cs224n/pa2/data/";

  // file containing candidate zero-fertility words
  public static final String ZFERTS = "zferts";


  /* Returns the extension for a given language
    */
  public static String GetLanguageExtension(String language){
    if(language.equalsIgnoreCase(ENGLISH)){ return ENGLISH_EXT; }
    if(language.equalsIgnoreCase(GERMAN)){ return GERMAN_EXT; }
    if(language.equalsIgnoreCase(SPANISH)){ return SPANISH_EXT; }
    if(language.equalsIgnoreCase(FRENCH)){ return FRENCH_EXT; }
    return null;
  }
  
  /* Will create and train the specified language model.
    */
  public static LanguageModel CreateLanguageModel(Map<String,String> options, Collection<List<String>> lmTrainingSentences) throws IOException{
    
    // construct model, using reflection ...................................
    LanguageModel model;
    try {
      Class modelClass = Class.forName(options.get("-lmmodel"));
      model = (LanguageModel) modelClass.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    System.out.println("Created language model: " + model);

    // TODO:
    // Set any options to your language model here (i.e. discount factors, lambdas, etc.)
    
    // train model .........................................................
    System.out.println("Training language model with "+lmTrainingSentences.size()+" sentences");
    model.train(lmTrainingSentences);

    return model;
  }

  /* Will create and train the specified word aligner
   */

  public static WordAligner CreateWordAligner(Map<String,String> options, List<SentencePair> waTrainingSentencePairs){

    // construct model, using reflection
    WordAligner wordAligner;
    try {
      Class modelClass = Class.forName(options.get("-wamodel"));
      wordAligner = (WordAligner) modelClass.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    System.out.println("Created word aligner: " + wordAligner);

    //TODO:
    // set any options to your word aligner here

    // train model .........................................................
    System.out.print("Training word aligner with "+waTrainingSentencePairs.size()+" sentences");
    wordAligner.train(waTrainingSentencePairs);

    return wordAligner;
  }

  static List<SentencePair> makeReverseSentencePairs(List<SentencePair> waTrainingSentencePairs) {
    List<SentencePair> rsp = new ArrayList<SentencePair>();
    for(SentencePair sp : waTrainingSentencePairs) {
      rsp.add(new SentencePair(sp.getSentenceID(), sp.getSourceFile(), sp.getFrenchWords(), sp.getEnglishWords()));
    }
    return rsp;
  }


  public static void main(String[] args) throws IOException{

    // set up default options ..............................................
    Map<String, String> options = new HashMap<String, String>();
    options.put("-lmmodel",     "cs224n.langmodel.EmpiricalUnigramLanguageModel");
    options.put("-lmsentences", "1000");
    options.put("-wamodel",     "cs224n.wordaligner.BaselineWordAligner");
    options.put("-wasentences", "1000");
    options.put("-source",      "french");
    options.put("-target",      "english");
    options.put("-lmweight",    "0.6");
    options.put("-transweight", "0.4");
    options.put("-lengthweight","1.0");

    // let command-line options override defaults .........................
    options.putAll(CommandLineUtils.simpleCommandLineParser(args));

    // print out options
    System.out.println("DecoderTester options:");
    for (Map.Entry<String, String> entry: options.entrySet()) {
      System.out.printf("  %-12s: %s%n", entry.getKey(), entry.getValue());
    }
    System.out.println();

    // initiallize train / test sets
    Collection<List<String>> lmTrainingSentences = new ArrayList<List<String>>();
    List<SentencePair> waTrainingSentencePairs = new ArrayList<SentencePair>();
    List<SentencePair> testSentencePairs = new ArrayList<SentencePair>();

    // populate train / test sets
    Pair<String, String> languages = ReadTrainAndTestData(options, lmTrainingSentences, waTrainingSentencePairs, testSentencePairs);

    System.out.println("**********\nCreating / Training Language Model ... ");
    LanguageModel langmodel = CreateLanguageModel(options, lmTrainingSentences);
    System.out.println("..done\n**********\n");

    System.out.println("**********\nCreating / Training Word Aligner ... ");
    WordAligner wordaligner = CreateWordAligner(options, waTrainingSentencePairs);
    System.out.println("...done\n**********\n");

    List<SentencePair> reverse_waTrainingSentencePairs = makeReverseSentencePairs(waTrainingSentencePairs);
    System.out.println("**********\nCreating / Training Reverse Word Aligner ... ");
    WordAligner reverse_wordaligner = CreateWordAligner(options, reverse_waTrainingSentencePairs);
    System.out.println("...done\n**********\n");

    System.out.println("**********\n Setting weights for the decoder ... ");
    double lmweight = Double.parseDouble(options.get("-lmweight"));
    double transweight = Double.parseDouble(options.get("-transweight"));
    double lengthweight = Double.parseDouble(options.get("-lengthweight"));
    System.out.println("\tlmweight = "+lmweight);
    System.out.println("\ttransweight = "+transweight);
    System.out.println("\tlengthweight = "+lengthweight);
    System.out.println("...done\n**********\n");

    System.out.println("**********\nCreating / Testing Decoder ...");
    Decoder decoder = new GreedyDecoder(langmodel, wordaligner, reverse_wordaligner, lmweight, transweight, lengthweight, ZFERTS+"."+GetLanguageExtension(languages.getSecond()));
    test(decoder, testSentencePairs, languages);
    System.out.println("...done\n**********\n");
  }


  /*  This will run your decoder on the test sentences and compare
    * the output.  It used the EditDistance, which is not a very good measure
    * of the quality of translations - a better choice would have been the BLEU
    * score, but I ran out of time.  If you look at the actual output of the decoder
    * though, the translation quality should improve as your word alignment
    * model gets better.
    *
    * (2008) now there's BLEU as well.
    */

  private static void test(Decoder decoder, List<SentencePair> sentencePairs, Pair<String, String> languages){
    double totalDistance = 0;
    double totalWords = 0;
    Bleu bleu = new Bleu();
    int refLen = 0;
    int hypLen = 0;
    for(SentencePair sentPair : sentencePairs){
      List<String> sourceSentence = sentPair.getFrenchWords();
      List<String> actualTranslation = sentPair.getEnglishWords();
      System.out.println(languages.getFirst()+" string:\t"+SentenceToString(sourceSentence)+
                         "\nactual "+languages.getSecond()+" string:\t"+SentenceToString(actualTranslation));

      List<String> guessedTranslation = decoder.Decode(sourceSentence);
      double dist = EditDistance.getDistance(guessedTranslation,actualTranslation);
      String[] guessed = (String[])guessedTranslation.toArray(new String[guessedTranslation.size()]);
      String[] ref = (String[])actualTranslation.toArray(new String[actualTranslation.size()]);
      String[][] refs = new String[1][];
      refs[0] = ref;
      hypLen += guessed.length;
      refLen += refs[0].length;
      SegStats s = new SegStats(guessed, refs);
      bleu.add(s);
      System.out.println("guessed "+languages.getSecond()+" string:\t"+SentenceToString(guessedTranslation));
      System.out.println("Edit Distance:\t"+dist+"\nSentence Size:\t"+actualTranslation.size());
      System.out.println();
      // sanity check
      if (hypLen != bleu.hypLen || refLen != bleu.refLen) {
        throw new RuntimeException("ERROR");
      }


      totalDistance += dist;
      totalWords += actualTranslation.size();
    }
    System.out.println("\nWER: "+(totalDistance/totalWords));
    System.out.println("\nBLEU: "+bleu.score());
    System.out.print("--> log Ngram Scores: ");
    double[] scores = bleu.rawNGramScores();
    for (int i = 0; i < scores.length; i++) {
      double score = scores[i];
      System.out.print(score);
      if (i!=scores.length-1) System.out.print("/");
    }
    System.out.println();
    System.out.println("--> BP: "+bleu.BP()+" (refLen="+bleu.refLen+",hypLen="+bleu.hypLen+")");
  }

  private static String SentenceToString(List<String> sentence){
    StringBuilder sb = new StringBuilder();
    for(String word : sentence){
      sb.append(word);
      sb.append(" ");
    }
    return sb.toString();
  }


  // BELOW HERE IS IO CODE

  /* Will populate lmTrainingSentences, waTrainingSentencePairs,
    *  testSentencePairs, and testAlignments according to arguments
    *  in options.
    */
  public static Pair<String, String> ReadTrainAndTestData(Map<String,String> options, Collection<List<String>> lmTrainingSentences, List<SentencePair> waTrainingSentencePairs, List<SentencePair> testSentencePairs){
    
    String source = options.get("-source").toLowerCase();
    String target = options.get("-target").toLowerCase();
    String source_ext = GetLanguageExtension(source);
    String target_ext = GetLanguageExtension(target);

    if(target.equals(source)){
      System.err.println("Error with language choices, using "+FRENCH+"->"+ENGLISH);
      source = FRENCH;
      target = ENGLISH;
      source_ext = FRENCH_EXT;
      target_ext = ENGLISH_EXT;
    }
    
    String folder;
    
    if(!source.equals(ENGLISH)){
      folder = source;
    }
    else if(!target.equals(ENGLISH)){
      folder = target;
    }
    else {
      System.out.println("One of the languages must be "+ENGLISH+", using "+ENGLISH+" as target");
      target = ENGLISH;
      target_ext = ENGLISH_EXT;
      folder = source;
    }

    int numWATrainingSentences = Integer.parseInt(options.get("-wasentences"));
    int numLMTrainingSentences = Integer.parseInt(options.get("-lmsentences"));
    int maxTrainingSentences = Math.max(numLMTrainingSentences,numWATrainingSentences);

    //load up test sentence pairs
    List<SentencePair> allTestPairs = new ArrayList<SentencePair>();

    allTestPairs.addAll(readSentencePairs(DATA_PATH+folder+"/test", source_ext, target_ext, Integer.MAX_VALUE));

    //only keep test sentences b/w 5 and 20 words
    for(SentencePair sentpair : allTestPairs){
      List<String> sourceSentence = sentpair.getFrenchWords();
      if(sourceSentence.size() >= 5 && sourceSentence.size() <= 20){
        testSentencePairs.add(sentpair);
      }
    }

    //load up train sentence pairs
    List<SentencePair> trainingSentencePairs = new ArrayList<SentencePair>();
    if (maxTrainingSentences > 0){
      trainingSentencePairs.addAll(readSentencePairs(DATA_PATH+folder+"/training", source_ext, target_ext, maxTrainingSentences));
    }

    for(int i = 0; i < trainingSentencePairs.size(); i++){
      //add first numWAtrainingSentences to waTrainingSentencePairs
      if(i < numWATrainingSentences){
        waTrainingSentencePairs.add(trainingSentencePairs.get(i));
      }
      //add first numLMTrainingSentences to lmTrainingSentences
      if(i < numLMTrainingSentences){
        lmTrainingSentences.add(trainingSentencePairs.get(i).getEnglishWords());
      }
    }

    //add test pairs to training pairs ... avoids smoothing issues for alignment model for unseen words
    waTrainingSentencePairs.addAll(testSentencePairs);

    return new Pair<String,String>(source, target);
  }

  private static List<SentencePair> readSentencePairs(String path,  String source_ext, String target_ext, int maxSentencePairs) {
    List<SentencePair> sentencePairs = new ArrayList<SentencePair>();
    List<String> baseFileNames = getBaseFileNames(path,target_ext);
    for (String baseFileName : baseFileNames) {
      if (sentencePairs.size() >= maxSentencePairs)
        continue;
      sentencePairs.addAll(readSentencePairs(baseFileName, source_ext, target_ext));
    }
    return sentencePairs;
  }

  private static List<SentencePair> readSentencePairs(String baseFileName, String source_ext, String target_ext) {
    List<SentencePair> sentencePairs = new ArrayList<SentencePair>();
    String englishFileName = baseFileName + "." + target_ext;
    String frenchFileName = baseFileName + "." + source_ext;
    //String encoding = "iso-9959-1";
    try {
      //BufferedReader englishIn = new BufferedReader(new InputStreamReader(new FileInputStream(englishFileName), encoding));
      BufferedReader frenchIn = new BufferedReader(new FileReader(frenchFileName));
      //BufferedReader frenchIn = new BufferedReader(new InputStreamReader(new FileInputStream(frenchFileName), encoding));
      BufferedReader englishIn = new BufferedReader(new FileReader(englishFileName));
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

  private static List<String> getBaseFileNames(String path, String tar_ext) {
    final String target_ext = tar_ext;
    List<File> englishFiles = IOUtils.getFilesUnder(path, new FileFilter() {
      public boolean accept(File pathname) {
        if (pathname.isDirectory())
          return true;
        String name = pathname.getName();
        return name.endsWith(target_ext);
      }
    });
    List<String> baseFileNames = new ArrayList<String>();
    for (File englishFile : englishFiles) {
      String baseFileName = chop(englishFile.getAbsolutePath(), "."+target_ext);
      baseFileNames.add(baseFileName);
    }
    return baseFileNames;
  }

  private static String chop(String name, String extension) {
    if (! name.endsWith(extension)) return name;
    return name.substring(0, name.length()-extension.length());
  }

}
