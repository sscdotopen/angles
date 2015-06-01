package io.ssc.angles.pipeline.explorers

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.parallel.immutable

object AntiPairs extends App {
  val logger = LoggerFactory.getLogger(AntiPairs.getClass)
  
  val antiPairsFile = args(0)
  val clustersFile = args(1)

  logger.info("Loading anti-pairs")
  val antiPairs = loadAntiFile(antiPairsFile)

  logger.info("Loading cluster file")
  val clusters = ClusterReadWriter.readClusterFile(clustersFile)
  logger.info("Got {} clusters from CSV", clusters.getNumClusters)

  val nonSingleClusters = new ClusterSet[String]

  for (c <- clusters.getClusters()) {
    if (c.size > 1) {
      nonSingleClusters.newCluster()
      for (u <- c) {
        nonSingleClusters.addExplorerToCurrentCluster(u)
      }
    }
  }

  logger.info("Got {} non-single clusters", nonSingleClusters.getNumClusters)

  val affected = getAffectedClusters(clusters, antiPairs)

  logger.info("")
  logger.info("Stats with single clusters")
  printStats(clusters, affected, antiPairs)

  val nonSingleAffected = getAffectedClusters(nonSingleClusters, antiPairs)

  logger.info("")
  logger.info("Stats without single clusters")
  printStats(nonSingleClusters, nonSingleAffected, antiPairs)

  logger.info("")

  logger.info("Total number of single clusters: {}", clusters.getClusters().count(i => i.size == 1))
  logger.info("Total number of non-single clusters: {}", clusters.getClusters().count(i => i.size > 1))

  def printStats(clusters: ClusterSet[String], affected: mutable.Buffer[String], antiPairs: List[(String, String)]) = {
    logger.info("Checked {} anti-pairs. Found {} pairs in the same cluster.", antiPairs.size, affected.size)
    logger.info("This is a ratio of {}%.", affected.size / antiPairs.size.toDouble * 100)

    if (affected.size > 0) {
      logger.info("{} clusters are affected", affected.distinct.size)
      //    affected.distinct.foreach { c => logger.info("{}", c) }
      logger.info("This is a ratio of {}%", affected.distinct.size.toDouble / clusters.getNumClusters.toDouble * 100)
    }
  }

  def getAffectedClusters(clusters: ClusterSet[String], antiPairs: List[(String, String)]) = {
    val affected = mutable.Buffer.empty[String]
    for (p <- antiPairs) {
      val cluster1 = clusters.getClusterIdsForExplorer(p._1)
      val cluster2 = clusters.getClusterIdsForExplorer(p._2)
      if (cluster1 == cluster2 && !cluster1.isEmpty) {
                logger.warn("{} and {} are both in cluster {} {} {}", p._1, p._2, cluster1, cluster2)
        affected += cluster1.toString
      }
    }
    affected
  }
  
  def loadAntiFile(file: String): List[(String, String)] = {
    val path = Paths.get(file)
    val reader = Files.newBufferedReader(path, Charset.forName("UTF-8"))
    val resultList = mutable.MutableList.empty[(String, String)]
    
    var line = reader.readLine()
    while (line != null) {
      if (!StringUtils.isEmpty(line)) {
        val separated = StringUtils.split(line, ";", 2)
        if (separated.size != 2) {
          logger.warn("The following CSV row must consist of exactly two values: {}", line)
        } else {
          val explorer1 = StringUtils.trim(separated(0))
          val explorer2 = StringUtils.trim(separated(1))
          try {
            resultList += ((explorer1, explorer2))
          } catch {
            case e: IllegalArgumentException => //logger.warn(e.getMessage)
          }
        }
      }
      line = reader.readLine()
    }
    reader.close
    
    resultList.toList
  }
}
