package de.fxttr.scala.xlparsec
import java.io.InputStream

import scala.util.Try

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Row
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object XlsxParser {
  def parse(inputStream: InputStream, config: XlsxConfig)(implicit
    spark: SparkSession,
  ): Map[String, Map[String, Either[Throwable, DataFrame]]] = {
    val workbook = new XSSFWorkbook(inputStream)

    val dataFrames = config.sheets.map { Sheet =>
      Sheet.name -> parseSheet(workbook.getSheet(Sheet.name), Sheet, config.context)
    }.toMap

    workbook.close()
    dataFrames
  }

  private def parseSheet(sheet: XSSFSheet, Sheet: Sheet, context: String)(implicit
    spark: SparkSession,
  ): Map[String, Either[Throwable, DataFrame]] =
    Sheet.scopes.map { scope =>
      scope.name -> parseScope(sheet, scope, context)
    }.toMap

  private def parseScope(sheet: XSSFSheet, scope: Scope, context: String)(implicit
    spark: SparkSession,
  ): Either[Throwable, DataFrame] =
    for {
      rows <- extractRows(sheet, scope)
      df <- createDataFrame(rows, scope)
      dfWithVCols <- addVirtualColumns(sheet, df, scope.vcolumns)
      dfWithVColsAndContext <- addContext(dfWithVCols, context)
    } yield dfWithVColsAndContext

  private def addContext(df: DataFrame, context: String): Either[Throwable, DataFrame] =
    Try {
      df.withColumn("Context", lit(context))
    }.toEither

  private def extractRows(sheet: XSSFSheet, scope: Scope): Either[Throwable, List[Row]] =
    Try {
      val (startRow, startCol) = toCoordinate(scope.read_range.start_cell)
      val (endRow, _) = toCoordinate(scope.read_range.end_cell)

      (startRow until endRow).toList.map { rowIndex =>
        val row = sheet.getRow(rowIndex)
        val cells = scope.columns.map { column =>
          extractCellValue(row, column.index + startCol)
        }
        Row(cells: _*)
      }
    }.toEither

  private def extractCellValue(row: org.apache.poi.ss.usermodel.Row, columnIndex: Int): String =
    Option(row.getCell(columnIndex)) match {
      case Some(cell) =>
        cell.getCellType match {
          case CellType.STRING => cell.getStringCellValue
          case CellType.NUMERIC =>
            if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
              cell.getDateCellValue.toString
            } else {
              cell.getNumericCellValue.toString
            }
          case CellType.BOOLEAN => cell.getBooleanCellValue.toString
          case CellType.FORMULA =>
            cell.getCachedFormulaResultType() match {
              case CellType.STRING => cell.getStringCellValue
              case CellType.NUMERIC =>
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                  cell.getDateCellValue.toString
                } else {
                  cell.getNumericCellValue.toString
                }
              case CellType.BLANK => ""
              case _              => "Unknown Type"
            }
          case CellType.BLANK => ""
          case _              => "Unknown Type"
        }
      case None => ""
    }

  private def createDataFrame(rows: List[Row], scope: Scope)(implicit
    spark: SparkSession,
  ): Either[Throwable, DataFrame] =
    Try {
      val schema =
        StructType(scope.columns.map(c => StructField(c.name, StringType, nullable = true)))
      val df = spark.createDataFrame(spark.sparkContext.parallelize(rows), schema)
      val windowSpec = Window.orderBy(monotonically_increasing_id())

      df.withColumn("id", row_number().over(windowSpec))
    }.toEither

  private def addVirtualColumns(sheet: XSSFSheet, df: DataFrame, vcolumns: List[VColumn])(implicit
    spark: SparkSession,
  ): Either[Throwable, DataFrame] =
    vcolumns.foldLeft[Either[Throwable, DataFrame]](Right(df)) { (dfAcc, vcol) =>
      import spark.implicits._
      dfAcc.flatMap { currentDf =>
        createVirtualColumn(sheet, vcol).map { vcolRow =>
          currentDf.withColumn(vcol.name, listToColumn(vcolRow)($"id"))
        }
      }
    }

  private def createVirtualColumn(sheet: XSSFSheet, vcol: VColumn): Either[Throwable, List[String]] =
    vcol.transform match {
      case "merge" =>
        Try {
          vcol.read_range match {
            case Some(read_range) =>
              val (startRow, startCol) = toCoordinate(read_range.start_cell)
              val (endRow, endCol) = toCoordinate(read_range.end_cell)

              (startRow until endRow).toList.map { rowIndex =>
                val row = sheet.getRow(rowIndex)

                (startCol until endCol).toList.map { colIndex =>
                  extractCellValue(row, colIndex)
                }.mkString(" ")
              }
            case _ => throw new IllegalArgumentException("Transformation is 'merge' but no read_range could be found!")
          }

        }.toEither
      case "fix" =>
        Try {
          vcol.columns match {
            case Some(columns) =>
              columns.map { fixColumn =>
                fixColumn.name
              }
            case _ => throw new IllegalArgumentException("Transformation is 'fix' but no columns could be found!")
          }
        }.toEither
    }

  private def toCoordinate(cell: String): (Int, Int) = {
    val reference = new CellReference(cell)
    (reference.getRow, reference.getCol)
  }

  private def listToColumn(list: List[String]) = udf((index: Int) => list(index - 1))
}
