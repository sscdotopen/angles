package io.ssc.angles.pipeline.nlp

import org.apache.lucene.analysis.util.CharArraySet
import org.apache.lucene.util.Version


object German {

  def stopSet(): CharArraySet = {
    val stopSet = new CharArraySet(Version.LUCENE_42, stopWords.length, true)

    stopWords foreach { word => stopSet.add(word) }

    stopSet
  }
  
  val stopWords =
    Array("der", "die", "und", "den", "von", "fuer", "das", "im", "zu", "des", "mit", "auf", "eine", "nach", "dem",
          "sich", "nicht", "ein", "hat", "bei", "werden", "es", "auch", "als", "vor", "dass", "ist", "aus", "um",
          "sei", "er", "einem", "sagte", "einer", "am", "ueber", "gegen", "einen", "haben", "sie", "zum", "zur",
          "noch", "mehr", "heute", "wird", "hatte", "sind", "soll", "wie", "habe", "bis", "werde", "aber", "so",
          "wurde", "nur", "war", "unter", "seien", "durch", "keine", "damit", "sollen", "sein", "zwei", "neuen", "ihre",
          "ab", "wegen", "angaben", "neue", "wenn", "muesse", "beim", "weiter", "zwischen", "wieder", "wurden",
          "wollen", "dies", "seiner", "eines", "waren", "bereits", "seine", "vom", "man", "oder", "hatten", "jetzt",
          "drei", "allem", "ausserdem", "weitere", "seit", "diese", "morgen", "ersten", "dann", "diesem", "haetten",
          "koenne", "alle", "gestern", "ihrer", "dabei", "arbeit", "dieser", "wuerden", "ohne", "koennen", "immer",
          "etwa", "allerdings", "nun", "dafuer", "ihren", "erneut", "bisher", "seinen", "dagegen", "naechsten",
          "beiden", "muessen", "schon", "muss", "dazu", "muessten", "zurueck", "geht", "jedoch", "ob", "sowie",
          "offenbar", "wir", "geben", "erst", "anderem", "gewesen", "ihr", "kann", "darauf", "koennten", "sieht",
          "wolle", "weil", "sollten", "mehrere", "zuvor", "seinem", "kein", "gibt", "gebe", "anderen", "dieses", "ins",
          "solle", "machen", "davon", "da", "ihrem", "gab", "doch", "sondern", "zwar", "lassen", "selbst", "dort",
          "derzeit", "deshalb", "danach", "erste", "keinen", "fast", "kommen", "weiteren", "gemacht", "heisst",
          "zugleich", "letzten", "zunaechst", "trotz", "waehrend", "steht", "darueber", "wies", "sollte", "gegeben",
          "bekannt", "hiess", "inzwischen", "staaten", "andere", "wuerde", "mitteilte", "nannte", "grosse", "einmal",
          "erwartet", "beide", "gehen", "worten", "gekommen", "haelt", "bleiben", "kam", "statt", "unterdessen",
          "bislang", "duerfe", "beginn", "hoehe", "bleibt", "nichts", "laut", "viele", "gleichzeitig", "zusammen",
          "zweiten", "woertlich", "eigenen", "ich", "erreicht", "begonnen", "bereit", "ihnen", "diesen", "gehe",
          "koennte", "ihm", "allen", "allein", "erstmals", "neuer", "ebenfalls", "erklaert", "stehen", "ihn", "wer",
          "sehr", "kommt", "gebracht", "besonders", "wollten", "wollte", "meinte", "gut", "ganz", "gilt", "neues",
          "hier", "ging", "konnte", "mal", "denen", "waere", "zudem", "hoehere", "zufolge", "seines", "einigen",
          "klar", "einige", "bekommen", "duerfen", "etwas", "hin", "solche", "dessen", "halten", "ihres", "denn",
          "bringen", "deren", "ums", "wo", "nachdem", "mehreren", "alles", "hinter", "darf", "dadurch", "haette",
          "daran", "macht", "besser", "neben", "musste", "stehe", "komme", "davor", "kamen", "vermutlich", "nahm",
          "tun", "sehen", "genommen", "erster", "falls", "jeder", "vielen", "uns", "konnten", "machte", "kaum", "sogar",
          "deinen", "was", "für", "über", "also", "worden", "müssen", "dürfen", "gern", "viel", "wäre", "froh", "können", "welche")
}
