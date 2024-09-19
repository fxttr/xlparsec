package de.fxttr.scala.xlparsec

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._

case class Column(
  name: String,
  index: Int,
  `type`: String,
  format: Option[String],
)

object Column {
  implicit val decoder: Decoder[Column] = deriveDecoder[Column]
  implicit val encoder: Encoder[Column] = deriveEncoder[Column]
}

case class VColumn(name: String, read_range: ReadRange, transform: String)

object VColumn {
  implicit val decoder: Decoder[VColumn] = deriveDecoder[VColumn]
  implicit val encoder: Encoder[VColumn] = deriveEncoder[VColumn]
}

case class ReadRange(
  start_cell: String,
  end_cell: String,
)

object ReadRange {
  implicit val decoder: Decoder[ReadRange] = deriveDecoder[ReadRange]
  implicit val encoder: Encoder[ReadRange] = deriveEncoder[ReadRange]
}

case class Scope(
  name: String,
  read_range: ReadRange,
  description: Option[String],
  columns: List[Column],
  vcolumns: List[VColumn],
)

object Scope {
  implicit val decoder: Decoder[Scope] = deriveDecoder[Scope]
  implicit val encoder: Encoder[Scope] = deriveEncoder[Scope]
}

case class Sheet(
  displayName: String,
  name: String,
  scopes: List[Scope],
)

object Sheet {
  implicit val decoder: Decoder[Sheet] = deriveDecoder[Sheet]
  implicit val encoder: Encoder[Sheet] = deriveEncoder[Sheet]
}

case class XlsxConfig(
  sheets: List[Sheet],
)

object XlsxConfig {
  implicit val decoder: Decoder[XlsxConfig] = deriveDecoder[XlsxConfig]
  implicit val encoder: Encoder[XlsxConfig] = deriveEncoder[XlsxConfig]

  def decodeConfig(config: String): Either[Throwable, XlsxConfig] =
    decode[XlsxConfig](config)
}
