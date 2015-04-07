package io.ssc.angles.pipeline.explorers

import java.net.URI
import java.util
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

import org.apache.commons.math3.linear.{OpenMapRealVector, RealVector}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
 * Created by xolor on 11.02.15.
 */
class GraphGenerator {

  val logger = LoggerFactory.getLogger(classOf[GraphGenerator])

  // Sample implementation of cosine similarity
  val COSINE_SIMILARITY: (RealVector, RealVector) => Double = {
    (lhs, rhs) => lhs.dotProduct(rhs) / (lhs.getNorm * rhs.getNorm)
  }

  // Sample implementation of extended jaccard similarity (http://strehl.com/diss/node56.html)
  val EXT_JACCARD_SIMILARITY: (RealVector, RealVector) => Double = {
    (lhs, rhs) => lhs.dotProduct(rhs) / (
      (lhs.getNorm * lhs.getNorm) + (rhs.getNorm * rhs.getNorm) - lhs.dotProduct(rhs)
      )
  }

  /**
   * Calculate the graph from a given input set using given functions.
   *
   * @param inputSet Input set of clusterable tweets with URIs and explorer id.
   * @param urlMappingFunction Function for mapping a URI to a String
   * @param similarityFunction Function for calculating the similarity between two vectors.
   */
  def execute(inputSet: List[ExplorerUriPair],
              urlMappingFunction: (URI) => String,
              similarityFunction: (RealVector, RealVector) => Double): Map[(String, String), Double] = {

    // Prepare map for URI->Dimension
    val dimensionMap: Map[String, Int] = calculateDimensions(inputSet, urlMappingFunction)
    logger.info("Found {} dimensions for url mapping", dimensionMap.size)

    // Collect all urls for each explorer
    val explorerUrlMap: Map[String, java.util.List[String]] = calculateExplorerUrlMap(inputSet, urlMappingFunction)

    logger.info("Calculating IDF vector")
    // Count "df" i.e. how many users tweeted a URI
    val urlCountMap: Map[String, Int] = calculateUrlCountMap(inputSet, urlMappingFunction)
    // Calculate the IDF vector
    val idfVector: RealVector = calculateIdfVector(explorerUrlMap, urlCountMap, dimensionMap)

    logger.info("Building vector space")
    // Convert to real mathematical vectors with x dimensions
    val explorerSpace: Map[String, RealVector] = buildExplorerSpace(dimensionMap, explorerUrlMap, idfVector)
    logger.info("Built vector space of {} explorers", explorerSpace.size)

    // Calculate the similarity
    val similarityPairs: Map[(String, String), Double] = calculateSimilarity(explorerSpace, similarityFunction)
    logger.info("Calculated {} similarities", similarityPairs.size)

    similarityPairs
  }

  /**
   * Calculate the IDF vector.
   *
   * @param explorerUrlMap
   * @param urlCountMap
   * @param dimensionMap
   * @return
   */
  def calculateIdfVector(explorerUrlMap: Map[String, java.util.List[String]], urlCountMap: Map[String, Int], dimensionMap: Map[String, Int]): RealVector = {
    val explorerCount = explorerUrlMap.size
    val idfVector = new OpenMapRealVector(dimensionMap.size)

    urlCountMap.foreach {
      case (url: String, df: Int) =>
        val dimension = dimensionMap.get(url)
        val value = Math.log(explorerCount / df)

        if (dimension.isEmpty)
          logger.warn("Dimension for url {} could not be found - check url mapping function", url)
        else
          idfVector.setEntry(dimension.get, value)
    }
    idfVector
  }

  /**
   * Count by how many users a URI was tweeted. This will become something like the "IDF" of TF-IDF.
   * @param pairs Input set of clusterable tweets with URIs and explorer id.
   * @param uriToString Function for mapping a URI to a String
   */
  def calculateUrlCountMap(pairs: List[ExplorerUriPair], uriToString: (URI) => String): Map[String, Int] = {
    var urlExplorerCount: ConcurrentHashMap[String, Int] = new ConcurrentHashMap[String, Int]

    // This map will contain
    pairs.par.map((pair: ExplorerUriPair) => (uriToString(pair.uri), pair.explorerId)
    ).distinct.seq.foreach {
      case (null, explorer) =>
        logger.warn("Url became null for explorer {} - check your URL mapping function", explorer)
      case pair =>
        urlExplorerCount.put(pair._1, (() => {
          var v = urlExplorerCount.getOrDefault(pair._1, 0)
          v + 1
        })())
    }

    urlExplorerCount.toMap
  }

