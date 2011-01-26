package cs224n.wordaligner;

import java.util.List;

import cs224n.util.Alignment;
import cs224n.util.CounterMap;
import cs224n.util.SentencePair;

/**
 * Simple alignment baseline which maps french positions to english positions.
 * If the french sentence is longer, all final word map to null.
 */
public class ModelOneWordAligner extends WordAligner {

  private TranslationModel translationModel;

  public Alignment alignSentencePair(SentencePair sentencePair) {
    Alignment alignment = new Alignment();
    int numFrenchWords = sentencePair.getFrenchWords().size();
    for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
      String frenchWord = sentencePair.getFrenchWords().get(frenchPosition);
      // start with null
      double bestProbability = translationModel.getProbability(NULL_WORD,
          frenchWord);
      int bestIndex = -1;
      // see if another alignment is better
      for (int englishPosition = 0; englishPosition < sentencePair
          .getEnglishWords().size(); englishPosition++) {
        String englishWord = sentencePair.getEnglishWords()
            .get(englishPosition);
        // TODO: add distortion
        double probability = translationModel.getProbability(englishWord,
            frenchWord);
        if (probability >= bestProbability) {
          bestProbability = probability;
          bestIndex = englishPosition;
        }
      }
      if (bestIndex >= 0) {
        alignment.addAlignment(bestIndex, frenchPosition, true);
      }
    }
    return alignment;
  }

  public double getAlignmentProb(List<String> targetSentence,
      List<String> sourceSentence, Alignment alignment) {
    return 0;
  }

  public CounterMap<String, String> getProbSourceGivenTarget() {
    return translationModel.getTranslationModelParams();
  }

  public void train(List<SentencePair> trainingPairs) {
    translationModel = new TranslationModel(trainingPairs);

    int i = 0;
//    System.out.println(translationModel.getTranslationModelParams());
    do {
      translationModel.startIteration();
      for (SentencePair sentencePair : trainingPairs) {
        int numFrenchWords = sentencePair.getFrenchWords().size();
        for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
          String frenchWord = sentencePair.getFrenchWords().get(frenchPosition);
          double sum_quantity = 0; // TODO rename me

          // Calculate sum_quantity.
          for (int englishPosition = 0; englishPosition < sentencePair
              .getEnglishWords().size(); englishPosition++) {
            String englishWord = sentencePair.getEnglishWords().get(
                englishPosition);
            // TODO Add distortion
            sum_quantity += translationModel.getProbability(englishWord,
                frenchWord);
          }
          sum_quantity += translationModel
              .getProbability(NULL_WORD, frenchWord);

          // Calculate posterior
          for (int englishPosition = 0; englishPosition < sentencePair
              .getEnglishWords().size(); englishPosition++) {
            String englishWord = sentencePair.getEnglishWords().get(
                englishPosition);
            translationModel.addFractionalCount(englishWord, frenchWord,
                translationModel.getProbability(englishWord, frenchWord)
                    / sum_quantity);

            // TODO add distortion

          }
          translationModel.addFractionalCount(NULL_WORD, frenchWord,
              translationModel.getProbability(NULL_WORD, frenchWord)
                  / sum_quantity);

          // TODO add distortion
        }
      }

      translationModel.finishIteration();
//      System.out.println(translationModel.getTranslationModelParams());
    } while (i++ < 10);

  }

}
