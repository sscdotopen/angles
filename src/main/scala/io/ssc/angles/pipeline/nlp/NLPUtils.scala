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

import java.io.StringReader

import de.l3s.boilerpipe.extractors.ArticleExtractor
//import io.ssc.angles.pipeline.nlp.Loglikelihood
import org.apache.lucene.analysis.de.GermanAnalyzer
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.util.Version

import scala.collection.mutable

object NLPUtils extends App {

  def extractArticle(html: String) = {
    ArticleExtractor.getInstance().getText(html)
  }

  def incrementCount(counts: mutable.Map[String, Int], key: String) = {
    val newCount = counts.getOrElse(key, 0) + 1
    counts(key) = newCount
  }

  def toGermanStemmedTerms(text: String) = {

    val analyzer = new GermanAnalyzer(Version.LUCENE_42, German.stopSet())

    val reader = new StringReader(text)

    val stream = analyzer.tokenStream(null, reader)
    stream.reset()

    val terms = mutable.ListBuffer[String]()

    while (stream.incrementToken()) {
      val term = stream.getAttribute(classOf[CharTermAttribute]).toString
      terms.append(term)
    }

    terms.toList
  }

}