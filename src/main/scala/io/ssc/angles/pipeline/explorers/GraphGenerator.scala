package io.ssc.angles.pipeline.explorers

import java.net.URI

import org.apache.commons.lang3.ObjectUtils
import org.apache.commons.math.linear.{OpenMapRealVector, RealVector}
import org.slf4j.LoggerFactory

import scala.collection.mutable

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
  def execute(inputSet: List[ClusterableTweet],
              urlMappingFunction: (URI) => String,
              similarityFunction: (RealVector, RealVector) => Double): Map[(String, String), Double] = {

    // Prepare map for URI->Dimension
    val dimensionMap: Map[String, Int] = calculateDimensions(inputSet, urlMappingFunction)
    logger.info("Found {} dimensions for url mapping", dimensionMap.size)
    
    // Collect all urls for each explorer
    val explorerUrlMap: Map[String, mutable.MutableList[String]] = calculateExplorerUrlMap(inputSet, urlMappingFunction)

    // Convert to real mathematical vectors with x dimensions
    val explorerSpace: Map[String, RealVector] = buildExplorerSpace(dimensionMap, explorerUrlMap)
    logger.info("Built vector space of {} explorers", explorerSpace.size)

    // Calculate the similarity
    val similarityPairs: Map[(String, String), Double] = calculateSimilarity(explorerSpace, similarityFunction)
    logger.info("Calculated {} similarities", similarityPairs.size)

    similarityPairs
  }

  /**
   * Calculate similarities with the previously given similarity-function
   */
  private def calculateSimilarity(explorerSpace: Map[String, RealVector], similarityFunction: (RealVector, RealVector) => Double): Map[(String, String), Double] = {
    var resultSet: mutable.HashMap[(String, String), Double] = mutable.HashMap.empty
    for ((leftId, lhs) <- explorerSpace) {
      for ((rightId, rhs) <- explorerSpace) {
        if (ObjectUtils.notEqual(leftId, rightId) && !resultSet.contains((rightId, leftId)) && !resultSet.contains((leftId, leftId))) {
          val similarity: Double = similarityFunction(lhs, rhs)
          if (similarity > 0.5)
            resultSet += (((leftId, rightId), similarity))
        }
      }
    }
    resultSet.toMap
  }

  /**
   * Convert the previously generated explorer-uri map into a real vector space. 
   */
  private def buildExplorerSpace(dimensionMap: Map[String, Int], explorerUrlMap: Map[String, mutable.MutableList[String]]): Map[String, RealVector] = {
    var explorerSpace: mutable.Map[String, RealVector] = mutable.HashMap.empty

    for ((name, urls) <- explorerUrlMap) {
      // Convert the list representation to a mathematic vector
      val vector: RealVector = new OpenMapRealVector(dimensionMap.size)
      urls.foreach(url => vector.setEntry(dimensionMap.get(url).get, vector.getEntry(dimensionMap.get(url).get) + 1))
      explorerSpace += ((name, vector))
    }

    explorerSpace.toMap
  }

  /**
   * Calculate a map of all tweeted uris per explorer.
   */
  private def calculateExplorerUrlMap(tweets: List[ClusterableTweet], uriToString: (URI) => String): Map[String, mutable.MutableList[String]] = {
    var resultMap = new mutable.HashMap[String, mutable.MutableList[String]]

    for (tweet <- tweets) {
      val newValue = resultMap.getOrElse(tweet.explorerId, mutable.MutableList.empty) ++ tweet.mapURIs(uriToString)
      resultMap.update(tweet.explorerId, newValue)
    }

    resultMap.toMap
  }

  /**
   * Calculate a uri-dimension map.
   */
  private def calculateDimensions(inputSet: List[ClusterableTweet], mappingFunction: (URI) => String): Map[String, Int] = {
    inputSet.flatMap(
      t => t.mapURIs(mappingFunction)
    ).distinct.zipWithIndex.toMap
  }

}
