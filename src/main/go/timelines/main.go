// Copyright (c) 2015 Niklas Wolber
// This file is licensed under the MIT license.
// See the LICENSE file for more information.
package main

import (
	"flag"
	"fmt"
	"log"
	"net/url"
	"os"

	twitter "github.com/ChimeraCoder/anaconda"
	r "github.com/dancannon/gorethink"
)

const (
	twitterConsumerKey       = "TWITTER_CONSUMER_KEY"
	twitterConsumerSecret    = "TWITTER_CONSUMER_SECRET"
	twitterApplicationKey    = "TWITTER_APPLICATION_KEY"
	twitterApplicationSecret = "TWITTER_APPLICATION_SECRET"
)

func main() {
	var help bool
	flag.BoolVar(&help, "h", false, "Display this help")
	flag.Parse()

	if help {
		fmt.Println("timelines expects to find Twitter credentials in four environment variables")
		fmt.Println("TWITTER_CONSUMER_KEY")
		fmt.Println("TWITTER_CONSUMER_SECRET")
		fmt.Println("TWITTER_APPLICATION_KEY")
		fmt.Println("TWITTER_APPLICATION_SECRET")
		return
	}

	// init rethinkdb
	session := dbConnect()
	defer session.Close()
	// init Twitter API
	api := configureTwitterAPI()
	defer api.Close()

	explorers := make(chan explorer)

	// fetch explorers
	go getExplorers(session, explorers)

	for {
		exp, ok := <-explorers

		if !ok {
			log.Println("No more explorers. Exiting")
			break
		}

		tweets := exp.getTimeline(api, session)
		for {
			tweet, ok := <-tweets

			if !ok {
				break
			}

			for _, uri := range tweet.Entities.Urls {
				storeURI(session, exp.id, tweet.Id, uri.Expanded_url)
			}
		}
	}
}

// getExplorers delivers the explorers in ascending order
// with respect to the last fetch time
func getExplorers(session *r.Session, explorers chan explorer) {
	log.Println("Fetching explorers")
	cursor, err := r.
		Table("explorers").
		OrderBy(r.OrderByOpts{Index: "fetchTime"}).
		Run(session)

	if err != nil {
		log.Fatalln(err)
	}
	defer cursor.Close()

	var exp explorer
	var row map[string]interface{}
	for cursor.Next(&row) {
		exp.id = int64(row["id"].(float64))
		exp.screenName = row["screen_name"].(string)
		exp.maxID = int64(row["maxID"].(float64))
		exp.fetchTime = int64(row["fetchTime"].(float64))
		explorers <- exp
	}

	if err = cursor.Err(); err != nil {
		log.Fatalln(err)
	}

	close(explorers)
}

type explorer struct {
	id         int64
	screenName string `gorethink:"screen_name"`
	fetchTime  int64
	maxID      int64
}

// update perists the latest timeline state (i.e. fetchTime and maxID)
func (e *explorer) update(session *r.Session, maxID int64) {
	r.
		Table("explorers").
		Update(map[string]interface{}{
		"id":        e.id,
		"fetchTime": r.Now(),
		"maxID":     maxID,
	}).
		RunWrite(session)
}

// getTimeline returns all tweets of an explorer, which are younger than his maxID
func (e *explorer) getTimeline(api *twitter.TwitterApi, session *r.Session) chan twitter.Tweet {
	t := make(chan twitter.Tweet)

	log.Println("Fetching", e.screenName)

	go func() {
		// the minimum id, that has been returned
		var minID int64
		defer e.update(session, minID)

		for {
			queryParams := url.Values{}
			queryParams.Add("screen_name", e.screenName)
			queryParams.Add("count", "200")
			queryParams.Add("trim_user", "true")

			if minID > 0 {
				// max_id returns tweets with IDs less than or equal to max_id.
				// In the case where we reached the oldest tweet, this would
				// return the oldest tweet over and over again. So we decrement
				// the value by one.
				queryParams.Add("max_id", fmt.Sprintf("%d", minID-1))
			}

			// automatically stops and retries, when rate limit is reached
			tweets, err := api.GetUserTimeline(queryParams)

			if err != nil {
				log.Println(err)
				close(t)
				return
			}

			if len(tweets) == 0 {
				log.Println("No more tweets.")
				close(t)
				return
			}

			log.Println("Fetched", len(tweets), "tweets from @", e.screenName)

			// process and deliver tweets
			for _, tweet := range tweets {
				if tweet.Id <= e.maxID {
					// we have seen enough
					log.Println("maxID reached, stopping.")
					close(t)
					return
				}

				if tweet.Id < minID || minID == 0 {
					minID = tweet.Id
				}

				t <- tweet
			}
		}
	}()

	return t
}

// storeURI persists a URI tweeted by an explorer in the database
func storeURI(session *r.Session, explorerID, tweetID int64, uri string) {
	r.
		Table("tweetedUris").
		Insert(map[string]interface{}{
		"explorer": explorerID,
		"tweet":    tweetID,
		"uri":      uri,
	}).
		RunWrite(session)
}

func dbConnect() *r.Session {
	log.Println("Connecting to database")
	var session *r.Session

	session, err := r.Connect(r.ConnectOpts{
		Address:  "localhost:28015",
		Database: "angles",
	})

	if err != nil {
		log.Fatalln(err)
	}

	session.Use("angles")
	return session
}

// configureTwitterAPI sets the credentials for the Twitter API
func configureTwitterAPI() *twitter.TwitterApi {
	twitter.SetConsumerKey(os.Getenv(twitterConsumerKey))
	twitter.SetConsumerSecret(os.Getenv(twitterConsumerSecret))

	return twitter.NewTwitterApi(os.Getenv(twitterApplicationKey), os.Getenv(twitterApplicationSecret))
}
