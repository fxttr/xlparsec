scalaVersion := "2.12.20"

name := "xlparsec"
organization := "de.fxttr.scala"
version := "1.0"

libraryDependencies ++= Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0",

    // Json
    "io.circe" %% "circe-core" % "0.15.0-M1",
    "io.circe" %% "circe-parser" % "0.15.0-M1",
    "io.circe" %% "circe-generic" % "0.15.0-M1",

    // Reading XLSX
    "org.apache.poi" % "poi-ooxml" % "5.3.0",

    // Spark
    "org.apache.spark" %% "spark-core" % "3.5.2",
    "org.apache.spark" %% "spark-sql" % "3.5.2",

    // Cats
    "org.typelevel" %% "cats-core" % "2.12.0",
    "org.typelevel" %% "cats-effect" % "3.5.4",

    // Testing
    "org.scalatest" %% "scalatest" % "3.2.11" % Test    
)