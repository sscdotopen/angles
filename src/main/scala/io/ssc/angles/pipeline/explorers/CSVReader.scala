package io.ssc.angles.pipeline.explorers

import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.io.Source

/**
 * Created by xolor on 19.02.15.
 */
object CSVReader {
  
  val logger = LoggerFactory.getLogger(CSVReader.getClass)

  def readExplorerPairsFromCSV(filename: String): List[ExplorerUriPair] = {
    val fileBuffer = Source.fromFile(filename)

    val resultList = mutable.MutableList.empty[ExplorerUriPair]

    for (line <- fileBuffer.getLines()) {
      if (!StringUtils.isEmpty(line)) {
        val separated = StringUtils.split(line, ",", 2)
        if (separated.size != 2) {
          logger.error("CSV row must consist of exactly two values: {}", line)
        } else {
          val explorer = StringUtils.trim(separated(0))
          var uri = StringUtils.trim(separated(1))
          uri = StringUtils.replace(uri, " ", "%20")
          uri = StringUtils.replace(uri, "|", "%7C")
          try {
            resultList += new ExplorerUriPair(explorer, uri)
          } catch {
            case e: IllegalArgumentException => logger.warn(e.getMessage)
          }
        }
      }
    }

    resultList.toList
  }

}