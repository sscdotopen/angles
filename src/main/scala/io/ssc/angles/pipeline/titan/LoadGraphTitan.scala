package io.ssc.angles.pipeline.titan

import java.net.URI
import java.util
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

import com.thinkaurelius.titan.core.TitanGraph
import com.thinkaurelius.titan.core.attribute.Precision
import com.thinkaurelius.titan.core.util.TitanCleanup
import com.tinkerpop.blueprints.{Direction, Edge, Vertex}
import io.ssc.angles.pipeline.explorers.{CSVReader, ClusterReadWriter, ClusterSet, HelperUtils}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.collection.mutable


/**
 * Created by xolor on 27.02.15.
 */
object LoadGraphTitan extends App {

  var logger = LoggerFactory.getLogger(LoadGraphTitan.getClass)

  override def main(args: Array[String]) {
    val clusterReader = new ClusterReadWriter
    logger = LoggerFactory.getLogger(LoadGraphTitan.getClass)
    var titanGraph: TitanGraph = TitanConnector.openDefaultGraph()

    logger.info("Removing existing titan graph ...")
    titanGraph.shutdown()
    TitanCleanup.clear(titanGraph)

    logger.info("Reopening titan graph ...")
    titanGraph = TitanConnector.openDefaultGraph()

    logger.info("Adding indices...")
    registerIndices(titanGraph)

    if (titanGraph.isClosed) {
      logger.info("Reopening graph again...")
      titanGraph = TitanConnector.openDefaultGraph()
    }

    //var batchGraph = BatchGraph.wrap(titanGraph)

    val vertexFile = "pairs.csv"
    val graphFile: String = "graph_jaccard.csv"

    logger.info("Reading vertices from CSV...")
    val workingList: Set[(String, String, String)] = CSVReader.readTuplesFromCSV(vertexFile)
    logger.info("Got {} pairs from CSV", workingList.size)

    logger.info("Reading cluster data")
    val jaccardClusters: ClusterSet = clusterReader.readClusterFile("communities_jaccard.tsv")
    val cosineClusters: ClusterSet = clusterReader.readClusterFile("communities_cosine.tsv")
    logger.info("Adding vertices ...")

    addVertices(titanGraph, workingList, cosineClusters, jaccardClusters)

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
    titanGraph.commit()

    logger.info("Done. Shutting down Titan...")

    titanGraph.shutdown()
  }


  def registerIndices(graph: TitanGraph): Unit = {
    val INDEX_BACKEND: String = "search"

    val mgmt = graph.getManagementSystem

    mgmt.set("storage.cassandra.replication-factor", 2)

    // Vertex label and indices
    val explorerLabel = mgmt.makeVertexLabel("explorer").make()
    val hostLabel = mgmt.makeVertexLabel("host").make()

    // Edge type and index for "jaccard"-edge
    val jaccardSimilarityKey = mgmt.makePropertyKey("similarity").dataType(classOf[Precision]).make() // double is not allowed -> have to use precision!
    val jaccardSimilarityLabel = mgmt.makeEdgeLabel("jaccard").make()
    mgmt.buildEdgeIndex(jaccardSimilarityLabel, "jaccard_edge_index", Direction.BOTH, jaccardSimilarityKey)

    // Mixed index for "references"-edge and edge index
    val timesKey = mgmt.makePropertyKey("times").dataType(classOf[Precision]).make() // double is not allowed -> have to use precision!
    val referencesLabel = mgmt.makeEdgeLabel("references").make()
    mgmt.buildIndex("references", classOf[Edge]).addKey(timesKey).buildMixedIndex(INDEX_BACKEND)
    mgmt.buildEdgeIndex(referencesLabel, "references_edge_index", Direction.BOTH, timesKey)

    // Composite vertex index on property "explorerId"
    val explorerId = mgmt.makePropertyKey("explorerId").dataType(classOf[String]).make()
    mgmt.buildIndex("byExplorerId", classOf[Vertex]).addKey(explorerId).unique().buildCompositeIndex()
    // Mixed vertex index on property "explorerName"
    val explorerName = mgmt.makePropertyKey("explorerName").dataType(classOf[String]).make()
    mgmt.buildIndex("explorerName_search", classOf[Vertex]).addKey(explorerName).buildMixedIndex(INDEX_BACKEND)
    // Composite vertex index on property "explorerName"
    mgmt.buildIndex("explorerName", classOf[Vertex]).addKey(explorerName).unique().buildCompositeIndex()
    // Mixed vertex index on property "host"
    val host = mgmt.makePropertyKey("host").dataType(classOf[String]).make()
    mgmt.buildIndex("host_search", classOf[Vertex]).addKey(host).buildMixedIndex(INDEX_BACKEND)
    // Mixed vertex index on property "host"
    mgmt.buildIndex("host", classOf[Vertex]).addKey(host).unique().buildCompositeIndex()
    // Composite vertex index on property "vertexKey"
    //val vertexKey = mgmt.makePropertyKey("key").dataType(classOf[String]).make()
    //mgmt.buildIndex("key", classOf[Vertex]).addKey(vertexKey).buildCompositeIndex()

    // Composite vertex index on property "tweetedUrls"
    val tweetedUrlsKey = mgmt.makePropertyKey("tweetedUrls").dataType(classOf[Array[String]]).make()
    mgmt.buildIndex("tweetedUrls", classOf[Vertex]).addKey(tweetedUrlsKey).buildCompositeIndex()

    // Composite and mixed vertex index on property "cosineCluster"
    val cosineClusterKey = mgmt.makePropertyKey("cosineCluster").dataType(classOf[Integer]).make()
    mgmt.buildIndex("cosineCluster", classOf[Vertex]).addKey(cosineClusterKey).buildCompositeIndex()
    mgmt.buildIndex("cosineCluster_search", classOf[Vertex]).addKey(cosineClusterKey).buildMixedIndex(INDEX_BACKEND)

    // Composite and mixed vertex index on property "jaccardCluster"
    val jaccardClusterKey = mgmt.makePropertyKey("jaccardCluster").dataType(classOf[Integer]).make()
    mgmt.buildIndex("jaccardCluster", classOf[Vertex]).addKey(jaccardClusterKey).buildCompositeIndex()
    mgmt.buildIndex("jaccardCluster_search", classOf[Vertex]).addKey(jaccardClusterKey).buildMixedIndex(INDEX_BACKEND)

    // Composite and mixed vertex index on both properties "jaccardCluster" and "cosineCluster"
    mgmt.buildIndex("jaccardCosineCluster", classOf[Vertex]).addKey(jaccardClusterKey).addKey(cosineClusterKey).buildCompositeIndex()
    mgmt.buildIndex("jaccardCosineCluster_search", classOf[Vertex]).addKey(jaccardClusterKey).addKey(cosineClusterKey).buildMixedIndex(INDEX_BACKEND)


    mgmt.commit()
  }

