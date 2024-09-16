package de.fxttr.scala.xlparsec

import XlsxConfig._
import io.circe.parser._
import org.apache.spark.sql.{DataFrame, SparkSession}

object Xlparsec {

  /**
   * Reads an XLSX File into multiple Apache Spark Dataframes, depending on the config
   *
   * @param filePath Path to the XLSX-File.
   * @param config A string, containing the xlsxParser configuration as JSON
   * @return Either an exception or a Map of Map of Spark Dataframes
   */
  def toDFs(filePath: String, config: String)(implicit spark: SparkSession): Either[Throwable, Map[String,Map[String,Either[Throwable,DataFrame]]]] = {
    decode[XlsxConfig](config) match {
      case Right(xlsxConfig) => Right(XlsxParser.parse(filePath, xlsxConfig))
      case Left(e) => Left(e)
    }
  }
}
