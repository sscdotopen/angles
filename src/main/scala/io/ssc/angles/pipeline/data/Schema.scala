package io.ssc.data

import scalikejdbc._
import org.joda.time.DateTime
import twitter4j.{Status, TwitterObjectFactory}


object Explorer extends SQLSyntaxSupport[Explorer] {

  override val tableName = "explorers"

  def apply(rs: WrappedResultSet) = {
    new Explorer(rs.long("id"), rs.string("screenname"), rs.string("name"), rs.stringOpt("description"))
  }
}

case class Explorer(id: Long, screenname: String, name: String, description: Option[String])


object Tweet extends SQLSyntaxSupport[Tweet] {

  override val tableName = "tweets"

  def apply(rs: WrappedResultSet) = {
    new Tweet(rs.long("id"), rs.long("explorer_id"), rs.jodaDateTime("creation_time"), rs.jodaDateTime("fetch_time"),
              rs.string("json"))
  }
}

case class Tweet(id: Long, explorerId: Long, creationTime: DateTime, fetchTime: DateTime, json: String) {
  def status(): Status = {
    TwitterObjectFactory.createStatus(json)
  }
}


object CrawledWebsite extends SQLSyntaxSupport[CrawledWebsite] {

  override val tableName = "crawled_websites"

  def apply(rs: WrappedResultSet) = {
    new CrawledWebsite(rs.long("id"), rs.string("uri"), rs.string("real_uri"), rs.long("tweet_id"),
                       rs.jodaDateTime("fetch_time"), rs.string("charset"), rs.string("html"))
  }
}

case class CrawledWebsite(id: Long, uri: String, realUri: String, tweetId: Long, fetchTime: DateTime, charset: String, html: String)



object CrawledWebsiteMetadata extends SQLSyntaxSupport[CrawledWebsiteMetadata] {

  override val tableName = "crawled_website_metadata"

  def apply(rs: WrappedResultSet) = {
    new CrawledWebsiteMetadata(rs.long("crawled_website_id"), rs.string("key"), rs.string("value"))
  }
}

case class CrawledWebsiteMetadata(crawledWebsiteId: Long, key: String, value: String)


object CrawledWebsiteNamedEntity extends SQLSyntaxSupport[CrawledWebsiteNamedEntity] {

  override val tableName = "crawled_website_named_entities"

  def apply(rs: WrappedResultSet) = {
    new CrawledWebsiteNamedEntity(rs.string("name"), rs.string("entity_type"), rs.long("website_id"), rs.int("count"))
  }
}

case class CrawledWebsiteNamedEntity(name: String, entityType: String, websiteId: Long, count: Int)