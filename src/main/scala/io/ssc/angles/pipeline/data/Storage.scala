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

package io.ssc.angles.pipeline.data

import io.ssc.angles.Config
import io.ssc.data._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import scalikejdbc._
import twitter4j.{Status, TwitterObjectFactory, User}

import scala.collection.mutable
;

object Storage {
  val log = LoggerFactory.getLogger(Storage.getClass)
  Class.forName("com.mysql.jdbc.Driver")

  val settings = ConnectionPoolSettings(
    validationQuery = "SELECT 1"
  )
  ConnectionPool.singleton(
    Config.property("jdbc.url"),
    Config.property("jdbc.user"),
    Config.property("jdbc.password"),
    settings
  )

  implicit val session = AutoSession

  def getConn() = {
    ConnectionPool.borrow()
  }

  def allExplorers(): List[Explorer] = {
    (sql"SELECT * FROM explorers" map { Explorer(_) }).list().apply()
  }

  def maxTweetIdPerExplorer(): Map[Long, Long] = {
    (sql"SELECT explorer_id, MAX(id) AS max_id FROM tweets GROUP BY explorer_id" map { rs =>
      rs.long("explorer_id") -> rs.long("max_id")
    }).list().apply().toMap
  }

  def tweetsSince(since: DateTime) = {
    (sql"SELECT * FROM tweets WHERE creation_time >= ${since}" map { Tweet(_) }).list().apply()
  }

  def expandedUrlsInTweetsSince(since: DateTime) = {
    (sql"SELECT * FROM tweets WHERE creation_time >= ${since} and follow_retweets=4" map { rs =>
      val status = Tweet(rs).status()

      if (status.getURLEntities.length > 0) {
        val urls = mutable.ListBuffer[(Long, String)]()
        for (entity <- status.getURLEntities) {
          //println(status.getId + " -> " + entity.getExpandedURL)
          urls += status.getId -> entity.getExpandedURL
        }
        urls.toList
      } else {
        List[(Long,String)]()
      }

    }).list().apply().flatMap { identity }
  }

  def allTweets() = {
    (sql"SELECT * FROM tweets" map { Tweet(_) }).list().apply()
  }

  def saveTweet(status: Status, explorerId: Long, fetchTime: DateTime, parentId: Option[Long]): Boolean = {

    val creationTime = new DateTime(status.getCreatedAt)

    var success = true
    try {
      withSQL {
        insert.into(Tweet).namedValues(
          Tweet.column.id -> status.getId,
          Tweet.column.explorerId -> explorerId,
          Tweet.column.creationTime -> creationTime,
          Tweet.column.fetchTime -> fetchTime,
          Tweet.column.json -> TwitterObjectFactory.getRawJSON(status),
          Tweet.column.parentTweet -> parentId,
          Tweet.column.retweetCount -> status.getRetweetCount
        )
      }.update.apply()
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        success = false
    }
    success
  }

  def crawledWebsites(since: DateTime) = {
    //TODO better check fetchtime of tweets
//    (sql"SELECT * FROM crawled_websites WHERE fetch_time >= ${since}" map { CrawledWebsite(_) }).list().apply()

    var count = 0
    Iterator.continually {
      val result = (sql"SELECT * FROM crawled_websites WHERE fetch_time >= ${since} LIMIT 500 OFFSET ${count}" map {
        CrawledWebsite(_)
      }).list().apply()
      val length = result.length
      count += length
      log.info("Fetched {} crawled websites", count)
      if (length > 0)
        Some(result)
      else
        None
    }
  }.takeWhile(_ != None).flatten.flatten

  def unmarkedWebsites(since: DateTime) = {
    //    (sql"SELECT * FROM crawled_websites WHERE fetch_time >= ${since}" map { CrawledWebsite(_) }).list().apply()

    var count = 0
    Iterator.continually {
      val result = (sql"SELECT w.* FROM crawled_websites w  JOIN tweets t on w.tweet_id = t.id WHERE w.fetch_time >= ${since} AND t.follow_retweets = 0 AND t.retweet_count > 0 LIMIT 2000 OFFSET ${count}" map {
        CrawledWebsite(_)
      }).list().apply()
      val length = result.length
      count += length
      log.info("Fetched {} unmarked websites", count)
      if (length > 0)
        Some(result)
      else
        None
    }
  }.takeWhile(_ != None).flatten.flatten

  def alreadyCrawled(tweetId: Long, uri: String) = {
    (sql"SELECT count(*) AS cnt from crawled_websites WHERE tweet_id = ${tweetId} AND uri = ${uri}" map { _.int("cnt") > 0 }).single().apply().get
  }

  def saveCrawledWebsite(uri: String, realUri: String, tweetId: Long, fetchTime: DateTime, charset: String, html: String): Boolean = {
    var success = true
    val c = CrawledWebsite.column
    try {
      withSQL {
        insert.into(CrawledWebsite)
              .columns(c.uri, c.realUri, c.tweetId, c.fetchTime, c.charset, c.html)
              .values(uri, realUri, tweetId, fetchTime, charset, html)
      }.update().apply()
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        success = false
    }
    success
  }

  def metadataFor(crawledWebsiteId: Long) = {
    val records = (sql"SELECT * FROM crawled_website_metadata where crawled_website_id = ${crawledWebsiteId}"
      map { CrawledWebsiteMetadata(_) }).list().apply()

    val metadata = mutable.Map[String, mutable.Set[String]]()

    //TODO change to multi map
    records foreach { record =>
      val values = metadata.getOrElse(record.key, mutable.Set())
      values.add(record.value)
      metadata.put(record.key, values)
    }

    (metadata map { case (key, values) => key -> values.toSet }).toMap
  }


