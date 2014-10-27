package io.ssc.angles.pipeline.http

import java.net.URI
import java.util.concurrent.ConcurrentHashMap

import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.DefaultRedirectStrategy
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpRequest, HttpResponse}

class TrackRedirectsStrategy extends DefaultRedirectStrategy {

  private val trackedRedirects = new ConcurrentHashMap[String, String]()

  def resolveUri(uri: String) = trackedRedirects.get(uri)

  override def getLocationURI(request: HttpRequest, response: HttpResponse, context: HttpContext): URI = {

    val redirectUri = super.getLocationURI(request, response, context)
    val clientContext = HttpClientContext.adapt(context)

    trackedRedirects.put(clientContext.getTargetHost + request.getRequestLine.getUri, redirectUri.toString)

    redirectUri
  }
}
