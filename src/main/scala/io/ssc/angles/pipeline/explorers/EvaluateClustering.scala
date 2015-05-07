package io.ssc.angles.pipeline.explorers

import org.apache.commons.math3.linear.{OpenMapRealVector, RealVector}
import org.slf4j.LoggerFactory

/**
 * Created by niklas on 07.05.15.
 */
object EvaluateClustering extends App {
  val logger = LoggerFactory.getLogger(EvaluateClustering.getClass)

  val pairsFile = args(0)
  val clusterFile = args(1)

  val clusters = loadClusters(pairsFile, clusterFile)

  // calculate intra-cluster distance as average of all distances
  val intraClusterDistance = (x: Iterable[RealVector]) => {
    val l = x.toList
    average((0 until l.size).par.flatMap { case i => (i + 1 until l.size).par.map { case j => l(i) getDistance l(j) }}.seq)
  }

  // calculate inter-cluster distance by finding the two most distant points in both clusters
//  val interClusterDistance =
//    (x: Iterable[RealVector], y: Iterable[RealVector]) =>
//      (x cross y).map((x : (RealVector, RealVector)) => x._1 getDistance x._2).max

  // calculate inter-cluster distance as distance between cluster centroids
  val interClusterDistance =
    (x: Iterable[RealVector], y: Iterable[RealVector]) =>
      centroid(x) getDistance centroid(y)

  val dunn = dunnIndex(clusters, intraClusterDistance, interClusterDistance)
  logger.info("Dunn index: {}", dunn)

  def dunnIndex(clusters: ClusterSet[RealVector],
                intraDistanceMeasure: (Iterable[RealVector] => Double),
                interDistanceMeasure: (Iterable[RealVector], Iterable[RealVector]) => Double): Double = {
    logger.info("Calculating inter-cluster distance")
    val interDistance = {
      (0 until clusters.getNumClusters).par.flatMap { case i =>
        logger.info("Processing inter distance on cluster {}", i)
        ((i + 1) until clusters.getNumClusters).par.map { case j =>
          val measure = interDistanceMeasure(clusters.getCluster(i), clusters.getCluster(j))
          logger.info("i = {}, j = {}, measure = {}", i.asInstanceOf[Object], j.asInstanceOf[Object], measure.asInstanceOf[Object])
          measure
        }
      }
    }.min

    logger.info("Calculating intra-cluster distance")
    val intraDistance = {
      (0 until clusters.getNumClusters).par.map { case i =>
        logger.info("Processing intra distance on cluster {}", i)
        intraDistanceMeasure(clusters.getCluster(i)) }
    }.max

    logger.info("Inter distance: {}", interDistance)
    logger.info("Intra distance: {}", intraDistance)

    interDistance / intraDistance
  }

  def loadClusters(pairsFile: String, clusterFile: String): ClusterSet[RealVector] = {
    val explorerSpace = loadExplorerSpace(pairsFile)

    val clusters = ClusterReadWriter.readClusterFile(clusterFile)
    logger.info("Got {} clusters from CSV", clusters.getNumClusters)

    val set = new ClusterSet[RealVector]

    for (i <- 0 until clusters.getNumClusters) {
      set.newCluster()
      val c = clusters.getCluster(i)
      c.map(e => explorerSpace.getOrElse(e, null)).foreach(e => set.addExplorerToCurrentCluster(e))
    }

    set
  }

  def loadExplorerSpace(pairsFile: String) = {
    val workingList: List[ExplorerUriPair] = CSVReader.readExplorerPairsFromCSV(pairsFile)
    logger.info("Got {} pairs from CSV", workingList.size)

    val explorerSpace = new GraphGenerator().buildExplorerSpace(workingList, BuildExplorerGraph.uriToSecondLevelDomain)
    logger.info("Got {} explorers", explorerSpace.size)

    explorerSpace
  }

  def centroid(cluster: Iterable[RealVector]): RealVector = {
    val dimensions = cluster.iterator.next().getDimension
    var centroid: RealVector = new OpenMapRealVector(dimensions)
    cluster.foreach(v => centroid = centroid add v)
    centroid.mapDivide(cluster.size)
  }

  implicit class Crossable[X](xs: Traversable[X]) {
    def cross[Y](ys: Traversable[Y]) = for { x <- xs; y <- ys } yield (x, y)
  }
  
  def average[T]( ts: Traversable[T] )( implicit num: Numeric[T] ) = {
    num.toDouble( ts.sum ) / ts.size
  }
}
