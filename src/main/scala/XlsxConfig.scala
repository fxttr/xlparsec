package de.fxttr.scala.xlparsec

import io.circe.Decoder
import io.circe.Encoder
import io.circe._
import io.circe.generic.semiauto._
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

case class VFixColumn(name: String, index: Int)

object VFixColumn {
  implicit val decoder: Decoder[VFixColumn] = deriveDecoder[VFixColumn]
  implicit val encoder: Encoder[VFixColumn] = deriveEncoder[VFixColumn]
}

case class VColumn(name: String, read_range: Option[ReadRange], transform: String, columns: Option[List[VFixColumn]])

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

case class Connection(
  adapter: String,
  path: String,
  server: Option[String],
  share: Option[String],
  authentication: Option[Authentication],
)

object Connection {
  implicit val decoder: Decoder[Connection] = deriveDecoder[Connection]
  implicit val encoder: Encoder[Connection] = deriveEncoder[Connection]
}

case class Authentication(
  username: String,
  password: String,
)

object Authentication {
  implicit val decoder: Decoder[Authentication] = deriveDecoder[Authentication]
  implicit val encoder: Encoder[Authentication] = deriveEncoder[Authentication]
}

case class XlsxConfig(
  context: String,
  connection: Connection,
  sheets: List[Sheet],
)

object XlsxConfig {
  implicit val decoder: Decoder[XlsxConfig] = deriveDecoder[XlsxConfig]
  implicit val encoder: Encoder[XlsxConfig] = deriveEncoder[XlsxConfig]

  def decodeConfig(config: String): Either[Throwable, XlsxConfig] =
    decode[XlsxConfig](config)
}
