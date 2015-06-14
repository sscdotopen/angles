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

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import io.ssc.angles.pipeline.data.{Storage, TwitterApi}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import twitter4j.{ResponseList, Status}

import scala.collection.JavaConversions._

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
