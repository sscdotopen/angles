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

import java.io.StringReader

import io.ssc.angles.pipeline.data.Storage
import io.ssc.angles.pipeline.http.MetadataExtractor
import org.apache.commons.lang.StringEscapeUtils
import org.htmlcleaner.HtmlCleaner
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import scala.collection.mutable

class ExtractMetadata extends Step {

  val log = LoggerFactory.getLogger(classOf[ExtractMetadata])

  val cleaner = new HtmlCleaner

  val cleanerProperties = cleaner.getProperties
  cleanerProperties.setTranslateSpecialEntities(true)
  cleanerProperties.setAdvancedXmlEscape(true)
  cleanerProperties.setTransResCharsToNCR(true)
  cleanerProperties.setTransSpecialEntitiesToNCR(true)
  cleanerProperties.setAllowHtmlInsideAttributes(false)
  cleanerProperties.setKeepWhitespaceAndCommentsInHead(false)
  cleanerProperties.setOmitCdataOutsideScriptAndStyle(true)
  cleanerProperties.setOmitComments(true)
  cleanerProperties.setIgnoreQuestAndExclam(true)
  cleanerProperties.setRecognizeUnicodeChars(true)
  cleanerProperties.setOmitComments(true)

  override def execute(since: DateTime): Unit = {
    log.info("Extracting metadata ...")

    val websites = Storage.crawledWebsites(since)

    //TODO respect noindex nosnippet none noarchive noimageindex unavailable_after: [RFC-850 date/time] in meta robots

    websites
      .filter { website =>
        log.debug("Looking at metadata for {}", website.realUri)
        Storage.metadataFor(website.id).isEmpty
      }
      .foreach { website =>

        log.info("Extracting metadata from {}", website.realUri)

        val attributes = mutable.Map[String, List[String]]()

        //TODO Charset?
        val reader = new StringReader(website.html)
        val root = cleaner.clean(reader)

        attributes += "uri" -> List(website.realUri)

        val extractor = new MetadataExtractor(root)

        attributes += "title" -> extractor.title
        attributes += "meta-author" -> extractor.metaName("author")
        attributes += "meta-date" -> extractor.metaName("date")
        attributes += "meta-content-language" -> extractor.metaName("Content-Language")
        attributes += "meta-fulltitle" -> extractor.metaName("fulltitle")
        attributes += "meta-description" -> extractor.metaName("description")
        attributes += "meta-publisher" -> extractor.metaName("publisher")
        attributes += "meta-keywords" -> extractor.metaKeywords

        attributes += "meta-og:url" -> extractor.metaProperty("og:url")
        attributes += "meta-og:type" -> extractor.metaProperty("og:type")
        attributes += "meta-og:title" -> extractor.metaProperty("og:title")
        attributes += "meta-og:description" -> extractor.metaProperty("og:description")
        attributes += "meta-og:sitename" -> extractor.metaProperty("og:site_name")
        attributes += "meta-og:image" -> extractor.metaProperty("og:image")
        attributes += "meta-og:image:width" -> extractor.metaProperty("og:image:width")
        attributes += "meta-og:image:height" -> extractor.metaProperty("og:image:height")
        attributes += "meta-og:locale" -> extractor.metaProperty("og:locale")

        attributes += "meta-website:published-time" -> extractor.metaProperty("website:published_time")
        attributes += "meta-website:modified-time" -> extractor.metaProperty("website:modified_time")
        attributes += "meta-website:author" -> extractor.metaProperty("website:author")
        attributes += "meta-website:publisher" -> extractor.metaProperty("website:publisher")
        attributes += "meta-website:section" -> extractor.metaProperty("website:section")
        attributes += "meta-website:tag" -> extractor.metaProperty("website:tag")

        attributes += "meta-dc:title" -> extractor.metaName("DC.title")
        attributes += "meta-dc:description" -> extractor.metaName("DC.description")
        attributes += "meta-dc:creator" -> extractor.metaName("DC.creator")
        attributes += "meta-dc:date" -> extractor.metaName("DC.Date")
        attributes += "meta-dc:subject" -> extractor.metaName("DC.Subject")

        attributes += "meta-twitter:title" -> extractor.metaName("twitter:title")
        attributes += "meta-twitter:description" -> extractor.metaName("twitter:description")
        attributes += "meta-twitter:url" -> extractor.metaName("twitter:url")
        attributes += "meta-twitter:image" -> extractor.metaName("twitter:image")
        attributes += "meta-twitter:image:width" -> extractor.metaName("twitter:image:width")
        attributes += "meta-twitter:image:height" -> extractor.metaName("twitter:image:height")
        attributes += "meta-twitter:creator" -> extractor.metaName("twitter:creator")
        attributes += "meta-twitter:creator:id" -> extractor.metaName("twitter:creator:id")

        attributes += "meta-vr:published" -> extractor.metaProperty("vr:published")
        attributes += "meta-vr:author" -> extractor.metaProperty("vr:author")
        attributes += "meta-vr:type" -> extractor.metaProperty("vr:type")
        attributes += "meta-vr:category" -> extractor.metaProperty("vr:category")

        attributes += "meta-itemprob:name" -> extractor.metaItemProb("name")
        attributes += "meta-itemprob:description" -> extractor.metaItemProb("description")

       //TODO analyze <link rel="profile" href="http://gmpg.org/xfn/11" />?

      /*
       <meta charset="UTF-8">
    <html lang="en">
        <link rel="canonical" href="http://www.sueddeutsche.de/medien/fuehrungsschwaeche-in-magazinverlagen-schnappatmung-1.2108507">
    <link rel='shortlink' href='http://blog.wawzyniak.de/?p=8420' />
    <base href="https://www.freitag.de/autoren/felix-werdermann/neue-gruene-asylpolitik" />
        <link rel="alternate" title="sueddeutsche.de Medien RSS Feed" href="http://rss.sueddeutsche.de/rss/Medien" type="application/rss+xml">
        <link rel="alternate" title="sueddeutsche.de RSS Feed" href="http://rssfeed.sueddeutsche.de/c/795/f/449002/index.rss" type="application/rss+xml">
        <link rel="alternate" title="S&uuml;ddeutsche.de Der Spiegel RSS Feed" href="http://rss.sueddeutsche.de/rss/Thema/Der+Spiegel" type="application/rss+xml">
    <link rel="image_src" href="http://twitpic.com/show/thumb/eaztf0.jpg" />
    */

        val existingAttributes = attributes.filter { case (key, values) => !values.isEmpty }
                                           .map { case (key, values) => key -> values.map { clean(_) } }

        val sortedKeys = existingAttributes.keys.toSeq.sorted
        for (key <- sortedKeys) {
          log.info("{}", Array(key.toUpperCase, existingAttributes(key).mkString(" - ")))
          log.info("Saving metadata for website {}", website.id)
          for (attribute <- existingAttributes(key)) {
            Storage.saveMetadata(website.id, key, attribute)
          }
        }
        log.info("")
    }
  }

  def clean(str: String) = {
    StringEscapeUtils.unescapeHtml(str.trim)
  }

}
