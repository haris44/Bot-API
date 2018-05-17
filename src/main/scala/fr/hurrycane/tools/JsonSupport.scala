package fr.hurrycane.tools
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import fr.hurrycane.dto.RequestDto
import fr.hurrycane.entity._
import fr.hurrycane.registry._
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {

  import DefaultJsonProtocol._

  implicit val userJsonFormat = jsonFormat1(Conversation)
  implicit val usersJsonFormat = jsonFormat1(Conversations)
  implicit val messageJsonFormat = jsonFormat4(Message)
  implicit val messagesJsonFormat = jsonFormat1(Messages)
  implicit val offerJsonFormat = jsonFormat7(Offer)
  implicit val offersJsonFormat = jsonFormat1(Offers)
  implicit val requestDtoJsonFormat = jsonFormat3(RequestDto)

  implicit val kubernetesMemberJsonFormat = jsonFormat2(KubernetesMember)
  implicit val kubernetesMembersJsonFormat = jsonFormat1(KubernetesMembers)

  // # Luis Entity
  implicit val luisEntityJsonFormat = jsonFormat2(LuisEntity)
  implicit val luisIntentJsonFormat = jsonFormat2(LuisIntent)
  implicit val luisResponseJsonFormat = jsonFormat4(LuisResponse)

  implicit val performedMessageJsonFormat = jsonFormat6(PerformedMessage)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)

}
