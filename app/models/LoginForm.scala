package models

import play.api.libs.json.Json

object LoginForm {

  case class LoginForm(username: String, password: String)

  implicit val writes = Json.writes[LoginForm]

  implicit val reads = Json.reads[LoginForm]

}
