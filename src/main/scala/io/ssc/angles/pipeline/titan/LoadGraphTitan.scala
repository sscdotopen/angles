package io.ssc.angles.pipeline.titan

import java.util
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

import com.google.common.collect.Lists
import com.thinkaurelius.titan.core.TitanGraph
import com.thinkaurelius.titan.core.attribute.Precision
import com.thinkaurelius.titan.core.util.TitanCleanup
import com.tinkerpop.blueprints.{Direction, Vertex}
import io.ssc.angles.pipeline.explorers.{CSVReader, CalculateClusters}
import org.slf4j.LoggerFactory
import scalikejdbc.ConnectionPool.MutableMap

import scala.collection.JavaConversions._
import scala.collection.mutable


/**
 * Created by xolor on 27.02.15.
 */
object LoadGraphTitan extends App {

  override def main(args: Array[String]) {
    val logger = LoggerFactory.getLogger(LoadGraphTitan.getClass)
    var graph: TitanGraph = TitanConnector.openDefaultGraph()

    logger.info("Removing existing titan graph ...")
    graph.shutdown()
    TitanCleanup.clear(graph)

    logger.info("Reopening titan graph ...")
    graph = TitanConnector.openDefaultGraph()
    
    logger.info("Adding indices...")
    registerIndices(graph)

    val vertexFile = "pairs.csv"
    val graphFile: String = "graph_jaccard.csv"

    logger.info("Reading vertices from CSV...")
    val workingList: Set[(String, String, String)] = CSVReader.readTuplesFromCSV(vertexFile)
    logger.info("Got {} pairs from CSV", workingList.size)

    logger.info("Adding vertices ...")

    var vertexMap = addVertices(graph, workingList)

    logger.info("Reading edges from CSV...")
    val graphData: Map[(String, String), Double] = CalculateClusters.readGraphCSV(graphFile)

    logger.info("Adding edges...")

    graphData.foreach { case ((leftString, rightString), weight) =>
      // Get or create nodes for left + right
      val leftNode = vertexMap.get(leftString).get
      val rightNode = vertexMap.get(rightString).get
      // Add edge to graph
      val edge = leftNode.addEdge("jaccard", rightNode)
      edge.setProperty("similarity", weight)
    }

    graph.commit()

    logger.info("Done. Shutting down Titan...")

    graph.shutdown()
  }


  def registerIndices(graph: TitanGraph): Unit = {
    val mgmt = graph.getManagementSystem
    val jaccardSimilarity = mgmt.makePropertyKey("similarity").dataType(classOf[Precision]).make()    // double is not allowed -> have to use precision!
    val similarity = mgmt.makeEdgeLabel("jaccard").make()
    mgmt.buildEdgeIndex(similarity, "jaccard_index", Direction.BOTH, jaccardSimilarity)
  }

  /**
   * Add vertices from a given list to a given titan graph and return a map String->Vertex of all inserted vertices.
   * @param graph The TitanGraph instance
   * @param workingList List of triples to add (explorerId : String, explorerName : String, uri : String)
   * @return
   */
  def addVertices(graph: TitanGraph, workingList: Set[(String, String, String)]): MutableMap[String, Vertex] = {
    // Build a map of all URLs a user has tweeted:
    var explorerUrlMap: ConcurrentHashMap[String, java.util.List[String]] = new ConcurrentHashMap[String, java.util.List[String]]
    workingList.par.foreach { case triple => {
      val explorerId = triple._1
      val uri = triple._3
      var newValue: util.List[String] = explorerUrlMap.getOrElse(explorerId, Collections.synchronizedList(new util.ArrayList[String]()))
      newValue += uri
      explorerUrlMap.update(explorerId, newValue)
    }
    }    
    
    var vertexMap = mutable.HashMap.empty[String, Vertex]
    workingList.foreach { case (pair: (String, String, String)) => {
      val explorerId = pair._1
      val explorerName = pair._2
      if (!vertexMap.contains(explorerName)) {
        val urls = explorerUrlMap.get(explorerId)
        val newVertex = graph.addVertex(explorerName)
        newVertex.setProperty("twitterId", explorerId)
        newVertex.setProperty("name", explorerName)
        newVertex.setProperty("urls", Lists.newArrayList(urls))
        vertexMap += ((explorerName, newVertex))
      }
    }
    }
    vertexMap
  }
}