  /**
   * Calculate similarities with the previously given similarity-function
   */
  private def calculateSimilarity(explorerMap: Map[String, RealVector], similarityFunction: (RealVector, RealVector) => Double): Map[(String, String), Double] = {
    var resultSet: ConcurrentHashMap[(String, String), Double] = new ConcurrentHashMap[(String, String), Double]

    val explorerSpace = explorerMap.toSeq
    val totalElements = explorerSpace.size
    val threadCount = Runtime.getRuntime.availableProcessors()
    val partitionSize = totalElements / threadCount

    val tasks = 0 until threadCount
    val futures = tasks.map {
      threadNumber => Future {
        val partitionBegin = threadNumber * partitionSize
        var partitionEnd: Int = 0
        if (threadNumber == threadCount - 1) {
          partitionEnd = totalElements
        } else {
          partitionEnd = partitionBegin + partitionSize
        }
        logger.info("Starting thread {} with partition from {} to {}", threadNumber.toString, partitionBegin.toString, partitionEnd.toString)
        calculateSimilarityPartially(similarityFunction, resultSet, explorerSpace, partitionBegin, partitionEnd)
        logger.info("Finished thread {}", threadNumber.toString, partitionBegin.toString, partitionEnd.toString)
      }
    }
    val futureSequence = Future.sequence(futures)
    Await.result(futureSequence, Duration.Inf)
    resultSet.toMap
  }

  def calculateSimilarityPartially(similarityFunction: (RealVector, RealVector) => Double,
                                   resultSet: ConcurrentHashMap[(String, String), Double],
                                   explorerSpace: Seq[(String, RealVector)],
                                   workerBegin: Int, workerEnd: Int)
  : Unit = {
    var outerCount = workerBegin
    while (outerCount < workerEnd) {
      val (leftId, lhs) = explorerSpace(outerCount)
      var innerCount = 0

      while (innerCount < outerCount) {
        val (rightId, rhs) = explorerSpace(innerCount)
        val similarity: Double = similarityFunction(lhs, rhs)
        if (similarity >= 0.1 && similarity <= 1.00)
          resultSet += (((leftId, rightId), similarity))
        innerCount += 1
      }
      outerCount += 1
    }
  }

  /**
   * Convert the previously generated explorer-uri map into a real vector space. 
   */
  private def buildExplorerSpace(dimensionMap: Map[String, Int], explorerUrlMap: Map[String, java.util.List[String]], idfVector: RealVector): Map[String, RealVector] = {
    var explorerSpace: ConcurrentHashMap[String, RealVector] = new ConcurrentHashMap[String, RealVector]

    explorerUrlMap.par.foreach { case tuple => {
      val name = tuple._1
      val urls = tuple._2
      // Convert the list representation to a mathematical vector
      val vector: RealVector = new OpenMapRealVector(dimensionMap.size)
      urls.foreach(url => vector.setEntry(dimensionMap.get(url).get, vector.getEntry(dimensionMap.get(url).get) + 1))
      explorerSpace += ((name, vector))
    }
    }
    
    // Use TF-IDF for feature extraction -> i.e. TF is number of urls for each user and IDF is number of users that tweeted a URL
    explorerSpace.par.map {
      case (s: String, v: RealVector) => (s, v.ebeMultiply(idfVector))
    }.seq.toMap
  }

  /**
   * Calculate a map of all tweeted uris per explorer.
   */
  private def calculateExplorerUrlMap(tweets: List[ExplorerUriPair], uriToString: (URI) => String): Map[String, util.List[String]] = {
    var resultMap: ConcurrentHashMap[String, java.util.List[String]] = new ConcurrentHashMap[String, java.util.List[String]]

    tweets.par.foreach { case tweet => {
      var newValue: util.List[String] = resultMap.getOrElse(tweet.explorerId, Collections.synchronizedList(new util.ArrayList[String]()))
      newValue += tweet.mapURI(uriToString)
      resultMap.update(tweet.explorerId, newValue)
    }
    }

    resultMap.toMap
  }

  /**
   * Calculate a uri-dimension map.
   */
  private def calculateDimensions(inputSet: List[ExplorerUriPair], mappingFunction: (URI) => String): Map[String, Int] = {
    inputSet.par.map(
      t => mappingFunction(t.uri)
    ).distinct.zipWithIndex.seq.toMap
  }

}
