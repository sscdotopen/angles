package io.ssc.angles.pipeline.explorers

import java.io.PrintWriter
import java.net.URI
import java.nio.file.{Files, Paths}
import java.util.Locale

import io.ssc.angles.pipeline.data.RethinkDb
import org.apache.commons.math.linear.RealVector
import org.slf4j.LoggerFactory

/**
 * Created by xolor on 11.02.15.
 */
object BuildExplorerGraph extends App {

  val uriToHost = (uri: URI) => uri.getHost
  val logger = LoggerFactory.getLogger(BuildExplorerGraph.getClass)
  val graphGenerator = new GraphGenerator

  logger.info("Querying database...")
  
  val workingList : List[ExplorerUriPair] = RethinkDb.getPairList
  logger.info("Got {} pairs from RethinkDB", workingList.size)

  // Build graph with cosine similarity function
  logger.info("Preparing graph with cosine similarity")
  buildAndWriteCSV("graph_cosine.csv", uriToHost, graphGenerator.COSINE_SIMILARITY)
  // Build graph with jaccard similarity function
  logger.info("Preparing graph with extended jaccard similarity")
  buildAndWriteCSV("graph_jaccard.csv", uriToHost, graphGenerator.EXT_JACCARD_SIMILARITY)


  def buildAndWriteCSV(filename: String, uriToString: (URI => String), similarityFunction: ((RealVector, RealVector) => Double)) {

    logger.info("Invoking graph builder...")
    var startTime = System.currentTimeMillis()

    val graph = new GraphGenerator().execute(workingList, uriToString, similarityFunction)

    var endTime = System.currentTimeMillis()

    logger.info("Graph generation finished within {} ms!", endTime - startTime)

    logger.info("Writing csv...")
    // write csv output
    var path = Paths.get(filename)
    var writer = new PrintWriter(Files.newBufferedWriter(path))

    writer.println("Source,Target,Weight,Type")

    graph.toList.foreach {
      case ((left, right), weight) => writer.format("\"%s\",\"%s\",%s,undirected\n", left, right, "%f".formatLocal(Locale.US, weight))
    }
    writer.flush()
    writer.close()
    
    logger.info("Finished csv export!")
  }

}
