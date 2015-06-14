/**
 * Angles
 * Copyright (C) 2015 Jakob Hendeß, Niklas Wolber
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
package io.ssc.angles.pipeline.data

import scala.collection.immutable.StringOps

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
