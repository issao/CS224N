package cs224n.wordaligner;

import java.util.List;

import cs224n.util.Alignment;
import cs224n.util.CounterMap;
import cs224n.util.SentencePair;

/**
 * Simple alignment baseline which maps french positions to english positions.
 * If the french sentence is longer, all final word map to null.
 */
public class ModelNWordAligner extends WordAligner {

  private static final int NUM_ITERATIONS = 10;
  private TranslationModel translationModel;
  private DistortionModel distortionModel;
  private ModelNWordAligner priorModel;

  public TranslationModel getTranslationModel() {
    return translationModel;
  }

  public ModelNWordAligner(ModelNWordAligner priorModel,
      DistortionModel distortionModel) {
    this.distortionModel = distortionModel;
    this.priorModel = priorModel;
  }

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
    double probability = 0;
    for (int sourcePosition = 0; sourcePosition < sourceSentence.size(); sourcePosition++) {
      int targetPosition = alignment.getAlignedTarget(sourcePosition);
      // Distortion can be caluclated generally because if it's from the null word, the bucket function handles that as a special case.
      double distortion = distortionModel.getProbability(
          targetSentence.size(), targetPosition, sourceSentence.size(),
          sourcePosition);
      if (targetPosition == -1) {
        probability += Math.log(translationModel.getProbability(NULL_WORD,
            sourceSentence.get(sourcePosition)) * distortion);
      } else {
        
        probability += Math.log(translationModel.getProbability(
            targetSentence.get(targetPosition),
            sourceSentence.get(sourcePosition))
            * distortion);
      }
    }
//    System.err.println(probability);
    return Math.exp(probability);
  }

  public CounterMap<String, String> getProbSourceGivenTarget() {
    CounterMap<String, String> reversed = new CounterMap<String, String>();
    CounterMap<String, String> forward = translationModel
        .getTranslationModelParams();
    for (String key : forward.keySet()) {
      for (String value : forward.getCounter(key).keySet()) {
        reversed.setCount(value, key, forward.getCount(key, value));
      }
    }
    return reversed;
  }

  public void train(List<SentencePair> trainingPairs) {
    // Run prior model if there is one.
    if (priorModel != null) {
      priorModel.train(trainingPairs);
      // Bootstrap with prior model's translationModel
      translationModel = priorModel.getTranslationModel();
    } else {
      translationModel = new TranslationModel();
      translationModel.init(trainingPairs);
    }
    distortionModel.init(trainingPairs);

    int i = 0;
    // System.out.println(translationModel.getTranslationModelParams());
    do {
      System.out.println("Iteration " + i);
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
    } while (i++ < NUM_ITERATIONS);

  }
}
