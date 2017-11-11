package service

import javax.inject.{Inject, Singleton}

import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.libs.ws._
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class BackendService  @Inject() (wsClient: WSClient) {
  def fetchSessionId(userHash: String, uuid: String):Future[Option[String]] = {
    Future {
      val request: WSRequest = wsClient.url(s"http://localhost.photeria.net:8080/service/login/success/${userHash}/${uuid}")
        .withHeaders("Accept" -> "application/json");

      val future: Future[WSResponse] = request.get()

      val response = Await.result(future, 10 seconds);
      val sessionId = (response.json \ "sessionId").as[String]

      Some(sessionId)
    }
  }
}
