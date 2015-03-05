package io.ssc.angles.pipeline.titan

import java.net.URI
import java.util
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

import com.thinkaurelius.titan.core.TitanGraph
import com.thinkaurelius.titan.core.attribute.Precision
import com.thinkaurelius.titan.core.util.TitanCleanup
import com.tinkerpop.blueprints.util.wrappers.batch.BatchGraph
import com.tinkerpop.blueprints.{Direction, TransactionalGraph, Vertex}
import io.ssc.angles.pipeline.explorers.CSVReader
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.collection.mutable


/**
 * Created by xolor on 27.02.15.
 */
object LoadGraphTitan extends App {

  var logger = LoggerFactory.getLogger(LoadGraphTitan.getClass)

  override def main(args: Array[String]) {
    logger = LoggerFactory.getLogger(LoadGraphTitan.getClass)
    var titanGraph: TitanGraph = TitanConnector.openDefaultGraph()

    logger.info("Removing existing titan graph ...")
    titanGraph.shutdown()
    TitanCleanup.clear(titanGraph)

    logger.info("Reopening titan graph ...")
    titanGraph = TitanConnector.openDefaultGraph()

    logger.info("Adding indices...")
    registerIndices(titanGraph)

    titanGraph.commit()

    var batchGraph = BatchGraph.wrap(titanGraph)

    val vertexFile = "pairs.csv"
    val graphFile: String = "graph_jaccard.csv"

    logger.info("Reading vertices from CSV...")
    val workingList: Set[(String, String, String)] = CSVReader.readTuplesFromCSV(vertexFile)
    logger.info("Got {} pairs from CSV", workingList.size)

    logger.info("Adding vertices ...")

    addVertices(batchGraph, workingList)

    /*logger.info("Reading edges from CSV...")
    val graphData: Map[(String, String), Double] = CalculateClusters.readGraphCSV(graphFile)

    logger.info("Adding edges...")

    graphData.foreach { case ((leftString : String, rightString: String), weight: Double) =>
      // Get or create nodes for left + right
      val leftNode = vertexMap.get(leftString)
      val rightNode = vertexMap.get(rightString)
      // Add edge to graph
      try {
        batchGraph.addEdge(null, leftNode, rightNode, "similarity", weight.asInstanceOf[Object])
      } catch {
        case e: NullPointerException => logger.error("Error adding edge", e)
      }
    }*/
    batchGraph.commit()

    logger.info("Done. Shutting down Titan...")

    titanGraph.shutdown()
  }


  def registerIndices(graph: TitanGraph): Unit = {
    val mgmt = graph.getManagementSystem

    // Edge type and index for "jaccard"-edge
    val jaccardSimilarity = mgmt.makePropertyKey("similarity").dataType(classOf[Precision]).make() // double is not allowed -> have to use precision!
    val similarity = mgmt.makeEdgeLabel("jaccard").make()
    mgmt.buildEdgeIndex(similarity, "jaccard_index", Direction.BOTH, jaccardSimilarity)

    // Edge type and index for "references"-edge
    val times = mgmt.makePropertyKey("times").dataType(classOf[Precision]).make() // double is not allowed -> have to use precision!
    val references = mgmt.makeEdgeLabel("references").make()
    mgmt.buildEdgeIndex(references, "references", Direction.BOTH, times)

    // Vertex index on property "explorerId"
    val explorerId = mgmt.makePropertyKey("explorerId").dataType(classOf[String]).make()
    mgmt.buildIndex("byExplorerId", classOf[Vertex]).addKey(explorerId).buildCompositeIndex()
    // Vertex index on property "explorerName"
    val explorerName = mgmt.makePropertyKey("explorerName").dataType(classOf[String]).make()
    mgmt.buildIndex("byExplorerName", classOf[Vertex]).addKey(explorerId).buildCompositeIndex()
    // Vertex index on property "url"
    val url = mgmt.makePropertyKey("url").dataType(classOf[String]).make()
    mgmt.buildIndex("byUrl", classOf[Vertex]).addKey(url).buildCompositeIndex()
    // Vertex index on property "vertexKey"
    val vertexKey = mgmt.makePropertyKey("key").dataType(classOf[String]).make()
    mgmt.buildIndex("key", classOf[Vertex]).addKey(vertexKey).buildCompositeIndex()
  }

