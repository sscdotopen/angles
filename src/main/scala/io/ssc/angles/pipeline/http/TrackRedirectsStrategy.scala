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
