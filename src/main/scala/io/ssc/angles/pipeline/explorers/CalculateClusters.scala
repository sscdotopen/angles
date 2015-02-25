package io.ssc.angles.pipeline.explorers

import java.nio.charset.Charset
import java.nio.file.{Files, Paths}

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import scalikejdbc.ConnectionPool.MutableMap

import scala.collection.mutable

/**
 * Executable for calculating clusters.
 * Parameters:
 *   - input csv file describing the graph
 *   - suffix of the outfiles -> i.e. communities_SUFFIX.tsv graph_SUFFIX.png
 */
object CalculateClusters extends App {

  var logger = LoggerFactory.getLogger(CalculateClusters.getClass)

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
    val jaccardClusterMap = gephiManager.runChineseWhispersClusterer()
    val clusterReadWriter = new ClusterReadWriter
    clusterReadWriter.writeClusterFile(communityFile, jaccardClusterMap)
    gephiManager.exportGraphToPNGImage(graphFile, 16384, 16384)


  }

  def readGraphCSV(filename: String): Map[(String, String), Double] = {
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
      val lineData = StringUtils.split(line, ",")
      if (lineData.length != 4) {
        throw new IllegalArgumentException("Invalid first line")
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
