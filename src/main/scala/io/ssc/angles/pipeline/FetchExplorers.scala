package io.ssc.angles.pipeline

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import io.ssc.angles.pipeline.data.{Storage, TwitterApi}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import twitter4j.{ResponseList, User}

import scala.collection.JavaConversions._

/**
 * Created by niklas on 28.01.15.
 */
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
