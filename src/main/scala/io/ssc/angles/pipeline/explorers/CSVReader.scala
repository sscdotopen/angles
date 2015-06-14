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

import java.nio.charset.Charset
import java.nio.file.{Files, Paths}

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import scalikejdbc.ConnectionPool._

import scala.collection.mutable

object CSVReader {

  val logger = LoggerFactory.getLogger(CSVReader.getClass)

  def readExplorerPairsFromCSV(filename: String): List[ExplorerUriPair] = {
    val path = Paths.get(filename)
    val reader = Files.newBufferedReader(path, Charset.forName("UTF-8"))
    val resultList = mutable.MutableList.empty[ExplorerUriPair]

    var line = reader.readLine()
    while (line != null) {
      if (!StringUtils.isEmpty(line)) {
        val separated = StringUtils.split(line, ";", 3)   // Somehow this doesn't work as expected?!
        if (separated.size != 3) {
          logger.warn("The following CSV row must consist of exactly three values: {}", line)
        } else {
          val explorer = StringUtils.trim(separated(1))
          var uri = HelperUtils.escapeURIString(separated(2))
          try {
            resultList += new ExplorerUriPair(explorer, uri)
          } catch {
            case e: IllegalArgumentException => //logger.warn(e.getMessage)
          }
        }
      }
      line = reader.readLine()
    }
    reader.close()
    resultList.toList
  }

  def readTuplesFromCSV(filename : String) : Set[(String, String, String)] = {
    val path = Paths.get(filename)
    val reader = Files.newBufferedReader(path, Charset.forName("UTF-8"))
    var resultList : mutable.Set[(String, String, String)] = mutable.Set.empty

    var line = reader.readLine()
    while (line != null) {
      if (!StringUtils.isEmpty(line)) {
        val separated = StringUtils.split(line, ";", 3) // Somehow this doesn't work as expected?!
        if (separated.size != 3) {
          logger.warn("The following CSV row must consist of exactly three values: {}", line)
        } else {
          val explorerId: String = StringUtils.trim(separated(0))
          val explorerName: String = StringUtils.trim(separated(1))
          val uri : String = StringUtils.trim(separated(2))
          try {
            resultList += ((explorerId, explorerName, uri))
          } catch {
            case e: IllegalArgumentException => //logger.warn(e.getMessage)
          }
        }
      }
      line = reader.readLine()
    }
    reader.close()
    resultList.toSet
  }

  def readGraphCSV(filename: String): Map[(String, String), Double] = {
    logger.info("Reading csv from {} ...", filename)
    // write csv output
    val path = Paths.get(filename)
    val reader = Files.newBufferedReader(path, Charset.forName("UTF-8"))

    var line = reader.readLine()

    if (!StringUtils.equals(line, "Source,Target,Weight,Type")) {
      logger.error("Invalid first line:  {}", line)
      throw new IllegalArgumentException("Invalid first line")
    }

    line = reader.readLine()

    val resultMap: MutableMap[(String, String), Double] = mutable.HashMap.empty

    while (line != null) {
      val lineData = StringUtils.split(line, ",")
      if (lineData.length != 4) {
        throw new IllegalArgumentException("Invalid first line")
      }

      val left = lineData(0)
      val right = lineData(1)
      val weight = NumberUtils.createDouble(lineData(2))

      resultMap += (((left, right), weight))

      line = reader.readLine()
    }

    logger.info("Finished csv import!")
    resultMap.toMap
  }
}
