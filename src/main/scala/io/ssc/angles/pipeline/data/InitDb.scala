package io.ssc.angles.pipeline.data

import scala.collection.immutable.StringOps

/**
 * Created by niklas on 06.02.15.
 */
object InitDb extends App {
  val screennames = new StringOps("""digiges
frank_rieger
netzpolitik
saschalobo
akvorrat
annalist
anked
martindelius
senficon
wetterfrosch
indeededly
maltespitz
Tikkachu
janl

KatholikenNet
jep_
SteinbachErika
dieAlbsteigerin
kritikkultur
jensspahn
jalenz

ideade
Beatrix_vStorch
Familienschutz_

sls_fdp

dmbmieterbund

afelia
newkitz

NachDenkSeiten
attacd
derfreitag
lobbycontrol
clauskleber
augstein
inespohl
soniamikich
mediummagazin

correctiv_org
danieldrepper
jsachse
nrecherche
themaastrix
wazrecherche

SWagenknecht
gregorgysi
halina_waw
mdb_stroebele

berlinvegan
eikes
SeeroiberJenny
wastun_jetzt
redaktionmerkur
theorieblog
duhastdiemacht
Lateinamerika
amerika21
FelSBerlin
ai_b_bbg
andrejholm
antifaberlin
indy_linksunten
kpfrankfurt
kritikol
ohlauerinfo
rwc_hh
wohnraum_hh
annskaja""")

  val twitterApi = TwitterApi.connect()
  for (screenname <- screennames.lines) {
    if (screenname != "") {
      val user = twitterApi.showUser(screenname)
      Storage.saveExplorer(user)
    }
  }
}
