package io.ssc.angles.pipeline.explorers

import java.io.PrintWriter
import java.nio.charset.Charset
import java.nio.file.{Files, Path, Paths}
import java.util

import com.google.common.collect.SetMultimap
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._


/**
 * Reader and writer for tsv-cluster files. Each line represents a new entry (i.e. node) one or more newlines denote a new cluster.
 */
class ClusterReadWriter {

  val logger = LoggerFactory.getLogger(classOf[ClusterReadWriter])

  def readClusterFile(filename: String): ClusterSet = {
    val path: Path = Paths.get(filename)
    val bufferedReader = Files.newBufferedReader(path, Charset.forName("UTF-8"))
    val clusterSet = new ClusterSet
    var line: String = bufferedReader.readLine()

    while (line != null) {
      var trimmedLine: String = StringUtils.trim(line)
      if (trimmedLine.isEmpty)
        clusterSet.newCluster()
      else
        clusterSet.addExplorerToCurrentCluster(trimmedLine)
      
      line = bufferedReader.readLine()
    }
    clusterSet
  }

  def writeClusterFile(filename: String, clusterMap: SetMultimap[Int, String]) = {
    logger.info("Writing cluster file {} ...", filename)

    var path = Paths.get(filename)
    var writer = new PrintWriter(Files.newBufferedWriter(path))

    for (t: Tuple2[Int, java.util.Collection[String]] <- clusterMap.asMap()) {
      val clusterId: Int = t._1
      val explorerIds: util.Collection[String] = t._2
      explorerIds.foreach {
        case explorerId: String => writer.println(StringUtils.replace(explorerId, "\"", ""))
        case any => logger.warn("Skipping invalid value in cluster {}: {}", clusterId, any)
      }
      writer.println()
    }

    writer.flush()
    writer.close()

    logger.info("Finished cluster export!")
  }

}
