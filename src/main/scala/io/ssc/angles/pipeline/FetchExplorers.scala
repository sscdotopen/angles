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
import twitter4j.{ResponseList, User}

import scala.collection.JavaConversions._

class FetchExplorers extends Step {
  val log = LoggerFactory.getLogger(classOf[FetchExplorers])

  override def execute(since: DateTime): Unit = {
    log.info("Fetching explorers ...")
    val twitterApi = TwitterApi.connect()

    val userIds = Storage.notCrawledExplorers().toArray

    log.info("{} explorers to fetch", userIds.length)

    if (userIds.length == 0)
      return

    userIds
      .grouped(100)
      .foreach { case users =>
        val explorers: ResponseList[User] = twitterApi.users().lookupUsers(users)

        for (explorer: User <- explorers) {
          try {
            Storage.saveExplorer(explorer)
            log.info("Fetched {}", explorer.getScreenName)
          } catch {
            case _: MySQLIntegrityConstraintViolationException => log.info("Error while saving explorer, duplicate?")
          }
        }
      }
  }
}
