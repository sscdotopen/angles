package io.ssc.angles.pipeline.explorers

import java.sql.SQLException

import io.ssc.angles.pipeline.data.Storage
import org.slf4j.LoggerFactory

/**
 * Application for importing explorers to the angles database. Note that this will be
 */
object ImportExplorers extends App {

  override def main(args: Array[String]) {

    val logger = LoggerFactory.getLogger(getClass)

    val allExplorerIds = Storage.allExplorers().map(e => e.id).toSet

    if (args.length != 1) {
      logger.error("Filename missing")
    } else {
      val filename = args(0)
      val tuples = CSVReader.readTuplesFromCSV(filename).map(t => (t._1, t._2)).foreach { x =>
        if (!allExplorerIds.contains(x._1.toLong)) {
          try {
            Storage.saveExplorer(x._1, x._2)
            logger.info("Added explorer {}", x)
          } catch {
            case e: SQLException => logger.warn("Adding explorer {} failed: {}", x.toString().asInstanceOf[Any], e.getMessage.asInstanceOf[Any])
          }
        } else {
          logger.info("Explorer {} already in database", x)
        }
      }
      logger.info("Done.")
    }
  }

}
