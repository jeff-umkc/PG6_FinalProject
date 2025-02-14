package edu.umkc.ic

import java.io.File

import org.apache.log4j.{Level, Logger}
import org.apache.spark.Logging

import scala.collection.mutable.HashMap
import scala.io.Source
import scala.sys.process.stringSeqToProcess

/**
 * Created by Jeff Lanning on 7/11/15.
 */
object PG6_ScalaHelper extends Logging {
  Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
  Logger.getLogger("org.apache.spark.streaming.NetworkInputTracker").setLevel(Level.INFO)

  /** Configures the Oauth Credentials for accessing Twitter */
  def configureTwitterCredentials() {
    val file = new File("../twitter.txt")
    if (!file.exists) {
      throw new Exception("Could not find configuration file " + file)
    }
    val lines = Source.fromFile(file.toString).getLines.filter(_.trim.size > 0).toSeq
    val pairs = lines.map(line => {
      val splits = line.split("=")
      if (splits.size != 2) {
        throw new Exception("Error parsing configuration file - incorrectly formatted line [" + line + "]")
      }
      (splits(0).trim(), splits(1).trim())
    })
    val map = new HashMap[String, String] ++= pairs
    val configKeys = Seq("consumerKey", "consumerSecret", "accessToken", "accessTokenSecret")
    println("Configuring Twitter OAuth")
    configKeys.foreach(key => {
      if (!map.contains(key)) {
        throw new Exception("Error setting OAuth authenticaion - value for " + key + " not found")
      }
      val fullKey = "twitter4j.oauth." + key
      System.setProperty(fullKey, map(key))
      println("\tProperty " + fullKey + " set as " + map(key))
    })
    println()
  }

  /** Returns the Spark URL */
  def getSparkUrl(): String = {
    val file = new File("/root/spark-ec2/cluster-url")
    if (file.exists) {
      val url = Source.fromFile(file.toString).getLines.toSeq.head
      url
    } else if (new File("../local").exists) {
      "local[4]"
    } else {
      throw new Exception("Could not find " + file)
    }
  }

  /** Returns the HDFS URL */
  def getHdfsUrl(): String = {
    try {
      val name: String = Seq("bash", "-c", "curl -s http://169.254.169.254/latest/meta-data/hostname") !!;
      println("Hostname = " + name)
      "hdfs://" + name.trim + ":9000"
    } catch {
      case e: Exception => {
        if (new File("../local").exists) {
          "."
        } else {
          throw e
        }
      }
    }
  }

  /** Set reasonable logging levels for streaming if the user has not configured log4j. */
  def setStreamingLogLevels() {
    val log4jInitialized = Logger.getRootLogger.getAllAppenders.hasMoreElements
    if (!log4jInitialized) {
      // We first log something to initialize Spark's default logging, then we override the
      // logging level.
      logInfo("Setting log level to [WARN] for streaming example." +
        " To override add a custom log4j.properties to the classpath.")
      Logger.getRootLogger.setLevel(Level.WARN)
    }
  }
}
