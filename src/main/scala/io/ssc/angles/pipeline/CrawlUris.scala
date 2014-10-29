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

import java.util.concurrent.atomic.AtomicInteger

import io.ssc.angles.pipeline.data.Storage
import io.ssc.angles.pipeline.http.Crawler
import org.joda.time.DateTime
import org.slf4j.LoggerFactory


class CrawlUris extends Step {

  val log = LoggerFactory.getLogger(classOf[CrawlUris])

  var crawledElements : AtomicInteger = new AtomicInteger()

  override def execute(since: DateTime): Unit = {
    crawledElements = new AtomicInteger(0)

    log.info("Extracting urls from latest tweets")
    val latestUrls = Storage.expandedUrlsInTweetsSince(since)
    val totalElements = latestUrls.size

    log.info("Crawling urls from latest tweets")
    val crawler = new Crawler

    latestUrls.par foreach { case (statusId, uri) =>
      val currentElement = crawledElements.getAndIncrement
      if (!Storage.alreadyCrawled(statusId, uri)) {

        val result = try {
          log.info("[{}/{}] Fetching {}", currentElement.toString, String.valueOf(totalElements), uri)
          crawler.fetch(uri)
        } catch {
          case t: Throwable =>
            log.error("[{}/{}] Fetching failed", currentElement.toString, String.valueOf(totalElements), t)
            None
        }

        result match {
          case Some((realUri, charset, html)) =>
            Storage.saveCrawledWebsite(uri, realUri, statusId, new DateTime(), charset.displayName, html)
            log.info("[{}/{}] Fetched {}", currentElement.toString, String.valueOf(totalElements), realUri)
          case _ =>
            log.warn("[" + currentElement + "/" + totalElements + "] Nothing found")
        }
      }
    }
  }


}
