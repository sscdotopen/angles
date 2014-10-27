package io.ssc.angles.pipeline.data

import io.ssc.angles.Config
import twitter4j.conf.ConfigurationBuilder
import twitter4j.TwitterFactory


object TwitterApi {

  def connect() = {
    val config = new ConfigurationBuilder()
      .setOAuthConsumerKey(Config.property("twitter.oAuthConsumerKey"))
      .setOAuthConsumerSecret(Config.property("twitter.oAuthConsumerSecret"))
      .setOAuthAccessToken(Config.property("twitter.oAuthAccessToken"))
      .setOAuthAccessTokenSecret(Config.property("twitter.oAuthAccessTokenSecret"))
      .setJSONStoreEnabled(true)
      .build
    new TwitterFactory(config).getInstance
  }
}
