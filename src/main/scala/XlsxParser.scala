package de.fxttr.scala.xlparsec

import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.spark.sql.{DataFrame, SparkSession, Row}
import org.apache.spark.sql.types._
import cats.implicits._

object XlsxParser {
  def parse(filePath: String, config: XlsxConfig)(implicit spark: SparkSession): Map[String, Map[String, Either[Throwable, DataFrame]]] = {
    val workbook = new XSSFWorkbook(filePath)
    val sheetConfigs = config.sheets

    val dataFrames: Map[String, Map[String, Either[Throwable, DataFrame]]] = sheetConfigs.map { sheetConfig =>
      val sheet = workbook.getSheet(sheetConfig.name)

      val scopeDataFrames: Map[String, Either[Throwable, DataFrame]] = sheetConfig.scopes.map { scope =>
        val result = try {
          val startCell = scope.read_range.start_cell
          val (startRow, startCol) = toCoordinate(startCell)

          val rows = (startRow until sheet.getLastRowNum).toList.map { rowIndex =>
            val row = sheet.getRow(rowIndex)
            val cells = scope.columns.map { column =>
              val cell = row.getCell(column.index)
              val cellValue = column.`type` match {
                case "date" => cell.getDateCellValue.toString
                case "string" => cell.getStringCellValue
                case "double" => cell.getNumericCellValue.toString
                case _ => ""
              }
              cellValue
            }
            Row(cells: _*)
          }

          val schema = StructType(scope.columns.map(c => StructField(c.name, StringType, nullable = true)))
          val rdd = spark.sparkContext.parallelize(rows)
          Right(spark.createDataFrame(rdd, schema))
        } catch {
          case e: Throwable => Left(e)
        }

        scope.name -> result
      }.toMap

      sheetConfig.name -> scopeDataFrames
    }.toMap

    workbook.close()
    dataFrames
  }

  private def toCoordinate(cell: String): (Int, Int) = {
    val row = cell.tail.toInt - 1
    val col = cell.head - 'A'
    (row, col)
  }
}
