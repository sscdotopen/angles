package io.ssc.angles.pipeline.batch

import io.ssc.angles.pipeline.{ExtractMetadata, ExtractNamedEntities, IndexArticles}
import org.joda.time.DateTime


object UpdateStoryIndex extends App {

  val since = new DateTime().minusDays(5)

  //new FetchTimelines().execute(since)
  //new CrawlUris().execute(since)

  new ExtractMetadata().execute(since)
  new ExtractNamedEntities().execute(since)

  new IndexArticles().execute(since)
}
