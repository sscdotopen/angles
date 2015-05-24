package io.ssc.angles.pipeline.explorers

/**
 * Created by xolor on 18.02.15.
 */
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
