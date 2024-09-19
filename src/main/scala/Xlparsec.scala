package de.fxttr.scala.xlparsec
import java.io.FileInputStream
import java.util.EnumSet

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SparkSession

import XlsxConfig._
import scala.util.Try

object Xlparsec {

  /**
   * Reads an XLSX File into multiple Apache Spark Dataframes, depending on the config
   *
   * @param filePath
   *   Path to the XLSX-File.
   * @param config
   *   A string, containing the xlsxParser configuration as JSON
   * @return
   *   Either an exception or a Map of Map of Spark Dataframes
   */
  def toDFs(config: String)(implicit
    spark: SparkSession,
  ): Either[Throwable, Map[String, Map[String, Either[Throwable, DataFrame]]]] =
    decodeConfig(config) match {
      case Right(xlsxConfig) =>
        xlsxConfig.connection.adapter match {
          case "local" => toDFsFromLocal(xlsxConfig)
          case "smb"   => toDFsFromSMB(xlsxConfig)
        }
      case Left(e) => Left(e)
    }

  private def toDFsFromLocal(config: XlsxConfig)(implicit
    spark: SparkSession,
  ): Either[Throwable, Map[String, Map[String, Either[Throwable, DataFrame]]]] =
    Try {
      XlsxParser.parse(new FileInputStream(new java.io.File(config.connection.path)), config)
    }.toEither

  private def toDFsFromSMB(
    config: XlsxConfig,
  )(implicit
    spark: SparkSession,
  ): Either[Throwable, Map[String, Map[String, Either[Throwable, DataFrame]]]] = {
    val client = new SMBClient()

    val connection = config.connection.server match {
      case Some(server) => client.connect(server)
      case None         => throw new IllegalArgumentException("Adapter is 'smb' but no server could be found!")
    }

    val session: Session = config.connection.authentication match {
      case Some(authentication) =>
        connection.authenticate(new AuthenticationContext(authentication.username, authentication.password.toCharArray, null))
      case None => throw new IllegalArgumentException("Adapter is 'smb' but no authentication could be found!")
    }

    val share: DiskShare = config.connection.share match {
      case Some(share) => session.connectShare(share).asInstanceOf[DiskShare]
      case None        => throw new IllegalArgumentException("Adapter is 'smb' but no share could be found!")
    }

    val file: File = share.openFile(
      config.connection.path,
      EnumSet.of(AccessMask.FILE_READ_DATA),
      null,
      SMB2ShareAccess.ALL,
      SMB2CreateDisposition.FILE_OPEN,
      null,
    )

    val dfs = Try {
      XlsxParser.parse(file.getInputStream(), config)
    }.toEither

    share.close()
    session.close()
    connection.close()

    dfs
  }
}
