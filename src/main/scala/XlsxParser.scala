package de.fxttr.scala.xlparsec
import scala.util.Try

import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Row
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._

object XlsxParser {
  def parse(filePath: String, config: XlsxConfig)(implicit
    spark: SparkSession,
  ): Map[String, Map[String, Either[Throwable, DataFrame]]] = {
    val workbook = new XSSFWorkbook(filePath)

    val dataFrames = config.sheets.map { Sheet =>
      Sheet.name -> parseSheet(workbook.getSheet(Sheet.name), Sheet)
    }.toMap

    workbook.close()
    dataFrames
  }

  private def parseSheet(sheet: XSSFSheet, Sheet: Sheet)(implicit
    spark: SparkSession,
  ): Map[String, Either[Throwable, DataFrame]] =
    Sheet.scopes.map { scope =>
      scope.name -> parseScope(sheet, scope)
    }.toMap

  private def parseScope(sheet: XSSFSheet, scope: Scope)(implicit
    spark: SparkSession,
  ): Either[Throwable, DataFrame] =
    for {
      rows <- extractRows(sheet, scope)
      df <- createDataFrame(rows, scope)
      dfWithVCols <- addVirtualColumns(sheet, df, scope.vcolumns)
    } yield dfWithVCols

  private def extractRows(sheet: XSSFSheet, scope: Scope): Either[Throwable, List[Row]] =
    Try {
      val (startRow, _) = toCoordinate(scope.read_range.start_cell)
      val (endRow, _) = toCoordinate(scope.read_range.end_cell)
      (startRow until endRow).toList.map { rowIndex =>
        val row = sheet.getRow(rowIndex)
        val cells = scope.columns.map { column =>
          extractCellValue(row, column)
        }
        Row(cells: _*)
      }
    }.toEither

  private def extractCellValue(row: org.apache.poi.ss.usermodel.Row, column: Column): String = {
    val cell = row.getCell(column.index)
    column.`type` match {
      case "date"   => cell.getDateCellValue.toString
      case "string" => cell.getStringCellValue
      case "double" => cell.getNumericCellValue.toString
      case _        => ""
    }
  }

  private def createDataFrame(rows: List[Row], scope: Scope)(implicit
    spark: SparkSession,
  ): Either[Throwable, DataFrame] =
    Try {
      val schema =
        StructType(scope.columns.map(c => StructField(c.name, StringType, nullable = true)))
      spark.createDataFrame(spark.sparkContext.parallelize(rows), schema)
    }.toEither

  private def addVirtualColumns(sheet: XSSFSheet, df: DataFrame, vcolumns: List[VColumn])(implicit
    spark: SparkSession,
  ): Either[Throwable, DataFrame] =
    vcolumns.foldLeft[Either[Throwable, DataFrame]](Right(df)) { (dfAcc, vcol) =>
      dfAcc.flatMap { currentDf =>
        createVirtualColumn(sheet, vcol).map { vcolDf =>
          currentDf.withColumn(vcol.name, vcolDf(vcol.name))
        }
      }
    }

  private def createVirtualColumn(sheet: XSSFSheet, vcol: VColumn)(implicit
    spark: SparkSession,
  ): Either[Throwable, DataFrame] =
    Try {
      val (startRow, startCol) = toCoordinate(vcol.read_range.start_cell)
      val (endRow, endCol) = toCoordinate(vcol.read_range.end_cell)
      val rows = (startRow until endRow).toList.map { rowIndex =>
        Row(sheet.getRow(rowIndex).getCell(startCol).getStringCellValue)
      }
      val schema = StructType(Array(StructField(vcol.name, StringType, nullable = true)))
      spark.createDataFrame(spark.sparkContext.parallelize(rows), schema)
    }.toEither

  private def toCoordinate(cell: String): (Int, Int) = {
    val reference = new CellReference(cell)
    (reference.getRow, reference.getCol)
  }
}
