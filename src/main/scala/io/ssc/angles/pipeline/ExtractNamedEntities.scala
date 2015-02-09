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

import io.ssc.angles.pipeline.data.Storage
import io.ssc.angles.pipeline.filters.GermanFilter
import io.ssc.angles.pipeline.nlp.{NLPUtils, NamedEntityRecognition}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory


class ExtractNamedEntities extends Step {

  val log = LoggerFactory.getLogger(classOf[ExtractNamedEntities])


  override def execute(since: DateTime): Unit = {

    log.info("Extracing named entities ...")

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
//      log.info("Entites for {}", website.realUri)
      for ((entity, count) <- entities) {
//        log.info("{} {}", entity, count)
        Storage.saveNamedEntity(entity.name, entity.entityType, website.id, count)
      }
//      log.info("")
    }
  }
}
