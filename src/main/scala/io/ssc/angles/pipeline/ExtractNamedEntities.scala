package io.ssc.angles.pipeline

import io.ssc.angles.pipeline.data.Storage
import io.ssc.angles.pipeline.filters.GermanFilter
import io.ssc.angles.pipeline.nlp.{NLPUtils, NamedEntityRecognition}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory


class ExtractNamedEntities extends Step {

  val log = LoggerFactory.getLogger(classOf[ExtractNamedEntities])


  override def execute(since: DateTime): Unit = {

    val websites = Storage.crawledWebsites(since)

    websites
    .filter { website =>
      val metadata = Storage.metadataFor(website.id)
      GermanFilter.passes(website, metadata)
    }
    .filter { website =>
      Storage.namedEntities(website.id).isEmpty
    }
    .foreach { website =>

      val text = NLPUtils.extractArticle(website.html)

      val entities = NamedEntityRecognition.namedEntities(text)
      log.info("Entites for {}", website.realUri)
      for ((entity, count) <- entities) {
        log.info("{} {}", entity, count)
        Storage.saveNamedEntity(entity.name, entity.entityType, website.id, count)
      }
      log.info("")
    }
  }
}
