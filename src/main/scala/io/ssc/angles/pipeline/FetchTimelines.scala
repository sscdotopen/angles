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

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import io.ssc.angles.pipeline.data.{Storage, TwitterApi}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import twitter4j.{Paging, ResponseList, Status}

import scala.collection.JavaConversions._

class FetchTimelines extends Step {

  val log = LoggerFactory.getLogger(classOf[FetchTimelines])

  val pageSize = 100

  override def execute(since: DateTime): Unit = {
    log.info("Fetching timelines ...")

    val twitterApi = TwitterApi.connect()

    val explorers = Storage.allExplorers()
    val maxTweetIdPerExplorer = Storage.maxTweetIdPerExplorer()

    val fetchTime = DateTime.now()

    for (explorer <- explorers) {
      var statuses: ResponseList[Status] = null
      var page = 1

      do {

        val paging = if (maxTweetIdPerExplorer.contains(explorer.id)) {
          new Paging(page, pageSize, maxTweetIdPerExplorer(explorer.id))
        } else {
          new Paging(page, pageSize)
        }

        log.info("Requesting page  {} of @{}", page, explorer.screenname)
        statuses = twitterApi.timelines().getUserTimeline(explorer.id, paging)

        var tweetsAdded = 0
        for (status <- statuses) {
          try {
            val saved = Storage.saveTweet(status, explorer.id, fetchTime, None)
            if (saved) {
              tweetsAdded += 1
            }
          } catch {
            case _: MySQLIntegrityConstraintViolationException => log.warn("Error while saving retweet, duplicate?")
          }
        }

        log.info("Saved {} tweets", tweetsAdded)
        page += 1

      } while (statuses.size == pageSize)
    }

  }


}