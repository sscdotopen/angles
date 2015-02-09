package io.ssc.angles.pipeline.explorers

import java.io.PrintWriter
import java.net.URI
import java.nio.file.{Files, Paths}
import java.util.Locale

import io.ssc.angles.pipeline.data.Storage
import org.apache.commons.math.linear.RealVector
import org.slf4j.LoggerFactory

/**
 * Created by xolor on 11.02.15.
 */
object BuildExplorerGraph extends App {

  val uriToHost = (uri: URI) => uri.getHost
  val logger = LoggerFactory.getLogger(BuildExplorerGraph.getClass)
  val graphGenerator = new GraphGenerator

  logger.info("Fetching explorer/uri-pairs...")
  val rawPairs: List[(String, String)] = Storage.allTweetURIPairs()

  logger.info("Got {} pairs", rawPairs.size)
  val workingList: List[ClusterableTweet] = rawPairs.map(a => new ClusterableTweet(a._1, List(URI.create(a._2)))).toList

  // Build graph with cosine similarity function
  buildAndWriteCSV("graph_cosine.csv", uriToHost, graphGenerator.COSINE_SIMILARITY)
  buildAndWriteCSV("graph_jaccard.csv", uriToHost, graphGenerator.EXT_JACCARD_SIMILARITY)


  def buildAndWriteCSV(filename: String, uriToString: (URI => String), similarityFunction: ((RealVector, RealVector) => Double)) {

    logger.info("Invoking graph builder...")
    var startTime = System.currentTimeMillis()

    val graph = new GraphGenerator().execute(workingList, uriToString, similarityFunction)

    var endTime = System.currentTimeMillis()

    //println(graph)
    logger.info("Graph generation finished within {} ms!", endTime - startTime)

    // write csv output
    var path = Paths.get(filename)
    var writer = new PrintWriter(Files.newBufferedWriter(path))

    writer.println("Source,Target,Weight,Type")

    graph.toList.foreach {
      case ((left, right), weight) => writer.format("\"%s\",\"%s\",%s,undirected\n", left, right, "%f".formatLocal(Locale.US, weight))
    }
    writer.flush()
    writer.close()
  }

}
