package models

import play.api.libs.json.Json

object ForgotPasswordForm {

  case class ForgotPasswordForm(username: String, email: String)

  implicit val writes = Json.writes[ForgotPasswordForm]
  implicit val reads = Json.reads[ForgotPasswordForm]
}
