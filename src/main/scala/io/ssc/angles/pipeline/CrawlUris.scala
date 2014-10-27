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
    val crawler = new Crawler

    latestUrls.par foreach { case (statusId, uri) =>
      if (!Storage.alreadyCrawled(statusId, uri)) {

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
    }
  }


}
