package fr.hurrycane.tools
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
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

  // # Luis Entity
  implicit val luisIntentJsonFormat = jsonFormat2(LuisIntent)
  implicit val luisResponseJsonFormat = jsonFormat3(LuisResponse)

  implicit val performedMessageJsonFormat = jsonFormat5(PerformedMessage)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)

}
