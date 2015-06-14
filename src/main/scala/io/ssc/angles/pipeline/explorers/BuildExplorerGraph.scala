/**
 * Angles
 * Copyright (C) 2015 Jakob Hendeß, Niklas Wolber
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.ssc.angles.pipeline.explorers

import java.io.PrintWriter
import java.net.URI
import java.nio.file.{Files, Paths}
import java.util.Locale

import com.google.common.collect.{BiMap, HashBiMap, HashMultimap, SetMultimap}
import edu.ucla.sspace.graph.{ChineseWhispersClustering, Graphs, SimpleWeightedEdge, SparseUndirectedGraph}
import edu.ucla.sspace.util.MultiMap
import org.apache.commons.lang3.math.NumberUtils
import org.apache.commons.math3.linear.RealVector
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

/**
 * Application for building similarity graphs. This will read explorer-URI pairs from pairs.csv and will write the
 * results to graph_cosine.csv and graph_jaccard.csv.
 *
 * You can additionally define three parameters:
 *  - lower threshold for similarity in the graph (default value: 0.5)
 *  - upper threshold for similarity in the graph (default value: 0.95)
 *  - any third parameter will DISABLE tf-idf on vector space calculation!
 */
object BuildExplorerGraph extends App {

  val csvFile = "pairs.csv"

  //val uriToHost = (uri: URI) => uri.getHost
  def uriToSecondLevelDomain(uri: URI) = if (uri.getHost != null) uri.getHost.split("\\.").takeRight(2).mkString(".") else null

  val logger = LoggerFactory.getLogger(BuildExplorerGraph.getClass)

  var lowerThreshold = 0.5
  var upperThreshold = 0.95

  if (args.length >= 2) {
    lowerThreshold = NumberUtils.createDouble(args(0))
    upperThreshold = NumberUtils.createDouble(args(1))
  }

  val disableTfIdf = args.length >= 3

  val graphGenerator = new GraphGenerator(lowerThreshold, upperThreshold, disableTfIdf)

  logger.info("Querying datastore...")

  val workingList: List[ExplorerUriPair] = CSVReader.readExplorerPairsFromCSV(csvFile)
  logger.info("Got {} pairs from CSV", workingList.size)

  // Build graph with cosine similarity function
  logger.info("Preparing graph with cosine similarity")
  val cosineGraph = buildGraph(uriToSecondLevelDomain, graphGenerator.COSINE_SIMILARITY)
  writeGraphCSV("graph_cosine.csv", cosineGraph)


  // Build graph with jaccard similarity function
  logger.info("Preparing graph with extended jaccard similarity")
  val jaccardGraph = buildGraph(uriToSecondLevelDomain, graphGenerator.EXT_JACCARD_SIMILARITY)
  writeGraphCSV("graph_jaccard.csv", jaccardGraph)


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
    val graph = graphGenerator.execute(workingList, uriToString, similarityFunction)
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
}
