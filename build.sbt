scalaVersion := "2.12.20"

name := "xlparsec"
organization := "de.fxttr.scala"
version := "1.0"

ThisBuild / assemblyMergeStrategy := {
 case PathList("META-INF", _*) => MergeStrategy.discard
 case _                        => MergeStrategy.first
}

ThisBuild / assemblyShadeRules := Seq(
  ShadeRule.rename("shapeless.**" -> "new_shapeless.@1").inAll,
  ShadeRule.rename("cats.kernel.**" -> s"new_cats.kernel.@1").inAll
)

libraryDependencies ++= Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0",

    // Json
    "io.circe" %% "circe-core" % "0.15.0-M1",
    "io.circe" %% "circe-parser" % "0.15.0-M1",
    "io.circe" %% "circe-generic" % "0.15.0-M1",

    // Reading XLSX
    "org.apache.poi" % "poi-ooxml" % "5.2.3",

    // Spark
    "org.apache.spark" %% "spark-core" % "3.5.2" % "provided",
    "org.apache.spark" %% "spark-sql" % "3.5.2" % "provided",

    // Testing
    "org.scalatest" %% "scalatest" % "3.2.11" % Test    
)