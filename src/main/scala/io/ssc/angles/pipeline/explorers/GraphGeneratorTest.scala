package io.ssc.angles.pipeline.explorers

import java.net.URI

import scala.collection.mutable

/**
 * Created by xolor on 11.02.15.
 */
object GraphGeneratorTest extends App{
  
  val uriToString = (uri: URI) => uri.toString

  var testList : mutable.MutableList[ExplorerUriPair] = new mutable.MutableList
  
  testList += new ExplorerUriPair("Hugo", List(URI.create("http://google.com/")))
  testList += new ExplorerUriPair("Hugo", List(URI.create("http://google.com/"), URI.create("https://google.com/")))
  testList += new ExplorerUriPair("Peter", List(URI.create("http://golem.de/"), URI.create("http://google.com/")))
  testList += new ExplorerUriPair("Gerda", List(URI.create("http://google.com/")))

  val gen = new GraphGenerator

  var result = gen.execute(testList.toList, uriToString, gen.COSINE_SIMILARITY)
  
  println(result)
  
}
