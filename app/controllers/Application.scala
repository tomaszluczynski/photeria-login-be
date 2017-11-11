package controllers

import javax.inject.Inject

import play.api.libs.json._
import play.api.mvc._
import models.LoginForm._
import models.RegistrationForm.RegistrationForm
import play.api.Logger
import service.{BackendService, DatabaseService}
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class Application @Inject() (
                              databaseService: DatabaseService,
                              backendService: BackendService,
                              wsClient: WSClient) extends Controller {


  def login = Action.async(BodyParsers.parse.json) { request =>
    val loginForm = request.body.validate[LoginForm]
    loginForm.fold (
      errors => {
        Future {
          BadRequest(Json.obj("status" -> "invalid request message format", "message" -> JsError.toJson(errors)))
        }
      },
      loginForm => {
        Logger.info(s"Received login request from ${loginForm.username}")
        val future = databaseService.isValidPassword(loginForm.username, loginForm.password)

        future.map (
          result => {
            result match {
              case None =>
                Unauthorized(Json.obj("status" -> "invalid login or password"));
              case Some((userHash, fUuid)) => {
                val uuid = Await.result(fUuid, 5 seconds)
                Ok(Json.obj("userHash" -> userHash, "uuid" -> uuid))

                /*val uuid = Await.result(fUuid, 5 seconds)

                Logger.info(s"querying for sessionId for userHash: ${userHash} and uuid: ${uuid}")
                val sessionId = Await.result(backendService.fetchSessionId(userHash, uuid), 10 seconds)

                sessionId match {
                  case Some(id) => {
                    Logger.info(s"returning sessionId: ${sessionId}")
                    Ok(Json.obj("sessionId" -> sessionId.get))
                  }
                  case None => {
                    Logger.info("no sessionId possible")
                    BadRequest(Json.obj("status" -> "unable to login at this time"))
                  }
                }*/
              }
            }
          }
        ).recover {
          case t:Throwable =>
              InternalServerError(Json.obj("message" -> t.getMessage))
        }
      }
    )
  }

  def forgotpassword = Action.async(parse.json) { request =>
    val username = (request.body \ "username").asOpt[String];
    val email = (request.body \ "email").asOpt[String];

    if (username.isEmpty && email.isEmpty) {
      Future {
        BadRequest(Json.obj("status" -> "invalid request message format, username and/or email is required"))
      }
    } else {
      Logger.info(s"Received forgot password request from ${username} / ${email}")

      val request: WSRequest = wsClient.url("http://localhost.photeria.net:8080/service/login/forgot").withHeaders("Accept" -> "application/json");

      val data = Json.obj(
        "username" -> username,
        "email" -> email
      )

      val future: Future[WSResponse] = request.post(data)
      future.map(
        response => {
          Logger.info(response.toString)
          Ok(Json.obj("status" -> (response.json \ "status").as[String]))
        }
      ).recover {
        case t: Throwable =>
          InternalServerError(Json.obj("message" -> t.getMessage))
      }
    }
  }

  def register = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[RegistrationForm] fold (
      errors => Future {
          BadRequest(Json.obj("status" -> "invalid request message format", "message" -> JsError.toJson(errors)))
      },
      form => {
        val request: WSRequest = wsClient.url("http://localhost.photeria.net:8080/service/login/register").withHeaders("Accept" -> "application/json");

        val data = Json.obj(
          "username" -> form.username,
          "password1" -> form.password,
          "password2" -> form.password2,
          "email" -> form.email,
          "firstName" -> form.firstName,
          "lastName" -> form.lastName
        )

        val future: Future[WSResponse] = request.post(data)
        future.map(
          response => {
            val status = (response.json \ "status").as[String]

            val result = status match {
              case "ok" => {
                val userHash = (response.json \ "userHash").as[String]
                val fUuid:Future[String] = databaseService.issueSSO(userHash)
                val uuid = Await.result(fUuid, 5 seconds)

                Ok(Json.obj(
                  "status" -> (response.json \ "status").as[String],
                  "uuid" -> uuid,
                  "userHash" -> userHash))
              }
              case "error" =>  BadRequest(response.json)
            }

            result
          }
        ).recover {
          case t: Throwable =>
            InternalServerError(Json.obj("message" -> t.getMessage))
        }
      }
    )
  }
}