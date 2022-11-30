name := """registration"""
organization := "com.moviepass"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.8"

resolvers += Resolver.jcenterRepo
ThisBuild / resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"
resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += Resolver.sonatypeRepo("public")

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
val playJsonDerivedCodecs = "org.julienrf" %% "play-json-derived-codecs" % "7.0.0"

val slickPgVersion = "0.19.0"
val slickPg = "com.github.tminglei" %% "slick-pg" % slickPgVersion
val slickPgPlayJson = "com.github.tminglei" %% "slick-pg_play-json" % slickPgVersion
val slickPgJTS = "com.github.tminglei" %% "slick-pg_jts_lt" % slickPgVersion
val postgres = "org.postgresql" % "postgresql" % "42.2.13"

val macwireDeps = Seq(
    "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided",
    "com.softwaremill.macwire" %% "util" % "2.3.3"
  )

val slickDeps = Seq(
    "com.typesafe.slick" %% "slick" % "3.3.2", 
    //"org.slf4j" % "slf4j-simple" % "1.7.30", 
    "com.typesafe.slick" %% "slick-hikaricp" % "3.3.2",
    "com.typesafe.play" %% "play-slick" % "5.0.0",
    "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0"
    )
//Resolvers

libraryDependencies ++= Seq(ws, jdbc, evolutions)
libraryDependencies ++= Seq(playJsonDerivedCodecs, slickPg, slickPgPlayJson, slickPgJTS, postgres) 
libraryDependencies ++= slickDeps
libraryDependencies ++= macwireDeps
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies += "org.webjars" % "bootstrap" % "4.4.1-1"
libraryDependencies += "org.webjars" %% "webjars-play" % "2.8.0"
libraryDependencies += "com.mashape.unirest" % "unirest-java" % "1.4.9"
libraryDependencies += "commons-codec" % "commons-codec" % "1.13"
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.3.0"
libraryDependencies += "com.github.t3hnar" %% "scala-bcrypt" % "4.3.0"
libraryDependencies += "com.pauldijou" %% "jwt-play" % "4.2.0"
libraryDependencies += "com.asana" % "asana" % "0.10.4"
libraryDependencies += "com.github.seratch" %% "awscala" % "0.9.+"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.stripe" % "stripe-java" % "21.0.0"
libraryDependencies += "com.segment.analytics.java" % "analytics" % "3.2.0"


console / initialCommands := """

  import com.softwaremill.macwire._
  import com.typesafe.config._
  import controllers._
  import api._
  import models._
  import modules._
  import persistence._
  //import utils._
  import java.time._
  import play.api._
  import play.api.{Mode, ApplicationLoader, BuiltInComponentsFromContext}
  import play.api.ApplicationLoader._
  import play.api.ApplicationLoader.Context
  import play.api.db.Database
  import play.api.db.DBComponents
  import play.api.i18n._
  import play.api.libs.ws._
  import play.api.libs.ws.ahc._
  import play.api.mvc._
  import play.api.routing.Router
  import play.api.libs.json.Json
  import play.filters.HttpFiltersComponents
  import router.Routes
  import scala.collection.immutable
  import scala.concurrent._
  import scala.concurrent.duration._
  import services._
  import scala.language.postfixOps
  
  val env     = Environment(new java.io.File("."), this.getClass.getClassLoader, Mode.Dev)
  val context = ApplicationLoader.Context.create(env)
  val loader  = ApplicationLoader(context)
  val app     = loader.load(context)
  Play.start(app)
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  val consoleApp = utils.ConsoleApp(app, context)
  println("Example: consoleApp.someService.doSomething()")

"""
console / cleanupCommands += """
  Play.stop(app)
"""