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

object CompareClusterSets extends App {

  val sourceFile = "communities_manual.tsv"
  val targetFile = "communities_cosine.tsv"

  val sourceClusterSet = ClusterReadWriter.readClusterFile(sourceFile)
  val targetClusterSet = ClusterReadWriter.readClusterFile(targetFile)

  sourceClusterSet.getExplorers.foreach { case (explorer: String) =>
    val sourceClusters = sourceClusterSet.getClusterIdsForExplorer(explorer)
    val targetClusters = targetClusterSet.getClusterIdsForExplorer(explorer)

    printf("%s FROM %s TO %s\n", explorer, sourceClusters, targetClusters)
  }


}
