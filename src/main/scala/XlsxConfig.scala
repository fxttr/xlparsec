package de.fxttr.scala.xlparsec

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class Transformation(
  `type`: String,
  parameters: Option[Map[String, String]]
)

object Transformation {
  implicit val decoder: Decoder[Transformation] = deriveDecoder[Transformation]
  implicit val encoder: Encoder[Transformation] = deriveEncoder[Transformation]
}

case class Column(
  name: String,
  index: Int,
  `type`: String,
  format: Option[String],
  transformations: Option[List[Transformation]]
)

object Column {
  implicit val decoder: Decoder[Column] = deriveDecoder[Column]
  implicit val encoder: Encoder[Column] = deriveEncoder[Column]
}

case class Filter(
  column: String,
  condition: String,
  value: String
)

object Filter {
  implicit val decoder: Decoder[Filter] = deriveDecoder[Filter]
  implicit val encoder: Encoder[Filter] = deriveEncoder[Filter]
}

case class ReadRange(
  start_cell: String,
  end_cell: Option[String]
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
  filters: Option[List[Filter]]
)

object Scope {
  implicit val decoder: Decoder[Scope] = deriveDecoder[Scope]
  implicit val encoder: Encoder[Scope] = deriveEncoder[Scope]
}

case class Sheet(
  displayName: String,
  name: String,
  scopes: List[Scope]
)

object Sheet {
  implicit val decoder: Decoder[Sheet] = deriveDecoder[Sheet]
  implicit val encoder: Encoder[Sheet] = deriveEncoder[Sheet]
}

case class XlsxConfig(
  sheets: List[Sheet]
)

object XlsxConfig {
  implicit val decoder: Decoder[XlsxConfig] = deriveDecoder[XlsxConfig]
  implicit val encoder: Encoder[XlsxConfig] = deriveEncoder[XlsxConfig]
}
