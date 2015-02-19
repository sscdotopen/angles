package io.ssc.angles.pipeline.explorers

import com.google.common.collect.{HashMultimap, SetMultimap}

import scala.collection.mutable

/**
 * Helper class for managing a set of clusters. Data is backed by two SetMultimaps.
 */
class ClusterSet {

  private val explorerSet: mutable.Set[String] = mutable.HashSet.empty[String]

  private val clusterToValueMap: SetMultimap[Int, String] = HashMultimap.create()

  private val valueToClusterMap: SetMultimap[String, Int] = HashMultimap.create()

  private var currentClusterId = 0

  def addExplorerToCurrentCluster(explorer: String): Unit = {
    clusterToValueMap.put(currentClusterId, explorer)
    valueToClusterMap.put(explorer, currentClusterId)
    explorerSet += explorer
  }

  def newCluster() = {
    currentClusterId += 1
  }

  def getClusterIdsForExplorer(explorer: String): java.util.Set[Int] = {
    valueToClusterMap.get(explorer)
  }

  def getExplorers: Set[String] = {
    explorerSet.toSet
  }

}
