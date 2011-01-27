package cs224n.wordaligner;

import java.util.List;

import cs224n.util.Alignment;
import cs224n.util.CounterMap;
import cs224n.util.SentencePair;

/**
 * Simple alignment baseline which maps french positions to english positions.
 * If the french sentence is longer, all final word map to null.
 */
public class ModelTwoWordAligner extends WordAligner {

  private TranslationModel translationModel;
  private DistortionModel distortionModel;

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
        double probability = translationModel.getProbability(englishWord,
            frenchWord)
            * distortionModel.getProbability(sentencePair.getEnglishWords()
                .size(), englishPosition, numFrenchWords, frenchPosition);
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
    // Run model one.
    ModelOneWordAligner modelOne = new ModelOneWordAligner();
    modelOne.train(trainingPairs);
    // Steal model one's translationModel
    translationModel = modelOne.getTranslationModel();
    distortionModel = new DisplacementDistortionModel();
    distortionModel.init(trainingPairs);

    int i = 0;
    // System.out.println(translationModel.getTranslationModelParams());
    do {
      translationModel.startIteration();
      distortionModel.startIteration();
      for (SentencePair sentencePair : trainingPairs) {
        int numFrenchWords = sentencePair.getFrenchWords().size();
        for (int frenchPosition = 0; frenchPosition < numFrenchWords; frenchPosition++) {
          String frenchWord = sentencePair.getFrenchWords().get(frenchPosition);
          double sum_quantity = 0; // TODO rename me

          // Calculate sum_quantity.
          for (int englishPosition = -1; englishPosition < sentencePair
              .getEnglishWords().size(); englishPosition++) {
            String englishWord;
            if (englishPosition >= 0) {
              englishWord = sentencePair.getEnglishWords().get(englishPosition);
            } else {
              englishWord = NULL_WORD;
            }
            
            double distortion = distortionModel.getProbability(sentencePair
                .getEnglishWords().size(), englishPosition, sentencePair
                .getFrenchWords().size(), frenchPosition);
            sum_quantity += translationModel.getProbability(englishWord,
                frenchWord) * distortion;
          }
          
          // Calculate posterior
          for (int englishPosition = -1; englishPosition < sentencePair
              .getEnglishWords().size(); englishPosition++) {
            String englishWord;
            if (englishPosition >= 0) {
              englishWord = sentencePair.getEnglishWords().get(englishPosition);
            } else {
              englishWord = NULL_WORD;
            }
            
            double distortion = distortionModel.getProbability(sentencePair
                .getEnglishWords().size(), englishPosition, sentencePair
                .getFrenchWords().size(), frenchPosition);
            double fractionalCount = translationModel.getProbability(
                englishWord, frenchWord) * distortion / sum_quantity;
            translationModel.addFractionalCount(englishWord, frenchWord,
                fractionalCount);
            distortionModel.addFractionalCount(sentencePair.getEnglishWords()
                .size(), englishPosition, sentencePair.getFrenchWords().size(),
                frenchPosition, fractionalCount);

          }
        }
      }

      translationModel.finishIteration();
      distortionModel.finishIteration();
      // System.out.println(translationModel.getTranslationModelParams());
    } while (i++ < 10);

  }
}
