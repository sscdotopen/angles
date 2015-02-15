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

import io.ssc.angles.pipeline.data.Storage
import io.ssc.angles.pipeline.http.Crawler
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

class CrawlUris extends Step {

  val log = LoggerFactory.getLogger(classOf[CrawlUris])

  override def execute(since: DateTime): Unit = {

    log.info("Extracting urls from latest tweets")
    val latestUrls = Storage.expandedUrlsInTweetsSince(since)

    log.info("Crawling urls from latest tweets")

    latestUrls.par foreach { case (statusId, uri) =>
      if (!Storage.alreadyCrawled(statusId, uri)) {

        val crawler = new Crawler
        val result = try {
          log.info("Fetching {}", uri)
          crawler.fetch(uri)
        } catch {
          case t: Throwable =>
            log.error("Fetching failed", t)
            None
        }

        result match {
          case Some((realUri, charset, html)) =>
            Storage.saveCrawledWebsite(uri, realUri, statusId, new DateTime(), charset.displayName, html)
            log.info("Fetched {}", realUri)
          case _ =>
            log.warn("Nothing found")
        }
      }
        Storage.markTweetCrawled(statusId)
    }
  }


}
