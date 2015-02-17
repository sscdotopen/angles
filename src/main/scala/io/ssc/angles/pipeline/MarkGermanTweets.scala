package io.ssc.angles.pipeline

import com.twitter.util.Config.intoList
import io.ssc.angles.pipeline.data.Storage
import io.ssc.angles.pipeline.filters.{ArticleFilter, GermanFilter}
import io.ssc.data.CrawledWebsite
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import scalikejdbc.DB

import scala.concurrent.Future

/**
 * Created by niklas on 08.02.15.
 */
class MarkGermanTweets extends Step {
  val log = LoggerFactory.getLogger(classOf[MarkGermanTweets])

  override def execute(since: DateTime): Unit = {
    val websites = Storage.unmarkedWebsites(since)

    val websitesWithMetadata = websites map { website =>
      website -> Storage.metadataFor(website.id)
    }

    var germanTweets = Set.empty[Long]
    websitesWithMetadata
      .filter { case (website, metadata) => GermanFilter.passes(website, metadata)}
      .filter { case (website, metadata) => ArticleFilter.passes(website, metadata)}
      .foreach { case (website: CrawledWebsite, metadata) =>
        val status = Storage.statusOfWebsite(website.id).get
        germanTweets += status.getId
      }

    log.info("Marking tweets with German news articles.")
    implicit val db = DB(Storage.getConn())
    try {
      db.begin()
      germanTweets
        .foreach(id => Storage.markTweetToFollow(id))
      db.commit()
    } catch {
      case _: Throwable => db.rollbackIfActive()
    }
    log.info("Marked {} tweets", germanTweets.size)

    Storage.markUninterestingTweets
  }
}
