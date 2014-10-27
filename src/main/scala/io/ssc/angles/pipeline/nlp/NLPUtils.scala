package io.ssc.angles.pipeline.nlp

import java.io.StringReader

import de.l3s.boilerpipe.extractors.ArticleExtractor
//import io.ssc.angles.pipeline.nlp.Loglikelihood
import org.apache.lucene.analysis.de.GermanAnalyzer
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.util.Version

import scala.collection.mutable

object NLPUtils extends App {

  def extractArticle(html: String) = {
    ArticleExtractor.getInstance().getText(html)
  }

  /*
  //TODO add another version for fingerprinting that uses stemming
  //TODO close reader
  def displayableInterestingBigrams(text: String, howMany: Int) = {

    val gramCounts = mutable.Map[String, Int]()
    val bigramCounts = mutable.Map[String, Int]()

    val reader = new StringReader(text)
    val analyzer = new ShingleAnalyzerWrapper(new StandardAnalyzer(Version.LUCENE_42, German.stopSet), 2, 2)

    val stream = analyzer.tokenStream(null, reader)
    stream.reset()

    while (stream.incrementToken()) {

      val bigram = stream.getAttribute(classOf[CharTermAttribute]).toString
      val terms = bigram.split(" ")

      if (terms.length > 1 && terms(0) != "_" && terms(1) != "_") {
        incrementCount(bigramCounts, bigram)
        incrementCount(gramCounts, terms(0))
        incrementCount(gramCounts, terms(1))
      }
    }

    val order = Ordering.fromLessThan[(String, Double)]({ case ((_, countA), (_, countB)) => {
      countA > countB
    }})


    val queue = new mutable.PriorityQueue[(String,Double)]()(order)

    val overall = gramCounts.values.sum

    for ((bigram, count) <- bigramCounts) {

      val terms = bigram.split(" ")
      val term1Count = gramCounts(terms(0))
      val term2Count = gramCounts(terms(1))


      //http://tdunning.blogspot.com/2008/03/surprise-and-coincidence.html
      val k_11 = count
      val k_12 = term2Count - count
      val k_21 = term1Count - count
      val k_22 = overall - term1Count - term2Count + count

      val llrScore = Loglikelihood.logLikelihoodRatio(k_11.toLong, k_12.toLong, k_21.toLong, k_22.toLong)
//      println(bigram + ":" + count + " " + term1Count + ", " + term2Count + " => " + llrScore)

      val candidate = bigram -> llrScore

        if (queue.size < howMany) {
          queue.enqueue(candidate)
        } else {
          if (order.lt(candidate, queue.head)) {
            queue.dequeue()
            queue.enqueue(candidate)
          }
        }

      //for ((similarItem, similarity) <- queue.dequeueAll)


    }

    queue.dequeueAll map { case (bigram, score) => bigram }
  }*/

  def incrementCount(counts: mutable.Map[String, Int], key: String) = {
    val newCount = counts.getOrElse(key, 0) + 1
    counts(key) = newCount
  }

