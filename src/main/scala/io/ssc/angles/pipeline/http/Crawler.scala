package io.ssc.angles.pipeline.http

import java.io.{Closeable, InputStreamReader}
import java.nio.charset.Charset

import com.google.common.base.Charsets
import com.google.common.io.{CharStreams, Closeables}
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.ssl._
import org.apache.http.impl.NoConnectionReuseStrategy
import org.apache.http.impl.client.HttpClientBuilder

class Crawler extends Closeable {

  private[this] val redirectTracker = new TrackRedirectsStrategy

  val builder = new SSLContextBuilder()
  builder.loadTrustMaterial(null, new TrustEverybodyStrategy)
  val socketFactory = new SSLConnectionSocketFactory(builder.build())

  private[this] val httpClient = HttpClientBuilder.create()
    //.setConnectionManager(new PoolingHttpClientConnectionManager())
    .setUserAgent("Angles-Crawler-Beta")
    .setSSLSocketFactory(socketFactory)
    //.setMaxConnTotal(250)
    .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE)
    .setRedirectStrategy(redirectTracker)
    .build()

  
  def fetch(uri: String): Option[(String, Charset, String)] = {

    val request = new HttpGet(uri)

    val response = httpClient.execute(request)

    val contentType = response.getEntity.getContentType.getValue

    if (contentType.startsWith("text/html")) {

      val charset = if (contentType.contains("charset=ISO-8859-1")) {
        Charsets.ISO_8859_1
      } else {
        Charsets.UTF_8
      }

      var realUri = uri
      while (redirectTracker.resolveUri(realUri) != null && redirectTracker.resolveUri(realUri) != realUri) {
        realUri = redirectTracker.resolveUri(realUri)
      }

      val stream = response.getEntity.getContent
      val html = CharStreams.toString(new InputStreamReader(stream, charset))

      Closeables.close(stream, false)

      Some((realUri, charset, html))
    } else {
      None
    }

  }

  override def close() = {
    httpClient.close()
  }
}
