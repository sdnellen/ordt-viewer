name := """ordt-viewer"""
organization := "com.example"

version := "191226-1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.8"
//scalacOptions += "-feature"

// use node for js compile
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

libraryDependencies += javaJpa

// 5.3.6 uses dom4j 1.6.1, 5.3.7 uses 2.1.1
//libraryDependencies += "org.hibernate" % "hibernate-core" % "5.3.7.Final"
//libraryDependencies += "org.hibernate" % "hibernate-core" % "5.2.5.Final"

// trying this per play 2.5 docs - 12/26/19
libraryDependencies +=   "org.hibernate" % "hibernate-entitymanager" % "5.1.0.Final"

// adding to fix xml load issue 11/2019
//libraryDependencies += "org.w3c" % "dom" % "2.3.0-jaxb-1.0.6"
//libraryDependencies += "dom4j" % "dom4j" % "1.6.1"

libraryDependencies += evolutions

// these and jquery are added 
libraryDependencies += filters
libraryDependencies += "org.webjars" % "bootstrap" % "3.3.6"
libraryDependencies += "org.webjars" % "jquery" % "2.2.3"

// workaround for JPA issue on lnx in play 2.4 ** can keep false for dist 2.5?? **
PlayKeys.externalizeResources := false


// added 9/17 for jar gen using sbt assembly
//import AssemblyKeys._
//assemblySettings
//mainClass in assembly := Some("play.core.server.ProdServerStart")
//fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)
