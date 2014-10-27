package io.ssc.angles.pipeline.http

import java.security.cert.X509Certificate

import org.apache.http.conn.ssl.TrustStrategy


class TrustEverybodyStrategy extends TrustStrategy {
  override def isTrusted(chain: Array[X509Certificate], authType: String): Boolean = true
}
