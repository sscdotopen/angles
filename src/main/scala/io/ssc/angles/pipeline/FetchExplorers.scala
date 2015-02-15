package io.ssc.angles.pipeline

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

    val explorers: ResponseList[User]  = twitterApi.users().lookupUsers(userIds)

    for (explorer: User <- explorers) {
      Storage.saveExplorer(explorer)
      log.info("Fetched {}", explorer.getScreenName)
    }
  }
}
