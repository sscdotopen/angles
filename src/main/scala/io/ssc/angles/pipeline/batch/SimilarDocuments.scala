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

package io.ssc.angles.pipeline.batch

import java.io.File
import java.util

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer
import edu.uci.ics.jung.graph.UndirectedSparseGraph
import io.ssc.angles.Config
import io.ssc.angles.pipeline.nlp.German
import org.apache.lucene.analysis.de.GermanAnalyzer
import org.apache.lucene.index.{DirectoryReader, DocsEnum, MultiFields}
import org.apache.lucene.search.similarities.{DefaultSimilarity, TFIDFSimilarity}
import org.apache.lucene.search.{DocIdSetIterator, IndexSearcher}
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.{BytesRef, Version}
import org.apache.mahout.math.{DenseVector, Vector}

import scala.collection.JavaConversions._
import scala.collection.mutable

object SimilarDocuments extends App {

  override def main(params: Array[String]) {

    val dataDir = Config.property("angles.dataDir")

    val analyzer = new GermanAnalyzer(Version.LUCENE_42, German.stopSet())
    val directory = FSDirectory.open(new File(dataDir, "articleIndex"))

    val reader = DirectoryReader.open(directory)
    val searcher = new IndexSearcher(reader)

    var communityFilename: String = null
    if (params.length > 1) {
      communityFilename = params(0)
    } else {
      communityFilename = "communities-manual.tsv"
    }

    //  val fields = MultiFields.getFields(reader)

    val tfidfSimilarity: TFIDFSimilarity = new DefaultSimilarity()

    case class IndexAndIdf(index: Int, idf: Float)

    val dictionary = mutable.Map[String, IndexAndIdf]()

    val field = "body"

    val termEnum = MultiFields.getTerms(reader, field).iterator(null)
    var bytesRef: BytesRef = null
    var break = false
    var index = 0
    while (!break) {
      bytesRef = termEnum.next()
      if (bytesRef != null) {
        if (termEnum.seekExact(bytesRef)) {
          val term = bytesRef.utf8ToString()
          val idf = tfidfSimilarity.idf(termEnum.docFreq, reader.numDocs)
          //println(term + " " + idf)
          dictionary.put(term, IndexAndIdf(index, idf))
          index += 1
        }
      } else {
        break = true
      }
    }

    var corpus = (for (docId <- 0 until reader.maxDoc) yield {
      docId -> new DenseVector(dictionary.size).asInstanceOf[Vector]
    }).toMap

    val termEnum2 = MultiFields.getTerms(reader, field).iterator(null)
    var bytesRef2: BytesRef = null
    var docsEnum: DocsEnum = null
    var break2 = false
    while (!break2) {
      bytesRef2 = termEnum2.next()
      if (bytesRef2 != null) {
        if (termEnum2.seekExact(bytesRef2)) {
          val term = bytesRef2.utf8ToString()
          docsEnum = termEnum2.docs(null, docsEnum)

          var break3 = false

          while (!break3) {
            val nextDocId = docsEnum.nextDoc
            if (nextDocId != DocIdSetIterator.NO_MORE_DOCS) {
              //println(term + " " + nextDocId + " " + docsEnum.freq)

              val tf = tfidfSimilarity.tf(docsEnum.freq())
              val indexAndIdf = dictionary(term)
              corpus(nextDocId).setQuick(indexAndIdf.index, tf * indexAndIdf.idf)

            } else {
              break3 = true
            }
          }

        }
      } else {
        break2 = true
      }
    }

    corpus = corpus
      .filter { case (docId, vector) => vector.getLengthSquared > 0 }
      .map { case (docId, vector) => docId -> vector.normalize() }

    var similarities = for ((docIdA, vectorA) <- corpus;
                            (docIdB, vectorB) <- corpus;
                            if (docIdA > docIdB)) yield {
      (docIdA, docIdB) -> vectorA.dot(vectorB)
    }

    val lowerThreshold = 0.15
    val upperThreshold = 0.95

    similarities = similarities filter { case ((docIdA, docIdB), similarity) => similarity > lowerThreshold && similarity < upperThreshold }


    similarities foreach { case ((docIdA, docIdB), similarity) =>
      val titleA = reader.document(docIdA).getField("title").stringValue()
      val titleB = reader.document(docIdB).getField("title").stringValue()

      val uriA = reader.document(docIdA).getField("uri").stringValue()
      val uriB = reader.document(docIdB).getField("uri").stringValue()

      val contentA = reader.document(docIdA).getField(field).stringValue()
      val contentB = reader.document(docIdB).getField(field).stringValue()

      //println("\n" + titleA + " <-> " + titleB + ":\n[" + contentA + "]\n[" + contentB + "]\n" + similarity + "\n")
      //println("\n" + titleA + " <-> " + titleB + ":\n[" + uriA + "]\n[" + uriB + "]\n" + similarity + "\n")
    }

    val docIds = (similarities flatMap { case ((docIdA, docIdB), similarity) => Array(docIdA, docIdB) }).toSet.toArray.sorted


    val similarityGraph = new UndirectedSparseGraph[Int, Int]()

    docIds foreach {
      similarityGraph.addVertex(_)
    }

    val edgeWeights = mutable.Map[Int, Double]()

    var edgeIndex = 0
    similarities foreach { case ((docIdA, docIdB), similarity) =>
      similarityGraph.addEdge(edgeIndex, docIdA, docIdB)
      edgeWeights.put(edgeIndex, similarity)
      edgeIndex += 1
    }

    var community = 0

    val explorerToCommunity = mutable.Map[String, Int]()

    scala.io.Source.fromFile(new File(dataDir, communityFilename)).getLines foreach { line =>
      if (!line.startsWith("#")) {
        if (line == "") {
          community += 1
        } else {
          explorerToCommunity.put(line.toLowerCase, community)
        }
      }
    }

    case class AvgAndStdDev(average: Double, stdDev: Double)

    val retweetRates = (scala.io.Source.fromFile(new File(dataDir, "retweetrates.tsv")).getLines map { line =>
      val tokens = line.split("\t")
      tokens(0) -> AvgAndStdDev(tokens(1).toDouble, tokens(2).toDouble)
    }).toMap


    val clusterer = new WeakComponentClusterer[Int, Int]()

    val components: util.Set[util.Set[Int]] = clusterer.transform(similarityGraph)

    var articlesDisplayed = 0

    components foreach { vertices =>

      val tags = mutable.Map[String, Int]()

      vertices foreach { docId =>
        for (tag <- reader.document(docId).getField("hashtags").stringValue().split(" ")) {
          if (tag != "") {
            val count = tags.getOrElse(tag, 0)
            tags.put(tag, count + 1)
          }
        }
      }

      val topTags = tags.toArray
        .sortBy({ case (tag, count) => count })
        .filter({ case (tag, count) => count > 1 })
        .takeRight(3).reverse
        .map({ case (tag, count) => "#" + tag })

      //TODO should be numClusters
      val numExplorers = (vertices map { docId => reader.document(docId).getField("screenname").stringValue() }).toSet.size

      val communities = (vertices map { docId =>
        val name = reader.document(docId).getField("screenname").stringValue()
        explorerToCommunity(name.toLowerCase)
      }).toSet

      val newsItems = vertices map { docId =>
        val title = reader.document(docId).getField("title").stringValue()
        val description = reader.document(docId).getField("description").stringValue()
        val uri = reader.document(docId).getField("uri").stringValue()
        val via = reader.document(docId).getField("screenname").stringValue()
        val community = explorerToCommunity(reader.document(docId).getField("screenname").stringValue().toLowerCase)
        val retweetRate = reader.document(docId).getField("retweetsPerHourInDayOne").stringValue().toDouble

        val ground = retweetRates(via.toLowerCase)

        NewsItem(title, description, uri, via, community, retweetRate, retweetScore(retweetRate, ground))
      }


      if (numExplorers > 1) {
        //if (communities.size > 1) {
        println("\n\n[" + topTags.mkString(" ") + "] " + vertices.size + " newsItems from " + communities.size + " communities")

        for (community <- communities) {
          println("----------")

          val newsItemsToDisplay = newsItems.toSeq.filter {
            _.community == community
          }
            .sortBy(_.stddevs)
            .reverse

          newsItemsToDisplay
            .foreach { newsItem =>

            println("\t [" + newsItem.title + "] via [" + newsItem.via + "]")
            println("\t [" + newsItem.description + "]")
            println("\t [" + newsItem.uri + "]")
            println("\t [" + newsItem.retweetRate + ", " + newsItem.stddevs + "]")
            println()
          }

          articlesDisplayed += newsItemsToDisplay.size
        }
      }
    }

    def retweetScore(rate: Double, ground: AvgAndStdDev): Double = {
      (rate - ground.average) / ground.stdDev
    }

    println("Displayed " + articlesDisplayed + " from " + reader.numDocs() + " articles")

    /*
  println()

  for (docId <- 0 until reader.numDocs) {
    val title = reader.document(docId).getField("title").stringValue()
    val uri = reader.document(docId).getField("uri").stringValue()
    val tags = reader.document(docId).getField("entities").stringValue()
    println(title)
    println(uri)
    println(tags)
    println()
  }*/



    directory.close()
  }
}

case class NewsItem(title: String, description: String, uri: String, via: String, community: Int, retweetRate: Double, stddevs: Double)