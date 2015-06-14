/**
 * Angles
 * Copyright (C) 2015 Jakob Hendeß, Niklas Wolber
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
object ClusterReadWriter {

  val logger = LoggerFactory.getLogger(ClusterReadWriter.getClass)

  def readClusterFile(filename: String): ClusterSet[String] = {
    val path: Path = Paths.get(filename)
    val bufferedReader = Files.newBufferedReader(path, Charset.forName("UTF-8"))
    val clusterSet = new ClusterSet[String]
    var line: String = bufferedReader.readLine()

    clusterSet.newCluster()
    while (line != null) {
      var trimmedLine: String = StringUtils.trim(line).toLowerCase
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
