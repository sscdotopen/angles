package io.ssc.angles.pipeline.titan

import java.net.URI
import java.util
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

import com.thinkaurelius.titan.core.TitanGraph
import com.thinkaurelius.titan.core.attribute.Precision
import com.thinkaurelius.titan.core.util.TitanCleanup
import com.tinkerpop.blueprints.util.wrappers.batch.BatchGraph
import com.tinkerpop.blueprints.{Edge, TransactionalGraph, Vertex}
import io.ssc.angles.pipeline.explorers._
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.collection.mutable


/**
 * Created by xolor on 27.02.15.
 */
object LoadGraphTitan extends App {

  var logger = LoggerFactory.getLogger(LoadGraphTitan.getClass)

  def zipper(map1: Map[(String, String), Double], map2: Map[(String, String), Double]) = {
    for (key <- map1.keys ++ map2.keys)
    yield (key, map1.getOrElse(key, 0d).asInstanceOf[Double], map2.getOrElse(key, 0d).asInstanceOf[Double])
  }


  def readSimiliarities(): Map[(String, String), (Double, Double)] = {
    logger.info("Reading jaccard similarities...")
    val jaccardSimilarities = CalculateClusters.readGraphCSV("graph_jaccard.csv")
    logger.info("Reading cosine similarities...")
    val cosineSimilarities = CalculateClusters.readGraphCSV("graph_cosine.csv")

    var outMap = mutable.HashMap.empty[(String, String), (Double, Double)]

    // Merge the data, so that we don't have any reflexivity i.e. if there is (x,y) there may be no (y,x)
    logger.info("Merging similarities...")
    zipper(cosineSimilarities, jaccardSimilarities).foreach { case ((left, right), cosine, jaccard) =>
      var mapValue = outMap.getOrElse((left, right), outMap.getOrElse((right, left), (0d, 0d)))

      if (cosine != 0d && jaccard != 0d && !mapValue.equals((0d, 0d)))
        throw new IllegalStateException("Mysterious third?! Merging similarities encountered critical error on " +((left, right), cosine, jaccard))

      if (cosine != 0d)
        mapValue = (cosine, mapValue._2)
      if (jaccard != 0d)
        mapValue = (mapValue._1, jaccard)

      if (outMap.contains((right, left))) {
        outMap.put((right, left), mapValue)
      } else {
        outMap.put((left, right), mapValue)
      }
    }
    outMap.toMap
  }

  def addSimilarityEdges(graph: BatchGraph[_ <: TransactionalGraph], similarityMap: Map[(String, String), (Double, Double)], explorerVertexIdMap: util.HashMap[String, String]) = {
    val total = similarityMap.size
    var current = 0

    similarityMap.foreach { case ((in, out), (cosine, jaccard)) =>
      if (current % 50 == 0)
        logger.info("Adding similarity edge {}/{}", current, total)
      current += 1

      val outVertex = graph.getVertex(explorerVertexIdMap.get(in))
      val inVertex = graph.getVertex(explorerVertexIdMap.get(out))
      
      if (outVertex == null || inVertex == null) {
        logger.warn("Vertex for id {} or id {} is {}", in.toString, out.toString, null)
      } else {
        val edge = outVertex.addEdge("similiar", inVertex)
        edge.setProperty("cosine", cosine)
        edge.setProperty("jaccard", jaccard)
      }
    }
  }

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

    var batchGraph = BatchGraph.wrap(titanGraph)
    val vertexFile = "pairs.csv"

    val graphFile: String = "graph_jaccard.csv"
    logger.info("Reading vertices from CSV...")
    val workingList: Set[(String, String, String)] = CSVReader.readTuplesFromCSV(vertexFile)
    logger.info("Got {} pairs from CSV", workingList.size)

    logger.info("Reading cluster data")
    val jaccardClusters: ClusterSet = clusterReader.readClusterFile("communities_jaccard.tsv")

    val cosineClusters: ClusterSet = clusterReader.readClusterFile("communities_cosine.tsv")
    logger.info("Adding vertices ...")

    val vertexIdMap = addVertices(batchGraph, workingList, cosineClusters, jaccardClusters)
    
    val similarities = readSimiliarities()
    logger.info("Adding similarity edges...")
    addSimilarityEdges(batchGraph, similarities, vertexIdMap)

    batchGraph.commit()

    logger.info("Done. Shutting down Titan...")

