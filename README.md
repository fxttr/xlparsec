# xlparsec for Apache Spark

A library built in Scala 2.12 that utilizes Apache POI to parse Excel Workbooks based on a flexible JSON configuration. It seamlessly integrates with Apache Spark, allowing you to load Excel data into Spark DataFrames for further processing.

## Features

- **JSON-Based Configuration:** Easily define how to extract data from Excel sheets, including ranges, columns, and virtual columns using a simple JSON config.
- **Virtual Columns:** Add computed or derived columns that don't exist in the original Excel sheet.

## Installation

1. Download the `xlparsec-${VERSION}.jar`.
2. Place the JAR file in your `${SPARK_HOME}/jars/` directory.

## Usage

### Basic Example

```scala
import de.fxttr.scala.xlparsec.Xlparsec
import org.apache.spark.sql.SparkSession

implicit val spark: SparkSession = SparkSession.builder()
  .appName("ExcelParserExample")
  .master("local[*]")
  .getOrCreate()

val configJson = """{...}"""

val result = Xlparsec.toDFs(configJson)

result match {
  case Right(dataFrames) => 
    dataFrames.foreach { case (sheetName, scopes) =>
      scopes.foreach { case (scopeName, dfEither) =>
        dfEither match {
          case Right(df) => df.show()
          case Left(error) => println(s"Error parsing scope $scopeName: $error")
        }
      }
    }
  case Left(error) => println(s"Error parsing file: $error")
}
```
For more examples, refer to the /examples directory.

### Documentation
Comprehensive documentation can be found in the /docs directory, including detailed configuration instructions, supported JSON schema, and advanced usage scenarios.

### Contributing
Feel free to open issues or submit pull requests for improvements or bug fixes. Contributions are always welcome!

### License
This project is licensed under the Apache-2.0 License.