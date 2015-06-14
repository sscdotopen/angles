/**
 * Angles
 * Copyright (C) 2015 Jakob Hendeß, Niklas Wolber
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
import io.ssc.angles.pipeline.filters.{ArticleFilter, GermanFilter}
import io.ssc.data.CrawledWebsite
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import scalikejdbc.DB

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