    titanGraph.shutdown()
  }


  def registerIndices(graph: TitanGraph): Unit = {
    val INDEX_BACKEND: String = "search"

    val mgmt = graph.getManagementSystem

    mgmt.set("storage.cassandra.replication-factor", 2)

    // mixed index for "references"-edge
    val timesKey = mgmt.makePropertyKey("times").dataType(classOf[Precision]).make() // double is not allowed -> have to use precision!
    mgmt.buildIndex("references", classOf[Edge]).addKey(timesKey).buildMixedIndex(INDEX_BACKEND)

    // mixed indices for "similiar"-edge
    val jaccardSimilarityKey = mgmt.makePropertyKey("jaccard").dataType(classOf[Precision]).make() // double is not allowed -> have to use precision!
    val cosineSimilarityKey = mgmt.makePropertyKey("cosine").dataType(classOf[Precision]).make() // double is not allowed -> have to use precision!
    mgmt.buildIndex("similiar_cosine_jaccard", classOf[Edge]).addKey(cosineSimilarityKey).addKey(jaccardSimilarityKey).buildMixedIndex(INDEX_BACKEND)
    mgmt.buildIndex("similiar_cosine", classOf[Edge]).addKey(cosineSimilarityKey).buildMixedIndex(INDEX_BACKEND)
    mgmt.buildIndex("similiar_jaccard", classOf[Edge]).addKey(jaccardSimilarityKey).buildMixedIndex(INDEX_BACKEND)

    // Composite vertex index on property "explorerId"
    val explorerId = mgmt.makePropertyKey("explorerId").dataType(classOf[String]).make()
    mgmt.buildIndex("byExplorerId", classOf[Vertex]).addKey(explorerId).unique().buildCompositeIndex()
    // Mixed vertex index on property "explorerName"
    val explorerName = mgmt.makePropertyKey("explorerName").dataType(classOf[String]).make()
    mgmt.buildIndex("explorerName_search", classOf[Vertex]).addKey(explorerName).buildMixedIndex(INDEX_BACKEND)
    // Composite vertex index on property "explorerName"
    mgmt.buildIndex("explorerName", classOf[Vertex]).addKey(explorerName).buildCompositeIndex()
    // Mixed vertex index on property "host"
    val host = mgmt.makePropertyKey("host").dataType(classOf[String]).make()
    mgmt.buildIndex("host_search", classOf[Vertex]).addKey(host).buildMixedIndex(INDEX_BACKEND)
    // Mixed vertex index on property "host"
    mgmt.buildIndex("host", classOf[Vertex]).addKey(host).buildCompositeIndex()
    // Composite vertex index on property "vertexKey"
    val vertexKey = mgmt.makePropertyKey("key").dataType(classOf[String]).make()
    mgmt.buildIndex("key", classOf[Vertex]).addKey(vertexKey).buildCompositeIndex()

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
  def addVertices(graph: BatchGraph[_ <: TransactionalGraph], workingList: Set[(String, String, String)], jaccardClusters: ClusterSet, cosineClusters: ClusterSet) = {

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
    val vertexIdMap = new util.HashMap[String, String]() // Map explorerName (?) to temporaryVertex id

    // Setup key settings
    graph.setVertexIdKey("key")
    graph.setEdgeIdKey("key")

    var id = 0
    var currentExplorer = 0
    val totalExplorers: Int = explorerHostCountMap.size

    explorerHostCountMap.foreach { case (explorerId: String, urls: Map[String, Int]) =>
      if (currentExplorer % 50 == 0)
        logger.info("Adding explorer {}/{}", currentExplorer, totalExplorers)
      currentExplorer += 1
      
      val explorerName: String = explorerIdNameMap.get(explorerId).get

      val vertexId = vertexIdMap.getOrDefault(explorerName, (() => {
        id += 1
        id.toString
      }).apply())

      val explorerNode = graph.addVertex(vertexId, "explorerId", explorerId)
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
          var urlVertexId: String = null

          if (vertexIdMap.containsKey("___" + host)) {
            urlVertexId = vertexIdMap.get("___" + host)
          } else {
            id += 1
            vertexIdMap.put("___" + host, id.toString)
            urlVertexId = id.toString
          }

          var urlNode: Vertex = graph.getVertex(urlVertexId)
          if (urlNode == null)
            urlNode = graph.addVertex(urlVertexId, "host", host)

          val newEdge = graph.addEdge(null, explorerNode, urlNode, "references")
          newEdge.setProperty("times", count)
        case _ =>
      }
    }
    vertexIdMap
  }

}