  /**
   * Add vertices from a given list to a given titan graph and return a map String->Vertex of all inserted vertices.
   * @param graph The TitanGraph instance
   * @param workingList List of triples to add (explorerId : String, explorerName : String, uri : String)
   * @return
   */
  def addVertices(graph: BatchGraph[_ <: TransactionalGraph], workingList: Set[(String, String, String)]) = {

    // Build a map of all URLs a user has tweeted:
    var explorerUrlMap: ConcurrentHashMap[String, java.util.List[String]] = new ConcurrentHashMap[String, java.util.List[String]]
    workingList.par.foreach { case triple => {
      try {
        val explorerId = triple._1
        val uri = URI.create(triple._3)
        var newValue: util.List[String] = explorerUrlMap.getOrElse(explorerId, Collections.synchronizedList(new util.ArrayList[String]()))
        newValue += uri.getHost
        explorerUrlMap.update(explorerId, newValue)
      } catch {
        case e : Exception => logger.warn("{}", e.getMessage)
      }
    }
    }

    val explorerUrlCountMap: mutable.Map[String, Map[String, Int]] = explorerUrlMap.map((s: (String, util.List[String])) => (s._1, s._2.groupBy(identity).mapValues(_.size)))

    //s.groupBy(identity).mapValues(_.size)

    val vertexIdMap = new util.HashMap[String, String]()

    // Setup key settings
    graph.setVertexIdKey("key")
    graph.setEdgeIdKey("id")    
    
    var id = 0

    explorerUrlCountMap.foreach { case (explorerId: String, urls: Map[String, Int]) =>
      var vertexId = vertexIdMap.getOrDefault(explorerId, (() => {id += 1; id.toString}).apply())

      val explorerNode = graph.addVertex(vertexId, "explorerId", explorerId)

      urls.foreach {
        case (url :String, count: Int) =>
          vertexId = vertexIdMap.getOrDefault(url, (() => {id += 1; id.toString}).apply())
          var urlNode : Vertex = graph.getVertex(vertexId)
          if (urlNode == null)
            urlNode = graph.addVertex(vertexId, "url", url)
          
          val newEdge = graph.addEdge(null, explorerNode, urlNode, "references")
          newEdge.setProperty("times", count)
        case _ => logger.warn("Matching failed")
      }
    }





    /*var urlVertexMap = mutable.HashMap.empty[String, Vertex]
    var explorerVertexMap = ParMap.empty[String, Vertex]
    val explorerCount = explorerUrlMap.size()
    var i = 1
    workingList.par.foreach { case (pair: (String, String, String)) => {
      val explorerId = pair._1
      val explorerName = pair._2
      if (!explorerVertexMap.contains(explorerName)) {
        i += 1
        logger.info("Importing explorer {} ({}/{})", explorerName, String.valueOf(i), String.valueOf(explorerCount))
        val urls = explorerUrlMap.get(explorerId)
        // Import explorer vertex
        val newExplorerVertex = graph.addVertex(explorerName, "twitterId", explorerId, "name", explorerName)
        /*newExplorerVertex.setProperty("twitterId", explorerId)
        newExplorerVertex.setProperty("name", explorerName)*/
        //newVertex.setProperty("urls", Lists.newArrayList(urls))
        explorerVertexMap += ((explorerName, newExplorerVertex))

        // Import referenced URLs for the current explorer
        urls.foreach { case url: String => {
          var urlVertex : Vertex = null
          if (urlVertexMap.contains(url)) {
            urlVertex = urlVertexMap.get(url).get
          } else {
            urlVertex = graph.addVertex(url, "url", url) // TODO: Save (also) mapped URI
            urlVertexMap.put(url, urlVertex)
          }          
          try {
            newExplorerVertex.addEdge("references", urlVertex) // FIXME: add property "times"
          } catch {
            case e: Exception => logger.error("An error has occurred: ", e)
          }
        }
        }
      }
    }*/
  }

}