  def toGermanStemmedTerms(text: String) = {

    val analyzer = new GermanAnalyzer(Version.LUCENE_42, German.stopSet())

    val reader = new StringReader(text)

    val stream = analyzer.tokenStream(null, reader)
    stream.reset()

    val terms = mutable.ListBuffer[String]()

    while (stream.incrementToken()) {
      val term = stream.getAttribute(classOf[CharTermAttribute]).toString
      terms.append(term)
    }

    terms.toList
  }



/*
  displayableInterestingBigrams("""
  ist Dozent der Bartlett School of Planning am University College London  (UCL). Er schreibt in seinem Blog michaeledwards.org.uk und arbeitet in  der Gruppe Just Space für ein anderes London.
  Ist das eine Besonderheit von Newham oder typisch für Labour-Politik?
    Es ist ein extremer Fall, aber die Tendenz geht in anderen Labour-regierten Bezirken in dieselbe Richtung, etwa im benachbarten Hackney oder in Greenwich. Der Labour-Stadtrat von Tower Hamlets, ebenfalls im East End, versucht dagegen den Bau von Sozialwohnungen voranzutreiben.
    Wir müssen an dieser Stelle über das britische Mietrecht reden. Ist es richtig, dass es keine Mietpreiskontrolle gibt und die einzige Chance für Ärmere ist, eine Sozialwohnung zu bekommen?
  Ja, das ist so, seit in den 1980er Jahren Margaret Thatcher sämtliche staatliche Mietpreisregeln aufgehoben hat. Noch dazu gibt es wenig Sicherheit. Die meisten Mietverträge mit privaten Vermietern laufen über sechs Monate. Wenn der Vermieter die Mieter danach draußen haben will, müssen sie sich etwas Neues suchen. Ein großer Teil der unteren und mittleren Einkommensgruppen würde gern städtische oder Genossenschaftswohnungen mieten, aber dafür gibt es lange Wartelisten. Es kann durchaus zehn oder fünfzehn Jahre dauern, bis man drankommt.
    Die andere Möglichkeit wäre zu kaufen.
  Wenn man das Geld dazu hat. In Großbritannien liegt der Durchschnittspreis für Immobilien fünfmal so hoch wie das jährliche Familieneinkommen. Das kann man sich als Normalverdiener nicht leisten. Und in London ist das Problem noch größer. Der Anteil der Briten, die Wohneigentum besitzen, ist daher seit Ausbruch der Finanzkrise deutlich gesunken.
  Wohin zieht denn der ärmere Teil der Bevölkerung, wenn er sich das East End nicht mehr leisten kann – einfach weiter nach Osten, nach Barking und Dagenham?
  Zum Teil. Manche verlassen London auch ganz. Andere leben in überfüllten Wohnungen. Es gibt sogar vereinzelte Fälle, wo sich Bewohner die Betten teilen: Der eine schläft nachts, der andere tagsüber …
  … wie die sogenannten Schlafburschen in Berlin zu Zeiten der Industrialisierung …
  … ja, die Scheußlichkeiten des 19. Jahrhunderts kehren zurück. Dann dann haben wir im Osten und anderen Teilen Londons illegale Bautätigkeiten. Viele Häuser haben hier einen Garten. Dort werden dann kleine Gebäude errichtet – oder aber Garagen zu Wohnraum umgebaut und meist an Immigranten vermietet.
    Sie haben noch bei Ruth Glass studiert …
  Ich hatte das Glück, bei ihr Soziologie zu lernen. Sie kam ja aus Berlin, war als Jüdin vor den Nazis nach London geflohen.
    Glass hat 1963 den Begriff „Gentrification“ erfunden, um die Umwandlung von Londoner Arbeitervierteln zu Mittelschichtsquartieren zu beschreiben. Jetzt gibt es zwar seit 50 Jahren Diskussionen über dieses Phänomen, dennoch ist in London ein Stadtviertel nach dem anderen gentrifiziert worden. Gibt es irgendwelche Grenzen?
  Es gab zumindest eine kurze Zeit in den 1970er Jahren, als einige Londoner Bezirksverwaltungen Häuser, die auf dem Wohnungsmarkt angeboten wurden, aufgekauft und dort Sozialwohnungen zur Verfügung gestellt haben, besonders in Camden und Islington – also den Vierteln, die Ruth Glass untersucht hat. Das hat Gentrifizierungsprozesse gebremst oder sogar umgekehrt. Bis heute sind in einigen Straßenzügen die positiven Effekte der damaligen Politik zu spüren. Aber dann kam Thatcher, seitdem schreitet die Gentrifizierung Londons unaufhaltsam voran. Gerade jetzt wird sie wegen der Spekulation auf dem Immobilienmarkt immer extremer. Besonders Häuser im Luxussegment werden von internationalen Investoren vermehrt aufgekauft, als seien sie Goldbarren, also als reine Vermögensanlage. Oft sind die Käufer nicht einmal daran interessiert, die Wohnungen zu vermieten. Die Gentrifizierung ist völlig außer Kontrolle geraten.
    In London hat nur eine Handvoll gegen die Spiele protestiert, in Brasilien gab es jetzt große Demonstrationen gegen die Fußball-WM und Olympia. Haben Sie mit Neid nach Rio de Janeiro geschaut?
    Viele Londoner Bewohner sind von der Wohnungssituation extrem genervt; derzeit organisieren sich immer mehr Mieter, um längere Verträge oder niedrigere Mieten zu fordern. Es gibt Demonstrationen und andere Aktionen. Das wird zwar nicht wie in Brasilien – obwohl, man weiß ja nie. Zumindest möchte ich nicht, dass Sie den Eindruck gewinnen, hier gäbe es überhaupt keinen Widerstand gegen die Wohnungspolitik.
   """, 3)*/



//http://homepages.inf.ed.ac.uk/pkoehn/publications/de-news/
//cp *.de.*txt de/
//cd de
//mkdir de-cleaned
//for file in `ls *.de.txt`; do cat $file|grep -v "^<" >> de-cleaned/news.txt; done



}