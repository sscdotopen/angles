package io.ssc.angles.pipeline

import java.sql.SQLException

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import io.ssc.angles.pipeline.data.{Storage, TwitterApi}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import twitter4j.{ResponseList, Status}

import scala.collection.JavaConversions._

/**
 * Created by niklas on 28.01.15.
 */
class FetchRetweets extends Step {
  val log = LoggerFactory.getLogger(classOf[FetchRetweets])

  override def execute(since: DateTime): Unit = {
    log.info("Fetching retweets ...")

    val twitterApi = TwitterApi.connect()

    val tweets = Storage.notFollowedTweets()
    val fetchTime = DateTime.now()

    for (tweetId <- tweets) {

      val retweets: ResponseList[Status] = twitterApi.tweets().getRetweets(tweetId)

      for (retweet: Status <- retweets) {
        try {
          Storage.saveTweet(retweet, retweet.getUser.getId, fetchTime, Some(tweetId))
        } catch {
          case _: MySQLIntegrityConstraintViolationException => log.warn("Error while saving retweet, duplicate?")
        }
      }

      Storage.markTweetFollowed(tweetId)
      log.info("Fetched {} retweets for tweet {}", retweets.size(), tweetId)
    }
  }
}
