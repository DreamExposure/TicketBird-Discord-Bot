-- MySQL dump 10.13  Distrib 5.7.26, for Linux (x86_64)
--
-- Host: host    Database: ticketbird
-- ------------------------------------------------------
-- Server version	redacted

/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE = @@TIME_ZONE */;
/*!40103 SET TIME_ZONE = '+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS = @@UNIQUE_CHECKS, UNIQUE_CHECKS = 0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0 */;
/*!40101 SET @OLD_SQL_MODE = @@SQL_MODE, SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES = @@SQL_NOTES, SQL_NOTES = 0 */;

--
-- Table structure for table `${prefix}api`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `${prefix}api`
(
    `USER_ID`     varchar(255) NOT NULL,
    `API_KEY`     varchar(64)  NOT NULL,
    `BLOCKED`     tinyint(1)   NOT NULL,
    `TIME_ISSUED` mediumtext   NOT NULL,
    `USES`        int(11)      NOT NULL,
    PRIMARY KEY (`USER_ID`, `API_KEY`)
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `${prefix}guild_settings`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `t${prefix}guild_settings`
(
    `GUILD_ID`           varchar(255) NOT NULL,
    `LANG`               varchar(255) NOT NULL,
    `PREFIX`             varchar(16)  NOT NULL,
    `PATRON_GUILD`       tinyint(1)   NOT NULL,
    `DEV_GUILD`          tinyint(1)   NOT NULL,
    `AWAITING_CATEGORY`  mediumtext   NOT NULL,
    `RESPONDED_CATEGORY` mediumtext   NOT NULL,
    `HOLD_CATEGORY`      mediumtext   NOT NULL,
    `CLOSE_CATEGORY`     mediumtext   NOT NULL,
    `SUPPORT_CHANNEL`    mediumtext   NOT NULL,
    `NEXT_ID`            int(11)      NOT NULL,
    `STAFF`              longtext     NOT NULL,
    `STATIC_MESSAGE`     mediumtext   NOT NULL,
    `CLOSED_TOTAL`       int(11)      NOT NULL DEFAULT '0',
    PRIMARY KEY (`GUILD_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `${prefix}projects`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `${prefix}projects`
(
    `GUILD_ID`       varchar(255) NOT NULL,
    `PROJECT_NAME`   longtext     NOT NULL,
    `PROJECT_PREFIX` varchar(16)  NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `${prefix}tickets`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `${prefix}tickets`
(
    `GUILD_ID`      varchar(255) NOT NULL,
    `NUMBER`        int(11)      NOT NULL,
    `PROJECT`       longtext     NOT NULL,
    `CREATOR`       mediumtext   NOT NULL,
    `CHANNEL`       mediumtext   NOT NULL,
    `CATEGORY`      mediumtext   NOT NULL,
    `LAST_ACTIVITY` mediumtext   NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE = @OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE = @OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS = @OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES = @OLD_SQL_NOTES */;

-- Dump completed on 2019-06-23  4:24:08
