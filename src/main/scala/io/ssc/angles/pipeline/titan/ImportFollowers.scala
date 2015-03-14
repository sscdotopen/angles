package io.ssc.angles.pipeline.titan

import java.util

import com.tinkerpop.blueprints.Vertex
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory

import scala.io.Source

/**
 * Created by xolor on 14.03.15.
 */
object ImportFollowers extends App {

  val logger = LoggerFactory.getLogger(getClass)

  val fileName = "followers.csv"
  val graph = TitanConnector.openDefaultGraph()

  /*  val batchGraph = BatchGraph.wrap(graph)
    batchGraph.setVertexIdKey("explorerId")
    batchGraph.setLoadingFromScratch(false)*/

  var done = 0

  // Register indices:
  val mgmt = graph.getManagementSystem
  var followsLabel = mgmt.getEdgeLabel("follows")
  if (followsLabel == null) {
    followsLabel = mgmt.makeEdgeLabel("follows").directed().make()
    /*mgmt.buildEdgeIndex(followsLabel, "followIndex", Direction.BOTH)*/
  }
  mgmt.commit()

  def getFirstElementOrNull(iterator: util.Iterator[Vertex]): Vertex = {
    if (iterator.hasNext)
      iterator.next()
    else
      null
  }

  logger.info("Reading input...")
  var lines = Source.fromFile(fileName).getLines().toList
  logger.info("Fire!")

  var tx = graph.buildTransaction().enableBatchLoading().start()


  lines.seq.foreach({ case line: String =>
    val values = StringUtils.split(line, ";")
    if (values.length != 2) {
      logger.warn("Expected length of two but got {} on line {}", values.length, line)
    } else {
      if (!"explorer".equals(values(0))) {
        val sourceExplorerId: String = values(0)
        val targetExplorerId: String = values(1)

        // Find the vertices in titan
        var sourceVertex = getFirstElementOrNull(tx.getVertices("explorerId", sourceExplorerId).iterator())
        var targetVertex = getFirstElementOrNull(tx.getVertices("explorerId", targetExplorerId).iterator())

        // Create the vertices if missing
        if (sourceVertex == null)
          sourceVertex = tx.addVertex(sourceExplorerId, "type", "explorer")
        if (targetVertex == null)
          targetVertex = tx.addVertex(targetExplorerId, "type", "explorer")

        sourceVertex.addEdge("follows", targetVertex)
      }
    }

    done += 1

    if (done % 1000 == 0)
      logger.info("Added {} edges", done)

    if (done % 100000 == 0) {
      logger.info("Explicitly committing progress...")
      tx.commit()
      tx = graph.buildTransaction().enableBatchLoading().start()
    }
  })

  logger.info("Final commit...")
  tx.commit()
  graph.commit()
  logger.info("Done!")

  graph.shutdown()

}
