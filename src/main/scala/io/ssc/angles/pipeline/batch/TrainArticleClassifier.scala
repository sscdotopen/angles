package io.ssc.angles.pipeline.batch

import java.util.concurrent.atomic.AtomicInteger

import com.google.common.io.Closeables
import io.ssc.angles.Config
import io.ssc.angles.pipeline.data.Storage
import io.ssc.angles.pipeline.ml.WebsiteVectorizer
import org.apache.mahout.classifier.evaluation.Auc
import org.apache.mahout.classifier.sgd._
import org.apache.mahout.math.Vector
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.util.Random

object TrainArticleClassifier extends App {

  val log = LoggerFactory.getLogger(TrainArticleClassifier.getClass)

  log.info("Vectorizing training websites....")

  val labeledUris = Storage.websitesWithLabels()

  val labeledInstances = labeledUris flatMap { case (uri, label) =>
  
      Storage.website(uri) match {
        case Some(website) =>
          val vector = WebsiteVectorizer.vectorize(website) 
          val labelIndex = if (label) { 1 } else { 0 }
          Some(vector -> labelIndex)
        case _ =>
          println("No data found for " + uri)
          None
      }
    }

  val shuffledLabeledInstances = Random.shuffle(labeledInstances)
  

  log.info("Preparing data for 5-fold cross validation...")

  val positiveExamples = shuffledLabeledInstances filter { _._2 == 1}
  val negativeExamples = shuffledLabeledInstances filter { _._2 == 0}

  val trainingSets = Array.ofDim[List[(Vector, Int)]](5)
  val positiveTestsets = Array.ofDim[List[(Vector, Int)]](5)
  val negativeTestsets = Array.ofDim[List[(Vector, Int)]](5)

  for (k <- 0 to 4) {

    val trainingSet = mutable.ListBuffer[(Vector, Int)]()
    val positiveTestset = mutable.ListBuffer[(Vector, Int)]()
    val negativeTestset = mutable.ListBuffer[(Vector, Int)]()

    var pos = 0
    while (pos < positiveExamples.size) {
      if (pos % 5 == k) {
        positiveTestset += positiveExamples(pos)
      } else {
        trainingSet += positiveExamples(pos)
      }
      pos += 1
    }

    pos = 0
    while (pos < negativeExamples.size) {
      if (pos % 5 == k) {
        negativeTestset += negativeExamples(pos)
      } else {
        trainingSet += negativeExamples(pos)
      }
      pos += 1
    }

    trainingSets(k) = trainingSet.toList
    positiveTestsets(k) = positiveTestset.toList
    negativeTestsets(k) = negativeTestset.toList
  }


  case class ParameterCombination(learningRate: Double, lambda: Double, prior: PriorFunction)
  case class PredictionQuality(auc: Double, numPositiveCorrect: Int, numNegativeCorrect: Int)


  val possibleParams = Array(0.000005, 0.000001, 0.00005, 0.00001, 0.0005, 0.0001, 0.005, 0.001, 0.05, 0.01, 0.5, 0.1)

  val parameterCombinations =
    for (prior <- Array(new L1(), new L2());
         learningRate <- possibleParams;
         lambda <- possibleParams) yield {
      ParameterCombination(learningRate, lambda, prior)
    }

  val numSearchesDone = new AtomicInteger(0)

  log.info("Starting grid search over {} classifiers...", parameterCombinations.size)

  val threshold = 0.5

  val candidates = parameterCombinations.par flatMap { parameterCombo =>

    var positiveCorrect = 0
    var negativeCorrect = 0
    val auc = new Auc(threshold)

    for (k <- 0 to 4) {

      val learner = new OnlineLogisticRegression(2, WebsiteVectorizer.numFeatures, parameterCombo.prior)
                        .learningRate(parameterCombo.learningRate)
                        .lambda(parameterCombo.lambda)

      for (pass <- 1 to 3) {
        for ((vector, label) <- Random.shuffle(trainingSets(k))) {
          learner.train(label, vector)
        }
      }

      for ((instance, _) <- positiveTestsets(k)) {
        val prediction = learner.classifyScalar(instance)
        auc.add(1, prediction)
        if (prediction > threshold) {
          positiveCorrect += 1
        }
      }

      for ((instance, _) <- negativeTestsets(k)) {
        val prediction = learner.classifyScalar(instance)
        auc.add(0, prediction)
        if (prediction <= threshold) {
          negativeCorrect += 1
        }
      }

      Closeables.close(learner, false)
    }

    val result = (positiveCorrect + negativeCorrect) + "/" + shuffledLabeledInstances.size +", " + positiveCorrect + "/" +
      positiveExamples.size + " positives, " + negativeCorrect + "/" + negativeExamples.size + " negatives, " +
      parameterCombo

    val positiveAccuracy = positiveCorrect.toDouble / positiveExamples.size
    val negativeAccuracy = negativeCorrect.toDouble / negativeExamples.size


    val candidate = positiveCorrect > 0 && negativeCorrect > 0

    if (candidate) {
      log.info("Found candidate {}", result)
    }

    val numTries = numSearchesDone.incrementAndGet()
    if (numTries % 10 == 0) {
      log.info("{} of {} done", numTries, parameterCombinations.size)
    }

    if (candidate) {
      Some((PredictionQuality(auc.auc(), positiveCorrect, negativeCorrect), parameterCombo))
    } else {
      None
    }
  }

  if (candidates.isEmpty) {
    log.warn("COULD NOT FIND CLASSIFIER WITH SATISFYING ACCURACY!!!")
  } else {
    val (bestQuality, bestCombination) =
      candidates.toArray.sortBy({ case (quality, combo) => (quality.auc, quality.numNegativeCorrect) }).last

    val bestLearner = new OnlineLogisticRegression(2, WebsiteVectorizer.numFeatures, bestCombination.prior)
      .learningRate(bestCombination.learningRate)
      .lambda(bestCombination.lambda)

    log.info("Training best learner {}, which gave {} on whole dataset", Array(bestCombination.toString, bestQuality.toString))

    for (pass <- 1 to 100) {
      for ((vector, label) <- Random.shuffle(shuffledLabeledInstances)) {
        bestLearner.train(label, vector)
      }
    }

    log.info("Saving best learner...")
    ModelSerializer.writeBinary(Config.property("angles.dataDir") + "/article-detector.model", bestLearner)

    Closeables.close(bestLearner, false)
  }
  
}
