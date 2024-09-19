package de.fxttr.scala.xlparsec

import org.apache.spark.sql.SparkSession
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.apache.spark.sql.DataFrame
import scala.util.{Either, Left, Right}
import scala.io.Source

class XlsxParserTest extends AnyFunSuite with Matchers {
  implicit val spark: SparkSession = SparkSession
    .builder()
    .master("local[*]")
    .appName("XlsxParserTest")
    .getOrCreate()

  val testFilePath = Source.fromURL(getClass.getResource("/test.xlsx"))
  val testConfig = Source.fromURL(getClass.getResource("/test.json")).mkString

  test("parseSheet should return DataFrame for valid sheet and scope") {
    val result = XlsxParser.parseSheet(
      testFilePath,
      testConfig
    )

    result shouldBe a[Map[_, _]]
    result("TestScope") shouldBe a[Right[_, DataFrame]]
  }

  test("parseScope should return an error for invalid range") {
    val invalidScope = Scope(
      name = "InvalidScope",
      read_range = ReadRange("A1000", "B2000"),
      description = None,
      columns = List(Column("Date", 0, "date", None)),
      vcolumns = List()
    )

    val result = XlsxParser.parseScope(
      spark.read.format("xlsx").load(testFilePath).sheetAt(0),
      invalidScope
    )

    result shouldBe a[Left[_, _]]
  }

  test("extractRows should correctly parse rows within the range") {
    val scope = testConfig.sheets.head.scopes.head
    val sheet = spark.read.format("xlsx").load(testFilePath).sheetAt(0)

    val result = XlsxParser.extractRows(sheet, scope)

    result shouldBe a[Right[_, _]]
    result.right.get.length should be > 0
  }

  test("createDataFrame should return DataFrame with correct schema") {
    val rows = List(
      Row("2024-09-19", "123.45"),
      Row("2024-09-20", "678.90")
    )

    val scope = testConfig.sheets.head.scopes.head

    val result = XlsxParser.createDataFrame(rows, scope)

    result shouldBe a[Right[_, DataFrame]]

    val df = result.right.get
    df.columns should contain allElementsOf List("Date", "Value")
  }

  test("addVirtualColumns should add virtual columns to DataFrame") {
    val rows = List(
      Row("2024-09-19", "123.45")
    )

    val scope = testConfig.sheets.head.scopes.head
    val df = spark.createDataFrame(
      spark.sparkContext.parallelize(rows),
      StructType(List(StructField("Date", StringType, true), StructField("Value", StringType, true)))
    )

    val vcol = VColumn("VirtualCol", ReadRange("C1", "C10"), "someTransformation")

    val result = XlsxParser.addVirtualColumns(df, List(vcol))

    result shouldBe a[Right[_, DataFrame]]

    val dfWithVCol = result.right.get
    dfWithVCol.columns should contain("VirtualCol")
  }
}
