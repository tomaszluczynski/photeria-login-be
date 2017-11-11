package service

import java.security.MessageDigest
import java.util.UUID
import javax.inject.{Inject, Singleton}

import anorm._
import play.api.Logger
import play.api.db.DB
import play.api.Play.current
import anorm.SqlParser
import sun.misc.BASE64Encoder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class DatabaseService {

  def isValidPassword(username: String, password: String): Future[Option[(String, Future[String])]] = {
    Logger.debug(s"Checking if ${username} provided correct password...")
    val f = Future {
      DB.withConnection { implicit c =>
        val md5Password = md5(password);
        val selectHash = SQL"SELECT hash FROM user WHERE username = ${username} AND password=${md5Password}"
        val dbHash = selectHash.as(SqlParser.str("hash").singleOpt)

        dbHash match {
          case None =>
            Logger.debug(s"...password was incorrect")
            None
          case Some(x) =>
            Logger.debug(s"...password was correct " + x)
            val uuid = issueSSO(dbHash.get)
            Some((dbHash.get, uuid))
        }
      }
    }
    return f;
  }

  def issueSSO(hash: String): Future[String] = {
    val f = Future {
      DB.withConnection { implicit c =>
        val uuid: String = UUID.randomUUID.toString

        SQL"DELETE FROM user_sso WHERE user_hash = ${hash}".executeUpdate()
        SQL"INSERT INTO user_sso (user_hash, sso_key, timestamp) VALUES (${hash}, ${uuid}, CURTIME())".executeInsert()
        Logger.debug(s"Issued SSO key for userhash ${hash}")
        uuid
      }
    }
    return f
  }

  def md5(value: String): String = {
    val md = MessageDigest.getInstance("SHA")
    md.update(value.getBytes("UTF-8"))
    val hash = (new BASE64Encoder).encode(md.digest())
    return hash;
  }
}
