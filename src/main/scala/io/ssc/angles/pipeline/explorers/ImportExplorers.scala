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
