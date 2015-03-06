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
    }

    resultList.toList
  }
  
  def readTuplesFromCSV(filename : String) : Set[(String, String, String)] = {
    val fileBuffer = Source.fromFile(filename)
    var resultList : mutable.Set[(String, String, String)] = mutable.Set.empty

    for (line <- fileBuffer.getLines()) {
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
    }
    resultList.toSet
  } 

}
