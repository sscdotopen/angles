// Copyright (c) 2015 Niklas Wolber
// This file is licensed under the MIT license.
// See the LICENSE file for more information.
package main

import (
	"log"
	"net/http"
	"strings"

	"code.google.com/p/go-uuid/uuid"

	r "github.com/dancannon/gorethink"
)

const (
	numWorkers = 50
)

func main() {
	db := initDb()

	log.Println("Fetching tweets")

	uris := db.getUncrawledUris()
	done := make(chan struct{})

	for i := 0; i < numWorkers; i++ {
		go func() {
			for {
				uri, ok := <-uris

				if !ok {
					break
				}

				log.Println("Fetching", uri)
				resp, err := http.Head(uri)

				var realURI string
				if err != nil {
					log.Println(err)
					realURI = "INVALID"
				} else {
					func() {
						defer resp.Body.Close()
						realURI = resp.Request.URL.String()
						log.Println("Fetched", realURI)
					}()
				}
				db.storeRealURI(uri, realURI)
			}
			done <- struct{}{}
		}()
	}

	for i := 0; i < numWorkers; i++ {
		<-done
	}

	db.close()
	log.Println("Done.")
}

type db struct {
	tempTable string
	session   *r.Session
}

// initDb initializes the database connection and sets up a temporary table
func initDb() *db {
	log.Println("Connecting to database")

	session, err := r.Connect(r.ConnectOpts{
		Address:  "localhost:28015",
		Database: "angles",
	})

	if err != nil {
		log.Fatalln(err)
	}

	session.Use("angles")

	// create temporary table to hold results
	tempTable := strings.Replace(uuid.New(), "-", "_", -1)
	if _, err := r.Db("angles").TableCreate(tempTable, r.TableCreateOpts{Durability: "soft"}).Run(session); err != nil {
		log.Fatalln(err)
	}
	if _, err := r.Table(tempTable).IndexCreate("uri").Run(session); err != nil {
		log.Fatalln(err)
	}

	return &db{
		tempTable: tempTable,
		session:   session,
	}
}

func (db *db) getUncrawledUris() chan string {
	uris := make(chan string)
	// prepare ReQL statement
	uncrawled := func(row r.Term) interface{} {
		return row.HasFields("realUri").Not()
	}
	selectURI := func(row r.Term) interface{} {
		return row.Field("uri")
	}
	cursor, err := r.
		Table("tweetedUris").
		Filter(uncrawled).
		Map(selectURI).
		Run(db.session)

	if err != nil {
		log.Fatalln(err)
	}

	// return results
	go func() {
		defer cursor.Close()

		var uri string
		// we only want distinct results
		// we could do this with the ReQL Distinct statement, but with large
		// result sets this will fail (row count > 100.000)
		// since we don't know the result set size before hand,
		// we can't up the limit
		seenURIs := make(map[string]struct{})
		for cursor.Next(&uri) {
			if _, exists := seenURIs[uri]; exists {
				continue
			}

			// mark URI as seen, so we only get distinct URIs
			seenURIs[uri] = struct{}{}
			uris <- uri
		}

		// tell any receivers, that we are done
		close(uris)

		if err = cursor.Err(); err != nil {
			log.Fatalln(err)
		}
	}()

	return uris
}

func (db *db) storeRealURI(uri, realURI string) {
	result, err := r.
		Table(db.tempTable).
		// don't wait for disk to confirm write, instead return as soon as possible
		Insert(map[string]interface{}{"uri": uri, "realUri": realURI}, r.InsertOpts{Durability: "soft"}).
		RunWrite(db.session)

	if err != nil {
		log.Fatalln(err)
	}

	if result.Created == 0 && result.Inserted == 0 {
		log.Println("Insert affected 0 rows, huh?!")
	}

	if erro := result.FirstError; erro != "" {
		log.Fatalln(erro)
	}
}

// close merges the crawl results back into the tweetedUris table
// and does some clean up work
func (db *db) close() {
	// merge results back into the original table
	log.Println("Merging temporary results.")
	updatedURIs := r.
		Table(db.tempTable).
		EqJoin("uri", r.Table("tweetedUris"), r.EqJoinOpts{Index: "uri"}).
		Zip()
	r.
		Table("tweetedUris").
		Insert(updatedURIs, r.InsertOpts{Conflict: "replace"}).
		RunWrite(db.session)

	log.Println("Cleaning up.")
	// remove temporary table
	r.Db("angles").TableDrop(db.tempTable).Run(db.session)

	db.session.Close()
}
