/**
 * Angles
 * Copyright (C) 2014  Sebastian Schelter
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

package io.ssc.angles.pipeline.nlp

import java.io.{ByteArrayOutputStream, File}

import com.google.common.base.Charsets
import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.io.IOUtils
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.sequences.DocumentReaderAndWriter
import io.ssc.angles.Config

import scala.collection.mutable

case class NamedEntity(name: String, entityType: String)

//TODO this code is ugly as hell....
object NamedEntityRecognition {

  lazy val classifier: CRFClassifier[CoreLabel] =
      CRFClassifier.getClassifier(new File(Config.property("angles.dataDir"), "ner/dewac_175m_600.crf.ser.gz"))
  classifier.loadTagIndex()

  def namedEntities(text: String) = {

    val readerAndWriter: DocumentReaderAndWriter[CoreLabel] = classifier.plainTextReaderAndWriter()
    val documents = classifier.makeObjectBankFromString(text, readerAndWriter)

    val out = new ByteArrayOutputStream()
    val writer = IOUtils.encodedOutputStreamPrintWriter(out, Charsets.UTF_8.displayName, true)

    classifier.classifyAndWriteAnswers(documents, writer, readerAndWriter)

    val output = out.toString(Charsets.UTF_8.displayName)

    val rawEntities = parseOutput(output)

    mergeEntities(rawEntities)
  }

  private[this] def mergeEntities(rawEntities: List[(String, String, Int)]) = {

    val entities = mutable.Map[NamedEntity, Int]()

    var prevName = new StringBuilder()
    var prevType = ""
    var prevPos = -1

    for (entity <- rawEntities) {
      val (currentName, currentType, currentPos) = entity

      if (currentType != prevType || currentPos - prevPos > 1) {
        if (prevType != "") {
          val entity = NamedEntity(prevName.toString, prevType)
          val count = entities.getOrElse(entity, 0)
          entities.put(entity, count + 1)
        }
        prevName = new StringBuilder(currentName)
        prevType = currentType
        prevPos = currentPos
      } else {
        prevName.append(" ").append(currentName)
        prevPos = currentPos
      }
    }
    if (prevType != "") {
      val entity = NamedEntity(prevName.toString, prevType)
      val count = entities.getOrElse(entity, 0)
      entities.put(entity, count + 1)
    }

    entities.toMap
  }

  private[this] def parseOutput(output: String): List[(String, String, Int)] = {
    val tagged = output.split(" ")

    val rawEntities = mutable.ListBuffer[(String, String, Int)]()

    var pos = 0
    while (pos < tagged.length) {
      val result = tagged(pos).trim
      if (result != "") {
        val splits = result.split("/")

        val word = splits(0)
        val entityType = splits(1)

        if (entityType != "O") {
          rawEntities.append((word, entityType, pos))
        }
      }
      pos += 1
    }
    rawEntities.toList
  }


}