  def saveMetadata(crawledWebsiteId: Long, key: String, value: String) = {
    var success = true
    try {
      withSQL {
        insert.into(CrawledWebsiteMetadata).values(crawledWebsiteId, key, value)
      }.update().apply()
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        success = false
    }
    success
  }

  def saveNamedEntity(name: String, entityType: String, websiteId: Long, count: Int) = {
    var success = true
    try {
      withSQL {
        insert.into(CrawledWebsiteNamedEntity).values(name, entityType, websiteId, count)
      }.update().apply()
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        success = false
    }
    success
  }

  def websitesWithLabels() = {
    (sql"SELECT * FROM website_labels" map { rs => rs.string("uri") -> rs.boolean("label") }).list().apply()
  }

  def website(uri: String) = {
    (sql"SELECT * FROM crawled_websites WHERE real_uri = ${uri} LIMIT 1" map { CrawledWebsite(_) }).single().apply()
  }

  def namedEntities(websiteId: Long) = {
    (sql"SELECT * from crawled_website_named_entities WHERE website_id = ${websiteId}" map { CrawledWebsiteNamedEntity(_) }).list().apply()
  }

  def markAsCandidate(website: CrawledWebsite) = {
    (sql"INSERT INTO candidates (website_id) VALUES (${website.id}) ON DUPLICATE KEY UPDATE website_id = website_id").update().apply()
  }

  def currentArticles() = {
    (sql"SELECT DISTINCT cw.id AS id, cwm.value AS title, cw.real_uri AS uri FROM crawled_websites cw JOIN candidates c ON cw.id = c.website_id JOIN crawled_website_metadata cwm ON cw.id = cwm.crawled_website_id WHERE cwm.key = 'meta-og:title'" map { rs =>
      (rs.long("id"), rs.string("title"), rs.string("uri"))
    }).list().apply()
  }

  def nonLabeledArticles(): List[String] = {
    (sql"SELECT DISTINCT cw.real_uri AS uri FROM crawled_websites cw LEFT JOIN website_labels wl ON cw.real_uri = wl.uri WHERE wl.uri IS NULL" map {
      rs => rs.string("uri")
    }).list().apply()
  }

  def statusesForWebsites(since: DateTime) = {
    (sql"SELECT cw.id AS id, cw.real_uri AS uri, t.json AS json FROM crawled_websites cw JOIN tweets t ON t.id = cw.tweet_id WHERE cw.fetch_time >= ${since}" map { rs =>
      (rs.long("id"), rs.string("uri"), TwitterObjectFactory.createStatus(rs.string("json")))
    }).list().apply()
  }

  def explorerOfWebsite(websiteId: Long) = {
    (sql"SELECT e.* FROM crawled_websites cw JOIN tweets t ON cw.tweet_id = t.id JOIN explorers e ON t.explorer_id = e.id WHERE cw.id = ${websiteId}" map { rs =>
      Explorer(rs)
    }).single().apply()
  }

  def statusOfWebsite(websiteId: Long) = {
    (sql"SELECT t.json AS json FROM crawled_websites cw JOIN tweets t ON cw.tweet_id = t.id WHERE cw.id = ${websiteId}" map { rs =>
      TwitterObjectFactory.createStatus(rs.string("json"))
    }).single().apply()
  }

  def notCrawledExplorers() = {
    (sql"SELECT DISTINCT t.explorer_id FROM tweets t LEFT JOIN explorers e ON t.explorer_id = e.id WHERE e.id IS NULL" map {rs => rs.long("explorer_id")}).list().apply()
  }

  def saveExplorer(user: User) = {
    withSQL {
      insert.into(Explorer).values(user.getId, user.getScreenName, user.getName, user.getDescription)
    }.update.apply()
  }
  
  def allTweetURIPairs() = {
    sql"SELECT t.explorer_id, w.real_uri FROM tweets t JOIN crawled_websites w ON t.id = w.tweet_id;" map {
      rs => (rs.string(1), rs.string(2))
    } list() apply()
  }

  def notFollowedTweets() = {
    (sql"SELECT id FROM tweets WHERE retweet_count>0 AND follow_retweets=1" map {rs => rs.long("id")}).list().apply()
  }

  def markTweetFollowed(tweet: Long): Unit = {
    withSQL {
      update(Tweet).set(
        Tweet.column.followRetweets -> 2
      ).where.eq(Tweet.column.id, tweet)
    }.update.apply()
  }

  def markTweetToFollow(tweet: Long)(implicit db: DB): Unit = {
    DB.withinTx { implicit session =>
      withSQL {
        update(Tweet).set(
          Tweet.column.followRetweets -> 1
        ).where.eq(Tweet.column.id, tweet)
      }.update.apply()
    }
  }

  def markUninterestingTweets() = {
    withSQL {
      update(Tweet).set(
        Tweet.column.followRetweets -> 3
      ).where.eq(Tweet.column.followRetweets, 0)
    }
  }

  def markTweetCrawled(tweet: Long) = {
    withSQL {
      update(Tweet).set(
        Tweet.column.followRetweets -> 0
      ).where.eq(Tweet.column.id, tweet)
    }
  }
}


