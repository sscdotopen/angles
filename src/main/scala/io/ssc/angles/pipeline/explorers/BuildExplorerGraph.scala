package io.ssc.angles.pipeline.explorers

import java.io.PrintWriter
import java.net.URI
import java.nio.file.{Files, Paths}
import java.util
import java.util.Locale

import com.google.common.collect.{BiMap, HashBiMap, HashMultimap, SetMultimap}
import edu.ucla.sspace.graph.{ChineseWhispersClustering, Graphs, SimpleWeightedEdge, SparseUndirectedGraph}
import edu.ucla.sspace.util.MultiMap
import io.ssc.angles.pipeline.data.RethinkDb
import org.apache.commons.math.linear.RealVector
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

/**
 * Created by xolor on 11.02.15.
 */
object BuildExplorerGraph extends App {

  val uriToHost = (uri: URI) => uri.getHost
  val logger = LoggerFactory.getLogger(BuildExplorerGraph.getClass)
  val graphGenerator = new GraphGenerator

  logger.info("Querying database...")

  val workingList: List[ExplorerUriPair] = RethinkDb.getPairList
  logger.info("Got {} pairs from RethinkDB", workingList.size)

  // Build graph with cosine similarity function
  logger.info("Preparing graph with cosine similarity")
  val cosineGraph = buildGraph(uriToHost, graphGenerator.COSINE_SIMILARITY)
  writeGraphCSV("graph_cosine.csv", cosineGraph)
  val cosineClusterMap = calculateClusters(cosineGraph)
  writeClusterFile("communities_cosine.tsv", cosineClusterMap)

  // Build graph with jaccard similarity function
  logger.info("Preparing graph with extended jaccard similarity")
  val jaccardGraph = buildGraph(uriToHost, graphGenerator.EXT_JACCARD_SIMILARITY)
  writeGraphCSV("graph_jaccard.csv", jaccardGraph)
  val jaccardClusterMap = calculateClusters(jaccardGraph)
  writeClusterFile("communities_jaccard.tsv", jaccardClusterMap)


  def calculateClusters(rawMap: Map[(String, String), Double]): SetMultimap[Int, String] = {
    val graph = new SparseUndirectedGraph()
    val vertexExplorerMap: BiMap[String, Int] = HashBiMap.create[String, Int]()
    var vertexCount: Int = 0

    logger.info("Preparing graph for clustering...")

    // Convert map to graph
    rawMap.foreach { case ((leftString, rightString), weight) => {
      // Calculate the vertex id for the left or the right if not already done
      val leftVertexId = vertexExplorerMap.getOrElseUpdate(leftString, (() => {
        vertexCount += 1
        vertexCount
      })())
      val rightVertexId = vertexExplorerMap.getOrElseUpdate(rightString, (() => {
        vertexCount += 1
        vertexCount
      })())
      // Add edge to graph
      graph.add(new SimpleWeightedEdge(leftVertexId, rightVertexId, weight))
    }
    }

    logger.info("Invoking chinese whisper clustering...")
    val clusters: MultiMap[Integer, Integer] = new ChineseWhispersClustering().cluster(Graphs.pack(graph))
    // Output: (VertexId - Clusters)
    logger.info("Clustering finished.")

    logger.info("Generating cluster map...")

    val resultMap: SetMultimap[Int, String] = HashMultimap.create()

    for (c: (Integer, java.util.Set[Integer]) <- clusters.asMap()) {
      val vertexId = c._1
      val clusters = c._2
      for (clusterId <- clusters) {
        resultMap.get(clusterId).add(vertexExplorerMap.inverse().get(vertexId))
      }
    }

    logger.info("Cluster map generated.")

    resultMap
  }


  def buildGraph(uriToString: (URI => String), similarityFunction: ((RealVector, RealVector) => Double)): Map[(String, String), Double] = {
    logger.info("Invoking graph builder...")
    val startTime = System.currentTimeMillis()

    val graph = new GraphGenerator().execute(workingList, uriToString, similarityFunction)

    val endTime = System.currentTimeMillis()

    logger.info("Graph generation finished within {} ms!", endTime - startTime)

    graph
  }

  def writeGraphCSV(filename: String, graph: Map[(String, String), Double]) {
    logger.info("Writing csv...")
    // write csv output
    val path = Paths.get(filename)
    val writer = new PrintWriter(Files.newBufferedWriter(path))

    writer.println("Source,Target,Weight,Type")

    graph.toList.foreach {
      case ((left, right), weight) => writer.format("\"%s\",\"%s\",%s,undirected\n", left, right, "%f".formatLocal(Locale.US, weight))
    }
    writer.flush()
    writer.close()

    logger.info("Finished csv export!")
  }

  def writeClusterFile(filename: String, clusterMap: SetMultimap[Int, String]) = {
    logger.info("Writing cluster file ...")

    var path = Paths.get(filename)
    var writer = new PrintWriter(Files.newBufferedWriter(path))

    for (t: Tuple2[Int, java.util.Collection[String]] <- clusterMap.asMap()) {
      val clusterId: Int = t._1
      val explorerIds: util.Collection[String] = t._2
      explorerIds.foreach {
        case explorerId: String => writer.println(explorerId)
        case any => logger.warn("Skipping invalid value in cluster {}: {}", clusterId, any)
      }
      writer.println()
    }

    writer.flush()
    writer.close()

    logger.info("Finished cluster export!")
  }
}
