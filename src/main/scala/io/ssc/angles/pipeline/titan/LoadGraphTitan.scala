package io.ssc.angles.pipeline.titan

import java.net.URI
import java.util
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

import com.thinkaurelius.titan.core.TitanGraph
import com.thinkaurelius.titan.core.attribute.Precision
import com.thinkaurelius.titan.core.util.TitanCleanup
import com.tinkerpop.blueprints.util.wrappers.batch.BatchGraph
import com.tinkerpop.blueprints.{Direction, Edge, TransactionalGraph, Vertex}
import io.ssc.angles.pipeline.explorers.{HelperUtils, CSVReader}
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
    val INDEX_BACKEND: String = "search"
    
    val mgmt = graph.getManagementSystem

    // Edge type and index for "jaccard"-edge
    val jaccardSimilarity = mgmt.makePropertyKey("similarity").dataType(classOf[Precision]).make() // double is not allowed -> have to use precision!
    val similarity = mgmt.makeEdgeLabel("jaccard").make()
    mgmt.buildEdgeIndex(similarity, "jaccard_index", Direction.BOTH, jaccardSimilarity)

    // mixed index for "references"-edge
    val timesKey = mgmt.makePropertyKey("times").dataType(classOf[Precision]).make() // double is not allowed -> have to use precision!
    mgmt.buildIndex("references", classOf[Edge]).addKey(timesKey).buildMixedIndex(INDEX_BACKEND)

    // Composite vertex index on property "explorerId"
    val explorerId = mgmt.makePropertyKey("explorerId").dataType(classOf[String]).make()
    mgmt.buildIndex("byExplorerId", classOf[Vertex]).addKey(explorerId).unique().buildCompositeIndex()
    // Mixed vertex index on property "explorerName"
    val explorerName = mgmt.makePropertyKey("explorerName").dataType(classOf[String]).make()
    mgmt.buildIndex("explorerName_search", classOf[Vertex]).addKey(explorerName).buildMixedIndex(INDEX_BACKEND)
    // Composite vertex index on property "explorerName"
    mgmt.buildIndex("explorerName", classOf[Vertex]).addKey(explorerName).buildCompositeIndex()
    // Mixed vertex index on property "host"
    val url = mgmt.makePropertyKey("host").dataType(classOf[String]).make()
    mgmt.buildIndex("host_search", classOf[Vertex]).addKey(url).buildMixedIndex(INDEX_BACKEND)
    // Mixed vertex index on property "host"
    mgmt.buildIndex("host", classOf[Vertex]).addKey(url).buildCompositeIndex()
    // Composite vertex index on property "vertexKey"
    val vertexKey = mgmt.makePropertyKey("key").dataType(classOf[String]).make()
    mgmt.buildIndex("key", classOf[Vertex]).addKey(vertexKey).buildCompositeIndex()

    // Composite vertex index on roperty "tweetedUrls"
    val tweetedUrlsKey = mgmt.makePropertyKey("tweetedUrls").dataType(classOf[Array[String]]).make()
    mgmt.buildIndex("tweetedUrls", classOf[Vertex]).addKey(tweetedUrlsKey).buildCompositeIndex()


    mgmt.commit()
  }

  /**
   * Add vertices from a given list to a given titan graph and return a map String->Vertex of all inserted vertices.
   * @param graph The TitanGraph instance
   * @param workingList List of triples to add (explorerId : String, explorerName : String, uri : String)
   * @return
   */
  def addVertices(graph: BatchGraph[_ <: TransactionalGraph], workingList: Set[(String, String, String)]) = {

    // Build a map of all URLs a user has tweeted:
    val explorerUrlMap: ConcurrentHashMap[String, java.util.List[String]] = new ConcurrentHashMap[String, java.util.List[String]]
    workingList.par.foreach { case triple => {
      try {
        val explorerId = triple._1
        val uriTemp: String = HelperUtils.escapeURIString(triple._3)
        val uri = URI.create(uriTemp)
        var newValue: util.List[String] = explorerUrlMap.getOrElse(explorerId, Collections.synchronizedList(new util.ArrayList[String]()))
        newValue += uri.getHost
        explorerUrlMap.update(explorerId, newValue)
      } catch {
        case e : Exception => logger.warn("{}", e.getMessage)
      }
    }
    }
    val explorerIdNameMap: Map[String, String] = workingList.par.map((t : Tuple3[String, String, String]) => (t._1, t._2)).toMap.seq
    val explorerHostCountMap: mutable.Map[String, Map[String, Int]] = explorerUrlMap.map((s: (String, util.List[String])) => (s._1, s._2.groupBy(identity).mapValues(_.size)))
    val vertexIdMap = new util.HashMap[String, String]()

    // Setup key settings
    graph.setVertexIdKey("key")
    graph.setEdgeIdKey("key")
    
    var id = 0

    explorerHostCountMap.foreach { case (explorerId: String, urls: Map[String, Int]) =>
      var vertexId = vertexIdMap.getOrDefault(explorerId, (() => {id += 1; id.toString}).apply())

      val explorerNode = graph.addVertex(vertexId, "explorerId", explorerId)
      explorerNode.setProperty("explorerName", explorerIdNameMap.get(explorerId))
      
      //explorerNode.setProperty("tweetedUrls", explorerUrlMap.get(explorerId).toArray.asInstanceOf[Array[String]])

      urls.foreach {
        case (host :String, count: Int) =>
          var urlVertexId : String = null
          
          if (vertexIdMap.containsKey(host)) {
            urlVertexId = vertexIdMap.get(host)
          } else {
            id += 1
            vertexIdMap.put(host, id.toString)
            urlVertexId = id.toString
          }

          var urlNode : Vertex = graph.getVertex(urlVertexId)
          if (urlNode == null)
            urlNode = graph.addVertex(urlVertexId, "host", host)
          
          val newEdge = graph.addEdge(null, explorerNode, urlNode, "references")
          newEdge.setProperty("times", count)
        case _ =>
      }
    }
  }

}
