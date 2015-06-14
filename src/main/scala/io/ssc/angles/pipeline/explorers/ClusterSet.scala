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

import com.google.common.collect.{HashMultimap, SetMultimap}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Helper class for managing a set of clusters. Data is backed by two SetMultimaps.
 */
class ClusterSet[T] {

  private val explorerSet: mutable.Set[T] = mutable.HashSet.empty[T]

  private val clusterToValueMap: SetMultimap[Int, T] = HashMultimap.create()

  private val valueToClusterMap: SetMultimap[T, Int] = HashMultimap.create()

  private var currentClusterId = -1

  def addExplorerToCurrentCluster(explorer: T): Unit = {
    if (currentClusterId == -1) {
      throw new Exception("newCluster has to be called first")
    }

    clusterToValueMap.put(currentClusterId, explorer)
    valueToClusterMap.put(explorer, currentClusterId)
    explorerSet += explorer
  }

  def newCluster() = {
    currentClusterId += 1
  }

  def getClusterIdsForExplorer(explorer: T): Set[Int] = {
    valueToClusterMap.get(explorer).asScala.toSet
  }

  def getExplorers: Set[T] = {
    explorerSet.toSet
  }

  def getNumClusters = {
    if (currentClusterId == -1) {
      throw new Exception("newCluster has to be called first")
    }

    clusterToValueMap.keySet().size()
  }

  def getCluster(cluster: Int): Iterable[T] = {
    clusterToValueMap.asMap().get(cluster).asScala
  }

  def getClusters(): Iterable[Iterable[T]] = {
    clusterToValueMap.asMap().entrySet().asScala.map(e => e.getValue.asScala).toIterable
  }

}
