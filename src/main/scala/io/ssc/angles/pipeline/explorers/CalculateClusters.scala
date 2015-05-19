package io.ssc.angles.pipeline.explorers

import java.nio.charset.Charset
import java.nio.file.{Files, Paths}

import com.google.common.collect.SetMultimap
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import scalikejdbc.ConnectionPool.MutableMap

import scala.collection.mutable
import scala.io.Source

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
    explorers.foreach{s =>
        if (!clusterMap.containsValue(s)) {
          clusterMap.put(i, s)
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

    val graphData = readGraphCSV(inFile)

    val gephiManager = new GephiManager
    gephiManager.loadGraphMap(graphData, false)
    gephiManager.runOpenOrdLayout()
    val clusterMap: SetMultimap[Int, String] = gephiManager.runChineseWhispersClusterer()
    addMissingExplorers(clusterMap)

    val clusterReadWriter = new ClusterReadWriter
    clusterReadWriter.writeClusterFile(communityFile, clusterMap)
    gephiManager.exportGraphToPNGImage(graphFile, 8192, 8192)
  }

  def readGraphCSV(filename: String): Map[(String, String), Double] = {
    logger = LoggerFactory.getLogger(CalculateClusters.getClass)

    logger.info("Reading csv from {} ...", filename)
    // write csv output
    val path = Paths.get(filename)
    val reader = Files.newBufferedReader(path, Charset.forName("UTF-8"))

    var line = reader.readLine()

    if (!StringUtils.equals(line, "Source,Target,Weight,Type")) {
      logger.error("Invalid first line:  {}", line)
      throw new IllegalArgumentException("Invalid first line")
    }

    line = reader.readLine()

    val resultMap: MutableMap[(String, String), Double] = mutable.HashMap.empty

    while (line != null) {
      line = StringUtils.remove(line, "\"")
      var lineData = StringUtils.split(line, ",")
      if (lineData.length != 4) {
        throw new IllegalArgumentException("Invalid line")
      }


      val left = lineData(0)
      val right = lineData(1)
      val weight = NumberUtils.createDouble(lineData(2))

      resultMap += (((left, right), weight))

      line = reader.readLine()
    }

    logger.info("Finished csv import!")
    resultMap.toMap
  }

}
