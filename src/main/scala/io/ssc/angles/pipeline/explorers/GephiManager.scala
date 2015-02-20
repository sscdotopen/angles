package io.ssc.angles.pipeline.explorers

import java.io.File

import org.gephi.graph.api.{Graph, GraphController, GraphFactory, Node}
import org.gephi.io.exporter.api.ExportController
import org.gephi.io.exporter.preview.PNGExporter
import org.gephi.layout.plugin.openord.OpenOrdLayoutBuilder
import org.gephi.project.api.{ProjectController, Workspace}
import org.openide.util.Lookup
import org.slf4j.LoggerFactory

import scala.collection.mutable


/**
 * Class for handling gephi graphs. Contains methods for importing graphs, exporting images, clustering and statistics.
 */
class GephiManager {

  val logger = LoggerFactory.getLogger(classOf[GephiManager])

  val projectController: ProjectController = Lookup.getDefault.lookup(classOf[ProjectController])

  projectController.newProject()

  val workspace: Workspace = projectController.getCurrentWorkspace

  def loadGraphMap(mapGraph: Map[(String, String), Double], isDirected: Boolean): Unit = {
    val graphModel = Lookup.getDefault.lookup(classOf[GraphController]).getModel(workspace)
    val nodeMap = mutable.HashMap.empty[String, Node]
    var graph : Graph = null

    if (isDirected) {
      graph = graphModel.getDirectedGraph
    } else {
      graph = graphModel.getUndirectedGraph
    }

    logger.info("Importing graph to gephi...")

    mapGraph.foreach { case ((leftString, rightString), weight) =>
      // Get or create nodes for left + right
      val leftNode = nodeMap.getOrElseUpdate(leftString, addNewNodeToGraph(graph, graphModel.factory(), leftString))
      val rightNode = nodeMap.getOrElseUpdate(rightString, addNewNodeToGraph(graph, graphModel.factory(), rightString))
      // Add edge to graph
      val edge = graphModel.factory().newEdge(leftNode, rightNode, weight.asInstanceOf[Float], isDirected)
      graph.addEdge(edge)
    }

    logger.info("Imported {} nodes and {} edges", graph.getNodeCount, graph.getEdgeCount)
  }

  /**
   * Create a new node inside the the given graph and return it.
   * *
   * @param graph
   * @param factory
   * @param nodeName
   * @return
   */
  private def addNewNodeToGraph(graph: Graph, factory: GraphFactory, nodeName: String): Node = {
    val newNode = factory.newNode(nodeName)
    graph.addNode(newNode)
    newNode
  }

  def exportGraphToPNGImage(filename: String, height: Int, width: Int) = {
    val exportController = Lookup.getDefault.lookup(classOf[ExportController])
    val pngExporter: PNGExporter = exportController.getExporter("png").asInstanceOf[PNGExporter]
    pngExporter.setWorkspace(workspace)
    pngExporter.setHeight(height)
    pngExporter.setWidth(width)

    logger.info("Begin PNG export from gephi data to file {}...", filename)
    exportController.exportFile(new File(filename), pngExporter)
    logger.info("Finished graph export")
  }

  def runOpenOrdLayout() = {
    val graphModel = Lookup.getDefault.lookup(classOf[GraphController]).getModel(workspace)
    val openOrdLayout = new OpenOrdLayoutBuilder().buildLayout
    openOrdLayout.setGraphModel(graphModel)

    logger.info("Preparing OpenOrd layout...")
    openOrdLayout.resetPropertiesValues()
    openOrdLayout.initAlgo()

    logger.info("Running OpenOrd layout...")
    while (openOrdLayout.canAlgo) {
      openOrdLayout.goAlgo()
    }
    logger.info("Finished OpenOrd layout!")
  }

}