  /**
   * Add vertices from a given list to a given titan graph and return a map String->Vertex of all inserted vertices.
   * @param graph The TitanGraph instance
   * @param workingList List of triples to add (explorerId : String, explorerName : String, uri : String)
   * @return
   */
  def addVertices(graph: TitanGraph, workingList: Set[(String, String, String)], jaccardClusters: ClusterSet, cosineClusters: ClusterSet) = {

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
        case e: Exception => logger.warn("{}", e.getMessage)
      }
    }
    }
    val explorerIdNameMap: Map[String, String] = workingList.par.map((t: Tuple3[String, String, String]) => (t._1, t._2)).toMap.seq
    val explorerHostCountMap: mutable.Map[String, Map[String, Int]] = explorerUrlMap.map((s: (String, util.List[String])) => (s._1, s._2.groupBy(identity).mapValues(_.size)))
    val vertexIdMap = new util.HashMap[String, Vertex]()

    // Setup key settings
    //graph.setVertexIdKey("key")
    //graph.setEdgeIdKey("key")

    var id = 0

    explorerHostCountMap.foreach { case (explorerId: String, urls: Map[String, Int]) =>
      /*var vertex : Vertex = vertexIdMap.getOrDefault(explorerId, (() => {
        id += 1; id.toString
      }).apply())*/

      val explorerNode = graph.addVertexWithLabel("explorer")
      explorerNode.setProperty("explorerId", explorerId)
      val explorerName: String = explorerIdNameMap.get(explorerId).get
      explorerNode.setProperty("explorerName", explorerName)
      // Find cosine cluster for explorer
      val cosineClusterIdForExplorer: util.Set[Int] = cosineClusters.getClusterIdsForExplorer(explorerName)
      if (cosineClusterIdForExplorer.size() != 0) {
        explorerNode.setProperty("cosineCluster", cosineClusterIdForExplorer.iterator().next())
      }
      // Find jaccard cluster for explorer
      val jaccardClusterIdsForExplorer: util.Set[Int] = jaccardClusters.getClusterIdsForExplorer(explorerName)
      if (jaccardClusterIdsForExplorer.size() != 0) {
        explorerNode.setProperty("jaccardCluster", jaccardClusterIdsForExplorer.iterator().next())
      }
      //explorerNode.setProperty("tweetedUrls", explorerUrlMap.get(explorerId).toArray.asInstanceOf[Array[String]])

      urls.foreach {
        case (host: String, count: Int) =>
          var hostNode: Vertex = null

          if (vertexIdMap.containsKey(host)) {
            hostNode = vertexIdMap.get(host)
          } else {
            hostNode = graph.addVertexWithLabel("host")
            hostNode.setProperty("host", host)
            vertexIdMap.put(host, hostNode)
          }

          val newEdge = graph.addEdge(null, explorerNode, hostNode, "references")
          newEdge.setProperty("times", count)
        case _ =>
      }
    }
  }

}
