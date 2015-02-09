package io.ssc.angles.pipeline.explorers

import java.net.URI

import scala.collection.mutable

/**
 * Created by xolor on 11.02.15.
 */
object GraphGeneratorTest extends App{
  
  val uriToString = (uri: URI) => uri.toString

  var testList : mutable.MutableList[ClusterableTweet] = new mutable.MutableList
  
  testList += new ClusterableTweet("Hugo", List(URI.create("http://google.com/")))
  testList += new ClusterableTweet("Hugo", List(URI.create("http://google.com/"), URI.create("https://google.com/")))
  testList += new ClusterableTweet("Peter", List(URI.create("http://golem.de/"), URI.create("http://google.com/")))
  testList += new ClusterableTweet("Gerda", List(URI.create("http://google.com/")))

  val gen = new GraphGenerator

  var result = gen.execute(testList.toList, uriToString, gen.COSINE_SIMILARITY)
  
  println(result)
  
}
