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

package io.ssc.angles.pipeline.filters

import java.io.FileInputStream
import java.net.URI

import com.google.common.io.Closeables
import io.ssc.angles.Config
import io.ssc.angles.pipeline.ml.WebsiteVectorizer
import io.ssc.angles.pipeline.nlp.{NLPUtils, LanguageDetection}
import io.ssc.data.CrawledWebsite
import org.apache.mahout.classifier.sgd.{ModelSerializer, OnlineLogisticRegression}

trait Filter {
  def passes(website: CrawledWebsite, metadata: Map[String, Set[String]]): Boolean
}

object GermanFilter extends Filter {
  override def passes(website: CrawledWebsite, metadata: Map[String, Set[String]]): Boolean = {
    metadata.getOrElse("meta-og:locale", Set()).contains("de_DE") ||
    metadata.getOrElse("meta-content-language", Set()).contains("de") ||
    LanguageDetection.isGerman(NLPUtils.extractArticle(website.html))
  }
}

object ArticleFilter extends Filter {

  val in = new FileInputStream(Config.property("angles.dataDir") + "/article-detector.model")
  val classifier = ModelSerializer.readBinary(in, classOf[OnlineLogisticRegression])
  Closeables.close(in, false)

  override def passes(website: CrawledWebsite, metadata: Map[String, Set[String]]): Boolean = {

    // hand crafted rules, ultimately, the classifier should have learned those
    if (website.realUri.startsWith("http://instagram.com")  ||
        website.realUri.startsWith("https://youtube.com")  ||
        website.realUri.startsWith("http://twitter.com")) {
      return false
    }

    val uri = new URI(website.realUri)
    if ("/".equals(uri.getPath) || "".equals(uri.getPath)) {
      return false
    }

    val vector = WebsiteVectorizer.vectorize(website)
    val prediction = classifier.classifyScalar(vector)
    prediction > 0.6
  }
}

