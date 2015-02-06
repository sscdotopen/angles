
package io.ssc.angles.pipeline.batch

import io.ssc.angles.pipeline._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import twitter4j.TwitterException

import scala.collection.mutable.Queue


object UpdateStoryIndex extends App {
  val log = LoggerFactory.getLogger(UpdateStoryIndex.getClass)

  val work = new Queue[Step]()
  work.enqueue(new FetchTimelines)
  work.enqueue(new CrawlUris)
  work.enqueue(new ExtractMetadata)
  work.enqueue(new ExtractNamedEntities)
  work.enqueue(new IndexArticles)

  val since = new DateTime().minusDays(5)
  do {
    try {
      val step = work.dequeue()
      step.execute(since)
    } catch {
      case tex: TwitterException => {
        if (tex.exceededRateLimitation()) {
          val limit = tex.getRateLimitStatus

          val sleepTime = limit.getSecondsUntilReset + 1
          log.info("Rate limit exceeded, going to sleep for {} seconds and then try again", sleepTime)
          Thread.sleep(sleepTime * 1000)
        } else {
          // Ugh?!
          log.error("{}", tex)
        }
      }
    }
  }  while (!work.isEmpty)
}
