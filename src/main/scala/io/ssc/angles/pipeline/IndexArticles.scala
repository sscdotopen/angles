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

package io.ssc.angles.pipeline

import java.io.File

import io.ssc.angles.Config
import io.ssc.angles.pipeline.data.Storage
import io.ssc.angles.pipeline.filters.{ArticleFilter, GermanFilter}
import io.ssc.angles.pipeline.nlp.{German, NLPUtils}
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringEscapeUtils
import org.apache.lucene.analysis.de.GermanAnalyzer
import org.apache.lucene.document.{Document, Field, FieldType}
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.Version
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import twitter4j.Status

class IndexArticles extends Step {

  val log = LoggerFactory.getLogger(classOf[IndexArticles])

  def execute(since: DateTime): Unit = {

    log.info("Indexing articles ...")

    val now = new DateTime()

    val TYPE_STORED_WITH_TERMVECTORS: FieldType = new FieldType()

    TYPE_STORED_WITH_TERMVECTORS.setIndexed(true)
    TYPE_STORED_WITH_TERMVECTORS.setTokenized(true)
    TYPE_STORED_WITH_TERMVECTORS.setStored(true)
    TYPE_STORED_WITH_TERMVECTORS.setStoreTermVectors(true)
    TYPE_STORED_WITH_TERMVECTORS.setStoreTermVectorPositions(true)
    TYPE_STORED_WITH_TERMVECTORS.freeze()

    val websites = Storage.crawledWebsites(since)

    val websitesWithMetadata = websites map { website =>
      website -> Storage.metadataFor(website.id)
    }

    val selection = websitesWithMetadata
      .filter { case (website, metadata) => GermanFilter.passes(website, metadata)}
      .filter { case (website, metadata) => ArticleFilter.passes(website, metadata)}

    val metasToConsider = Array("title", "meta-description", "meta-keywords", "meta-og:title", "meta-og:description")

    val withEntities = selection map { case (website, metadata) =>
      val entities = Storage.namedEntities(website.id)
      (website, metadata, entities)
    }

    val pathToIndex = new File(Config.property("angles.dataDir"), "articleIndex")
    FileUtils.deleteDirectory(pathToIndex)

    val analyzer = new GermanAnalyzer(Version.LUCENE_42, German.stopSet())
    val directory = FSDirectory.open(pathToIndex)
    val config = new IndexWriterConfig(Version.LUCENE_42, analyzer)
    val indexWriter = new IndexWriter(directory, config)

    withEntities foreach { case (website, metadata, entities) =>

      val titleString = if (metadata.contains("meta-og:title")) {
        metadata("meta-og:title").mkString(" ")
      } else if (metadata.contains("title")) {
        metadata("title").mkString(" ")
      } else {
        ""
      }

      val descriptionString = if (metadata.contains("meta-og:description")) {
        metadata("meta-og:description").mkString(" ")
      } else if (metadata.contains("meta-description")) {
        metadata("meta-description").mkString(" ")
      } else {
        ""
      }

      val keywordsString = if (metadata.contains("meta-keywords")) {
        metadata("meta-keywords").mkString(" ")
      } else {
        ""
      }

      val nerString = (entities map { entity => entity.name.replaceAll(" ", "_") + "_" + entity.entityType}).mkString(" ")

      val terms = NLPUtils.toGermanStemmedTerms(nerString)

      val explorer = Storage.explorerOfWebsite(website.id).get

      val status = Storage.statusOfWebsite(website.id).get

      val retweets = ExtractStatusData.retweetsPerHoursInDayOne(now, status)

      val document = new Document()

      document.add(new Field("uri", website.realUri, TYPE_STORED_WITH_TERMVECTORS))
      document.add(new Field("screenname", explorer.screenname, TYPE_STORED_WITH_TERMVECTORS))
      document.add(new Field("retweetsPerHourInDayOne", retweets.toString, TYPE_STORED_WITH_TERMVECTORS))
      document.add(new Field("hashtags", hashtags(status), TYPE_STORED_WITH_TERMVECTORS))
      document.add(new Field("title", clean(titleString.toString), TYPE_STORED_WITH_TERMVECTORS))
      document.add(new Field("description", clean(descriptionString.toString), TYPE_STORED_WITH_TERMVECTORS))
      document.add(new Field("keywords", keywordsString.toString, TYPE_STORED_WITH_TERMVECTORS))
      document.add(new Field("entities", nerString.toString, TYPE_STORED_WITH_TERMVECTORS))
      document.add(new Field("body", clean(NLPUtils.extractArticle(website.html)), TYPE_STORED_WITH_TERMVECTORS))

      indexWriter.addDocument(document)

      log.info(website.realUri)
      log.info(terms.mkString(" "))
    }

    indexWriter.close()
  }

  //TODO this should be in metadata extractor
  def clean(str: String) = {
    StringEscapeUtils.unescapeHtml(str.trim)
  }

  def hashtags(status: Status) = {
    (for (hashtagEntity <- status.getHashtagEntities) yield {
      hashtagEntity.getText.toLowerCase
    }).mkString(" ")
  }


}
