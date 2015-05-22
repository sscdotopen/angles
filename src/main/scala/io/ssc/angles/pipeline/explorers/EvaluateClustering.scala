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
    average((0 until l.size).flatMap { case i => (i + 1 until l.size).map { case j => l(i) getDistance l(j) } }.seq)
  }

  // calculate inter-cluster distance as distance between cluster centroids
  val interClusterDistance =
    (x: Iterable[RealVector], y: Iterable[RealVector]) =>
      centroid(x) getDistance centroid(y)

  val dunn = dunnIndex(clusters, intraClusterDistance, interClusterDistance)
  logger.info("Dunn index: {} (higher is better)", dunn)

  val daviesBouldin = daviesBouldinIndex(clusters)
  logger.info("Davies-Bouldin index: {} (lower is better)", daviesBouldin)

  def dunnIndex(clusters: ClusterSet[RealVector],
                intraDistanceMeasure: (Iterable[RealVector] => Double),
                interDistanceMeasure: (Iterable[RealVector], Iterable[RealVector]) => Double): Double = {
    logger.info("Calculating inter-cluster distance")
    val interDistance = {
      (0 until clusters.getNumClusters).flatMap { case i =>
        ((i + 1) until clusters.getNumClusters).map { case j =>
          val measure = interDistanceMeasure(clusters.getCluster(i), clusters.getCluster(j))
          measure
        }
      }
    }.min

    logger.info("Calculating intra-cluster distance")
    val intraDistance = {
      (0 until clusters.getNumClusters).map { case i =>
        intraDistanceMeasure(clusters.getCluster(i))
      }
    }.max

//    logger.info("Inter distance: {}", interDistance)
//    logger.info("Intra distance: {}", intraDistance)

    interDistance / intraDistance
  }

  def daviesBouldinIndex(clusterSet: ClusterSet[RealVector]) = {
    case class ClusterInfo(centroid: RealVector, avgDist: Double)

    val clusters = clusterSet.getClusters
    val centroids = clusters.map(c => centroid(c))

    val averageDistanceToCentroid = (x: (RealVector, Iterable[RealVector])) => average(x._2.map(node => node getDistance x._1))
    val distances = centroids.zip(clusters).map(averageDistanceToCentroid)

    val infos = centroids.zip(distances).map { x => ClusterInfo(x._1, x._2) }
    val foo = infos.map(c => Set(c)).map(c => c cross infos).map(x => x.filter(p => p._1 != p._2).map(p => (p._1.avgDist + p._1.avgDist) / (p._1.centroid getDistance p._2.centroid)).max)
    foo.sum / foo.size
  }

  def loadClusters(pairsFile: String, clusterFile: String): ClusterSet[RealVector] = {
    val explorerSpace = loadExplorerSpace(pairsFile)

    val clusters = ClusterReadWriter.readClusterFile(clusterFile)
    logger.info("Got {} clusters from CSV", clusters.getNumClusters)

    val set = new ClusterSet[RealVector]

    for (i <- 0 until clusters.getNumClusters) {
      val c = clusters.getCluster(i)
      val nonNullVectors = c.map(e => explorerSpace.getOrElse(e, null)).filter(v => v != null)
      if (nonNullVectors.size > 0) {
        set.newCluster()
        nonNullVectors.foreach(e => set.addExplorerToCurrentCluster(e))
      }
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
    def cross[Y](ys: Traversable[Y]) = (xs).flatMap { case x => (ys).map { case y => (x, y) } }
  }

  def average[T](ts: Traversable[T])(implicit num: Numeric[T]) = {
    if (ts.size != 0) {
      num.toDouble(ts.sum) / ts.size
    } else {
      logger.warn("Tried to calculate average on empty sequence.")
      0.0
    }
  }
}
