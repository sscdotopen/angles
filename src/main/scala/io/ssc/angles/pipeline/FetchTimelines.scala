package io.ssc.angles.pipeline

import io.ssc.angles.pipeline.data.{Storage, TwitterApi}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import twitter4j.{Paging, ResponseList, Status}

import scala.collection.JavaConversions._

class FetchTimelines extends Step {

  val log = LoggerFactory.getLogger(classOf[FetchTimelines])

  val pageSize = 100

  override def execute(since: DateTime): Unit = {

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
          val saved = Storage.saveTweet(status, explorer.id, fetchTime)
          if (saved) {
            tweetsAdded += 1
          }
        }

        log.info("Saved {} tweets", tweetsAdded)
        page += 1

      } while (statuses.size == pageSize)
    }

  }


}