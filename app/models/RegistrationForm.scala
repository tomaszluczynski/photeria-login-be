package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

object RegistrationForm {

  case class RegistrationForm(
                               username: String,
                               password: String,
                               password2: String,
                               email: String,
                               firstName: Option[String],
                               lastName: Option[String])

  implicit val writes = Json.writes[RegistrationForm]

  implicit val reads: Reads[RegistrationForm] = (
      (JsPath \ "username").read[String](Reads.minLength[String](4)) and
      (JsPath \ "password").read[String] and
      (JsPath \ "password2").read[String] and
      (JsPath \ "email").read[String] and
      (JsPath \ "firstName").readNullable[String] and
      (JsPath \ "lastName").readNullable[String]
    )(RegistrationForm.apply _)


}
