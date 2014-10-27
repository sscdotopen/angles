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

package io.ssc.angles.pipeline.ml

import java.net.URI
import java.util.regex.Pattern

import io.ssc.angles.pipeline.data.Storage
import io.ssc.angles.pipeline.http.Utils
import io.ssc.angles.pipeline.nlp.NLPUtils
import io.ssc.data.CrawledWebsite
import org.apache.lucene.analysis.de.GermanAnalyzer
import org.apache.lucene.util.Version
import org.apache.mahout.math.RandomAccessSparseVector
import org.apache.mahout.vectorizer.encoders.{ConstantValueEncoder, LuceneTextValueEncoder, StaticWordValueEncoder}
import org.htmlcleaner.TagNode

import scala.collection.mutable


object WebsiteVectorizer {

  val numFeatures = 100000
  val interceptEncoder = new ConstantValueEncoder("intercept")
  val featureEncoder = new StaticWordValueEncoder("feature")
  val luceneEncoder = new LuceneTextValueEncoder("lucene")
  luceneEncoder.setAnalyzer(new GermanAnalyzer(Version.LUCENE_42))

  val wordSplitter = Pattern.compile("\\W+")

  def vectorize(website: CrawledWebsite) = {

    val metadata = Storage.metadataFor(website.id)
    val entities = Storage.namedEntities(website.id)

    val vector = new RandomAccessSparseVector(numFeatures)

    interceptEncoder.addToVector("1", vector)

    val siteUri = new URI(website.realUri)
    featureEncoder.addToVector(siteUri.getHost, vector)

    val pathTokens = siteUri.getPath.split("/")
    featureEncoder.addToVector("numPathTokens", pathTokens.size, vector)

    pathTokens foreach { token =>
      featureEncoder.addToVector("pathToken-" + token, vector)
    }

    if (siteUri.getQuery != null) {
      val queryTokens = siteUri.getQuery.split("&")
      featureEncoder.addToVector("numQueryTokens", queryTokens.size, vector)

      queryTokens foreach { token =>
        featureEncoder.addToVector("queryToken-" + token, vector)
      }
    } else {
      featureEncoder.addToVector("numQueryTokens", 0, vector)
    }

    val allTags = Utils.clean(website.html).getAllElements(true)

    val tagCounts = mutable.Map[String, Int]()

    allTags foreach { tag: TagNode =>
      val count = tagCounts.getOrElse(tag.getName, 0)
      tagCounts.put(tag.getName, count + 1)
    }

    for ((tag, count) <- tagCounts) {
      featureEncoder.addToVector("html-tag-" + tag, count, vector)
    }

    for ((key, values) <- metadata) {
      values foreach { value =>
        featureEncoder.addToVector(key + value, vector)
      }
    }

    entities foreach { entity =>
      featureEncoder.addToVector(entity.entityType + entity.name, entity.count, vector)
    }

    val article = NLPUtils.extractArticle(website.html)

    featureEncoder.addToVector("numWordsInText", wordSplitter.split(article).size, vector)

    luceneEncoder.addText(article)
    luceneEncoder.flush(1.0, vector)

    vector
  }

}
