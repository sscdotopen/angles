package io.ssc.angles.pipeline.explorers

import java.io.PrintWriter
import java.nio.file.{Files, Paths}

import org.slf4j.LoggerFactory

/**
 * Use this class to transform pairs.csv into explorers.csv
 */
object ExtractExplorerNames extends App {

  override def main(args: Array[String]) {
    val logger = LoggerFactory.getLogger(ExtractExplorerNames.getClass)
    val outFile: String = "explorers_all.tsv"

    val pairFile = "pairs.csv"

    logger.info("Reading CSV...")
    val explorerPairs = CSVReader.readExplorerPairsFromCSV(pairFile)

    logger.info("Writing output ...")
    val writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outFile)))
    explorerPairs.map(p => p.explorerId).distinct.foreach { s => writer.write(s + "\n") }

    writer.flush()
    writer.close()

    logger.info("Done!")
  }

}
