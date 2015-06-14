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
import java.nio.file.Files
import java.nio.file.Paths
import org.slf4j.LoggerFactory

object GenerateAntiPairs extends App {
  val logger = LoggerFactory.getLogger(GenerateAntiPairs.getClass)
  
  val clustersFile = args(0)
  val antiPairsFile = args(1)
  
  logger.info("Loading cluster file")
  val clusters = ClusterReadWriter.readClusterFile(clustersFile)
  logger.info("Got {} clusters from CSV", clusters.getNumClusters)
  
  val path = Paths.get(antiPairsFile)
  val writer = new PrintWriter(Files.newBufferedWriter(path))
  
  var n = 0
  for (i <- 1 until clusters.getNumClusters; j <- (i + 1) until clusters.getNumClusters) {
    val cluster1 = clusters.getCluster(i)
    val cluster2 = clusters.getCluster(j)
    
    (cluster1 cross cluster2).foreach { p =>
      writer.print(p._1)
      writer.print(";")
      writer.println(p._2)
      n += 1
    }
  }
  writer.close()
  logger.info("Generated {} anti-pairs", n)
  
  implicit class Crossable[X](xs: Traversable[X]) {
    def cross[Y](ys: Traversable[Y]) = (xs).flatMap { case x => (ys).map { case y => (x, y) } }
  }
}
