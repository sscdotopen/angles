package io.ssc.angles.pipeline.explorers

/**
 * Created by xolor on 18.02.15.
 */
object CompareClusterSets extends App {

  val clusterReadWriter = new ClusterReadWriter

  val sourceFile = "communities_manual.tsv"
  val targetFile = "communities_jaccard.tsv"

  val sourceClusterSet = clusterReadWriter.readClusterFile(sourceFile)
  val targetClusterSet = clusterReadWriter.readClusterFile(targetFile)

  sourceClusterSet.getExplorers.foreach { case (explorer: String) =>
    val sourceClusters = sourceClusterSet.getClusterIdsForExplorer(explorer)
    val targetClusters = targetClusterSet.getClusterIdsForExplorer(explorer)

    printf("%s FROM %s TO %s\n", explorer, sourceClusters, targetClusters)
  }


}
