-- MySQL dump 10.14  Distrib 5.5.41-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: wangle
-- ------------------------------------------------------
-- Server version	5.5.41-MariaDB-1~trusty-log

CREATE DATABASE wangle;
USE wangle;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `candidates`
--

DROP TABLE IF EXISTS `candidates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `candidates` (
  `website_id` int(11) NOT NULL,
  UNIQUE KEY `uq_c_website_id` (`website_id`),
  CONSTRAINT `fk_c_website_id` FOREIGN KEY (`website_id`) REFERENCES `crawled_websites` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `candidates`
--

LOCK TABLES `candidates` WRITE;
/*!40000 ALTER TABLE `candidates` DISABLE KEYS */;
/*!40000 ALTER TABLE `candidates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `crawled_website_metadata`
--

DROP TABLE IF EXISTS `crawled_website_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `crawled_website_metadata` (
  `crawled_website_id` int(11) NOT NULL,
  `key` varchar(255) NOT NULL,
  `value` mediumtext NOT NULL,
  KEY `fk_cwm_` (`crawled_website_id`),
  CONSTRAINT `fk_cwm_` FOREIGN KEY (`crawled_website_id`) REFERENCES `crawled_websites` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `crawled_website_metadata`
--

LOCK TABLES `crawled_website_metadata` WRITE;
/*!40000 ALTER TABLE `crawled_website_metadata` DISABLE KEYS */;
/*!40000 ALTER TABLE `crawled_website_metadata` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `crawled_website_named_entities`
--

DROP TABLE IF EXISTS `crawled_website_named_entities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `crawled_website_named_entities` (
  `name` text NOT NULL,
  `entity_type` varchar(45) NOT NULL,
  `website_id` int(11) NOT NULL,
  `count` int(11) NOT NULL,
  KEY `fk_cwne_website_id` (`website_id`),
  CONSTRAINT `fk_cwne_website_id` FOREIGN KEY (`website_id`) REFERENCES `crawled_websites` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `crawled_website_named_entities`
--

LOCK TABLES `crawled_website_named_entities` WRITE;
/*!40000 ALTER TABLE `crawled_website_named_entities` DISABLE KEYS */;
/*!40000 ALTER TABLE `crawled_website_named_entities` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `crawled_websites`
--

DROP TABLE IF EXISTS `crawled_websites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `crawled_websites` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uri` text NOT NULL,
  `real_uri` text NOT NULL,
  `tweet_id` bigint(20) NOT NULL,
  `fetch_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `charset` varchar(25) NOT NULL,
  `html` longtext NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cw_tweet_id` (`tweet_id`),
  CONSTRAINT `fk_cw_tweet_id` FOREIGN KEY (`tweet_id`) REFERENCES `tweets` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=5637 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `crawled_websites`
--

LOCK TABLES `crawled_websites` WRITE;
/*!40000 ALTER TABLE `crawled_websites` DISABLE KEYS */;
/*!40000 ALTER TABLE `crawled_websites` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `explorers`
--

DROP TABLE IF EXISTS `explorers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `explorers` (
  `id` bigint(20) NOT NULL,
  `screenname` varchar(25) NOT NULL,
  `name` varchar(45) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `screenname_UNIQUE` (`screenname`),
  UNIQUE KEY `name_UNIQUE` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `explorers`
--

LOCK TABLES `explorers` WRITE;
/*!40000 ALTER TABLE `explorers` DISABLE KEYS */;
INSERT INTO `explorers` VALUES (819606,'janl','Jan Lehnardt','Dissatisfied with the status-quo.\n\n@couchdb • @hoodiehq • @jsconfeu\n\nDuct tape artist and feminist.\n\nMay the bridges we burn light the way.'),(5876652,'saschalobo','Sascha Lobo','Autor, Internet. \r\nImpressum: http://t.co/CIJeOQtd'),(9655032,'netzpolitik','netzpolitik','Markus Beckedahl, http://t.co/3nYEqSELVL,'),(10735672,'wetterfrosch','??????','Living on diaspora: wetter@diaspora.subsignal.org'),(13595222,'akvorrat','AK Vorrat','Zusammenschluss von Bürgerrechtler.innen, Datenschützer.innen & Internet-Nutzer.innen und kämpft gegen Überwachung und Vorratsdatenspeicherungen'),(14613514,'derfreitag','der Freitag','Hier twittern Jan Jasper Kosok #jjk & Juliane Löffler #jl. Beiträge aus der Freitag-Community werden mit #blog getaggt.'),(14618956,'maltespitz','Malte Spitz','Bundesparteirat, Landesvorstand NRW und  Sprecher BAG Medien und Netzpolitik Bündnis 90/Die Grünen, Autor, TED Speaker, Themen: Medien-, Bürgerrechts- und Netzp'),(14619744,'Augstein','Augstein','Journalist'),(14861745,'Senficon','Julia Reda','MEP for @Piratenpartei and the European Pirate Party (PPEU) | President of @YoungPiratesEU | Vice-Chair of @GreensEP group'),(15364897,'BerlinVegan','Berlin-Vegan','Nachrichten zu Veganismus und Tierrechten in Berlin und weltweit. Abonniert den Tierrechtskalender: http://t.co/cvUGsdtmGq'),(15520241,'annalist','Anne Roth','Innenpolitik, Netzpolitik, Medien & Feminismus / Schreibt ins Netz seit 1999 / Referentin in Sachen NSA / English Tweets @Anne_Roth #NSAUA'),(16219122,'eikes','Eike.Se/nd','Nerd'),(16557497,'anked','anke domscheit-berg','transparency + open government activist, Eastern Germany-socialized feminist, Book author, broadband entrepreneur'),(16569660,'Afelia','Marina Weisband','Meinung siehe Umfragen.'),(16837284,'themaastrix','Marco Maas','Netzversteher, Teilzeit-Nerd. #ddj, #socialtv #lügenpresse, macht mit bei @opendatacity, @venturemedia, @fotografirma @viergleicheins, @ddjhh, @codeforhh'),(18717137,'jep_','Jan-Eric Peters','Chefredakteur WELT-Gruppe'),(18866407,'Halina_Waw','Halina Wawzyniak','Halina Wawzyniak ist MdB, rechts- und netzpolitische Sprecherin der @linksfraktion im Deutschen Bundestag. (#buerotweet von den Mitarbeitern)'),(18942977,'mediummagazin','medium magazin','Das Magazin für Journalisten. Hier twittern: @JensTwiehaus /twi, @haemanns /aha, @AnMilz /ami, @TStrothjohann /ts, @ulrikelanger /ula'),(20582214,'danieldrepper','Daniel Drepper','co-#founder & senior #reporter @correctiv_org / freedom of information / @fussballdoping / #investigative & #innovation grad @columbiajourn / PGP 6EF26405'),(21385327,'FelSBerlin','FelS','Berliner Gruppe, die sich seit 1991 mit der Weiterentwicklung linksradikaler Politik beschäftigt.'),(23576906,'jalenz','Lenz Jacobsen','Politikredakteur @zeitonline (Türkei/Meta/Protest/AfD/Linke) :::\nmüziksiz bir hayat hatad?r'),(26044503,'Attacd','Attac Deutschland','Aktionsinfos vom globalisierungskritischen Netzwerk Attac in Deutschland'),(29954030,'jsachse','Jonathan Sachse','Reporter @correctiv_org\n\nPGP: 00912CB2 /\njonathan.sachse@correctiv.org\n\nhttp://t.co/mPE6Z6Ly6a & @fussballdoping'),(35440667,'Lateinamerika','Lateinamerika XXI','Aktuelle Informationen über Lateinamerika. In Partnerschaft mit http://t.co/8aBru13V51'),(38444282,'lobbycontrol','LobbyControl','LobbyControl klärt auf über Lobbying, PR-Kampagnen und Denkfabriken.'),(38451030,'inespohl','Ines Pohl','Nieman Fellow \'05,                                                                             taz Chefredakteurin'),(39463907,'NachDenkSeiten','NachDenkSeiten',''),(44619712,'ideade','idea.de','Evangelische Nachrichtenagentur'),(47375691,'SWagenknecht','Sahra Wagenknecht','Mitglied des Deutschen Bundestages - Erste Stellvertretende Vorsitzende der Linksfraktion'),(48668233,'Tikkachu','Cornelia Otto','People say I\'m an idealist. But sometimes I\'m not sure if that\'s meant as a compliment...'),(51103685,'AndrejHolm','Andrej Holm','Stadtsoziologe und Aktivist, blogge gelegentlich zu Gentrification und Anti-Gentrification-Protesten'),(53016156,'frank_rieger','Frank Rieger','Bio: http://t.co/WI7rI0TCFc Bücher: http://t.co/037TY3Dexs, http://t.co/V6vl93RNfW'),(66651957,'sls_fdp','S.L.-Schnarrenberger',''),(77600247,'amerika21','amerika21','Nachrichten und Analysen aus Lateinamerika und der Karibik'),(103913586,'kpfrankfurt','kritik&praxis ffm','Kritik&Praxis - radikale Linke [f]frankfurt Bei twitter'),(111706879,'AntifaBerlin','Antifa Linke Berlin','Antifaschistische und antikapitalistische Gruppe aus Berlin. Besteht seit 2003, macht regelmäßig Kampagnen und legt wert auf Bündnisarbeit.'),(112933148,'KritiKol','IL Rhein-Neckar','IL Rhein-Neckar (Kritisches Kollektiv). Kritik und kreativer Protest im Südwesten seit 2003. Jeden 3. Sonntag Volxküche im ASV Mannheim'),(114757243,'indy_linksunten','Indymedia linksunten','Indymedia ist ein dezentral organisiertes, weltweites Netzwerk sozialer Bewegungen.'),(119345403,'theorieblog','Theorieblog','Das Theorieblog wurde Anfang 2010 als Forum für politische Theorie, Philosophie und Ideengeschichte ins Leben gerufen.'),(125955877,'kritikkultur','Claus Junghanns','Blogger - Bürger - Unionist. Büronachbar von @petertauber. Motto: Freiheit ist, Politik so zu betreiben, dass man diese auch einmal hinter sich lassen kann.'),(145180684,'SeeroiberJenny','Anne Helm','Flucht nach vorne! Und immer auf die Mitte zielen! - politisch motiviert - Synchronschauspielerin, Kommunalpolitikerin, Sängerin, Antirassistin'),(205317030,'wohnraum_hh','Mietenwahnsinn.de','Wir berichten über Entwicklungen rund um Leerstand, steigende Mieten und den Kämpfen für ein Recht auf Stadt in Hamburg.'),(227065093,'WAZRecherche','WAZ Recherche','#Recherche-Team der #WAZ. Blog: http://t.co/ZuG3si8NWg Verschlüsselter Upload (Dokumente & Nachrichten): http://t.co/rIQ2S5Bi1w'),(250051006,'KatholikenNet','KATH.NET','Hier twittert Roland Noé, Herausgeber der katholischen Internetzeitung http://t.co/f3ealOZj'),(268464220,'digiges','DigitaleGesellschaft','Wir wollen eine offene und freie digitale Gesellschaft erhalten und mitgestalten. Dazu brauchen wir Dich, Dein Wissen und Dein Engagement.'),(279412211,'martindelius','Martin Delius','Antifaschist und „rotes Tuch“ für Hartmut M. (foto: @stadhold)'),(299650387,'jensspahn','Jens Spahn','Mitglied des Deutschen Bundestages (CDU) Jahrgang 1980'),(304342650,'MdB_Stroebele','Christian Ströbele','Hier twittern Hans-Christian Ströbele und sein Team [T] | MdB der Fraktion Bündnis90/DieGrünen'),(425845268,'SteinbachErika','Erika Steinbach','Bundestagsabgeordnete und Privatperson. Geigerin, Informatikerin, Diplomverwaltungswirtin. Neigungen: Lyrik, gute Musik und Natur'),(429357796,'dieAlbsteigerin','Katrin Albsteiger','Schwäbin. Bayerin. MdB. CSU. JU.'),(466681587,'indeededly','Martin Schmetz','Sicherheitspolitik-blog. International relations, political science, cybersecurity, east asia, the internets.'),(482977620,'newkitz','LuisaMaria Schweizer','program coordinator @HIAgermany - chairwoman @Euroalter Berlin -  the new Kitz on the block'),(508915165,'DMBMieterbund','Deutscher Mieterbund','Hier twittern für den Deutschen Mieterbund: Lukas Siebenkotten (ls), Ulrich Ropertz (ur), Heike Keilhofer (kei) und Jürgen Schoo (js)'),(545887340,'SoniaMikich','Sonia Mikich','Journalistin WDR, deutsch-englisch-jugoslawisch Mischmasch. Hier persönliche Ansichten'),(588259039,'nrecherche','netzwerk recherche','Journalistenvereinigung netzwerk recherche e.V. – German Association of Investigative Journalists info@netzwerkrecherche.de'),(805308596,'Beatrix_vStorch','Beatrix von Storch','Kandidatin für das EU-Parlament 2014 • Alternative für Deutschland'),(867711740,'redaktionmerkur','Redaktion Merkur','Redaktion des Merkur, der deutschen Zeitschrift für europäisches Denken. (Es twittert @knoerer)'),(888289790,'GregorGysi','Gregor Gysi','Fraktionsvorsitzender @linksfraktion, direkt gewählt in Berlin Treptow-Köpenick\r\nhttps://t.co/yHX4y27QMT'),(1055071543,'annskaja','annskaja','yesterday the future seemed brighter - comments on the net, politics, netpolitics and hippos'),(1367254122,'ClausKleber','Claus Kleber','Moderator des @heutejournal - Was ich hier schreibe geht auf meine Kappe!'),(1645952918,'ai_b_bbg','AntiRa Infoportal','Antirassistisches Infoportal Berlin-Brandenburg // Informationen über Rassismus und Ausgrenzung in Berlin. Kontakt: antirassistischeinfos@riseup.net'),(2301638324,'correctiv_org','CORRECT!V','Wir sind das erste gemeinnützige Recherchebüro im deutschsprachigen Raum. \n\nWeitere CORRECT!V-Kanäle in sozialen Medien: https://t.co/l5dFDJSkZd'),(2369878637,'Familienschutz_','Familienschutz','Initiative Familienschutz'),(2376031538,'wastun_jetzt','#wastun','#wastun gegen die völlige digitale Überwachung. Beteiligt euch!'),(2474502649,'RWC_HH','RefugeeWelcomeCenter','Lampedusa in Hamburg House : Refugee Welcome Center Laeiszstrasse 12'),(2575032920,'duhastdiemacht','(ex) DuHastDieMacht','ACHTUNG! @DuHastDieMacht hat sich umbenannt und ist ab sofort unter @_meshcollective zu erreichen!!'),(2589568548,'OhlauerInfo','Ohlauer Infopoint','Ohlauer Infopoint - News bzgl der Räumung // Infotelefon: 0157 58376788');
/*!40000 ALTER TABLE `explorers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tweets`
--

DROP TABLE IF EXISTS `tweets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tweets` (
  `id` bigint(20) NOT NULL,
  `explorer_id` bigint(20) NOT NULL,
  `creation_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `fetch_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `json` longtext NOT NULL,
  `parent_tweet` bigint(20) DEFAULT -1,
  `follow_retweets` bit(2) DEFAULT b'0'  COMMENT '0 = unprocessed\n1 = follow\n2 = followed\n3 = uninteresting',
  `retweet_count` int(7) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tweets`
--

LOCK TABLES `tweets` WRITE;
/*!40000 ALTER TABLE `tweets` DISABLE KEYS */;
/*!40000 ALTER TABLE `tweets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `website_labels`
--

DROP TABLE IF EXISTS `website_labels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `website_labels` (
  `uri` text NOT NULL,
  `label` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `website_labels`
--

LOCK TABLES `website_labels` WRITE;
/*!40000 ALTER TABLE `website_labels` DISABLE KEYS */;
/*!40000 ALTER TABLE `website_labels` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

SET GLOBAL wait_timeout = 1000;
