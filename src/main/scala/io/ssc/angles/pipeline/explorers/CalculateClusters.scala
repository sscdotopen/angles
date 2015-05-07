package io.ssc.angles.pipeline.explorers

import java.nio.file.{Files, Paths}

import com.google.common.collect.SetMultimap
import scala.io.Source
import org.slf4j.LoggerFactory

/**
 * Executable for calculating clusters.
 * Parameters:
 * - input csv file describing the graph
 * - suffix of the outfiles -> i.e. communities_SUFFIX.tsv graph_SUFFIX.png
 */
object CalculateClusters extends App {

  var logger = LoggerFactory.getLogger(CalculateClusters.getClass)

  def addMissingExplorers(clusterMap: SetMultimap[Int, String]): Unit = {
    logger.info("Adding missing explorer to separate cluster...")

    val explorerFilename = "explorers_all.tsv"
    val explorers = Source.fromFile(explorerFilename).getLines().toList

    if (explorers.size == 0) {
      logger.warn("No explorers found - check explorers_all.tsv")
      return
    }

    var i = -1

    // Put each explorer without cluster in a separate cluster
    explorers.foreach { s =>
      if (!clusterMap.containsValue(s)) {
        clusterMap.put(-1, s)
        i -= 1
      }
    }
  }

  override def main(args: Array[String]) {
    logger = LoggerFactory.getLogger(CalculateClusters.getClass)

    if (args.length != 2) {
      println("Usage: INCSV SUFFIX")
      println("  INCSV    name of the graph csv")
      println("  SUFFIX   suffix of the outfiles i.e. communities_SUFFIX.tsv and graph_SUFFIX.png")
      System.exit(255)
    }

    val inFile = args(0)
    val suffix = args(1)
    val communityFile: String = "communities_" + suffix + ".tsv"
    val graphFile: String = "graph_" + suffix + ".png"

    val graphData = CSVReader.readGraphCSV(inFile)
    val gephiManager = new GephiManager
    gephiManager.loadGraphMap(graphData, false)
    gephiManager.runOpenOrdLayout()
    val jaccardClusterMap = gephiManager.runChineseWhispersClusterer()
    ClusterReadWriter.writeClusterFile(communityFile, jaccardClusterMap)
    gephiManager.exportGraphToPNGImage(graphFile, 16384, 16384)
  }
}
