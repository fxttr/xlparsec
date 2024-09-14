package de.fxttr.scala.Xlparsec

import io.circe.generic.auto._
import io.circe.parser._

case class ColumnConfig(name: String, `type`: String, source: String)
case class Filters(ignoreEmptyRows: Boolean, dateRange: Option[DateRange])
case class DateRange(start: String, end: String)
case class SheetConfig(sheetName: String, columns: List[ColumnConfig], filters: Option[Filters])

object Config {
    def loadConfig(configJson: String): SheetConfig = {
        decode[SheetConfig](configJson) match {
            case Right(config) => config
            case Left(error) => throw new RuntimeException(s"Error parsing config: $error")
        }
    }
}