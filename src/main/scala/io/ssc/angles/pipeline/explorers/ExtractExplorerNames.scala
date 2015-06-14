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
