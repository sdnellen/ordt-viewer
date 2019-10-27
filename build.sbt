name := """ordt-viewer"""
organization := "com.example"

version := "181112-1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.8"
//scalacOptions += "-feature"

// use node for js compile
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

libraryDependencies += javaJpa
libraryDependencies += "org.hibernate" % "hibernate-core" % "5.2.5.Final"
libraryDependencies += evolutions

// these and jquery are added 
libraryDependencies += filters
libraryDependencies += "org.webjars" % "bootstrap" % "3.3.6"
libraryDependencies += "org.webjars" % "jquery" % "2.2.3"

// workaround for JPA issue on lnx in play 2.4 ** can keep false for dist 2.5?? **
PlayKeys.externalizeResources := false

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in Compile)

EclipseKeys.projectFlavor := EclipseProjectFlavor.Java           // Java project. Don't expect Scala IDE
EclipseKeys.createSrc := EclipseCreateSrc.ValueSet(EclipseCreateSrc.ManagedClasses, EclipseCreateSrc.ManagedResources)  // Use .class files instead of generated .scala files for views and routes

// added 9/17 for jar gen using sbt assembly
//import AssemblyKeys._
//assemblySettings
//mainClass in assembly := Some("play.core.server.ProdServerStart")
//fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)