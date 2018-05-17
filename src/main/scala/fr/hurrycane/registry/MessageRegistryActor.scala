package fr.hurrycane.registry

import java.net.URLEncoder
import java.util.UUID.randomUUID

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.stream.ActorMaterializer
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.sprayjson._
import fr.hurrycane.dto.RequestDto
import fr.hurrycane.entity.{ ActionPerformed, LuisResponse, PerformedMessage }
import fr.hurrycane.tools.{ JsonSupport, RabbitFactory }
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import spray.json._

import scala.concurrent.duration._

final case class Message(uuid: String, content: String, mood: String, conversationId: String)
final case class Messages(messages: Seq[Message])

object MessageRegistryActor {
  final case object GetMessages
  final case class SendMessage(message: RequestDto)
  final case class GetMessage(uuid: String)

  def props: Props = Props[MessageRegistryActor]
}

class MessageRegistryActor extends Actor with ActorLogging with JsonSupport {
  import MessageRegistryActor._
  import com.newmotion.akka.rabbitmq._
  import play.api.libs.ws.DefaultBodyReadables._

  private val rabbit: ActorRef = context.actorOf(ConnectionActor.props(RabbitFactory.getFactory, reconnectionDelay = 10.seconds), "rabbit-connexion")
  private val elastic = HttpClient("elasticsearch://" + sys.env("ELASTIC_HOST") + ":" + sys.env("ELASTIC_PORT") + "?ssl=true")

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import context.dispatcher

  def receive: Receive = {
    case GetMessages =>
      sender() ! elastic.execute {
        searchWithType("bot-message" / "message")
      }.map({
        case Left(failure) => throw new Exception(failure.toString + " - Cannot get messages")
        case Right(results) => Messages(results.result.hits.hits.map(el => el.to[Message]).toSeq)
      })

    case SendMessage(request) =>
      log.info("CALLING LUIS")
      val message = Message(randomUUID().toString, request.content, request.mood, request.conversationId)
      elastic.execute {
        indexInto("bot-message" / "message").doc[Message](message).refresh(RefreshPolicy.IMMEDIATE)
      }
      sender() ! StandaloneAhcWSClient()
        .url(sys.env("LUIS_URL") + URLEncoder.encode(message.content, "UTF-8"))
        .get()
        .map(response => {
          response.body[String].parseJson.convertTo[LuisResponse]
        })
        .map(parsed => {
          log.info("RECEIVE LUIS RESPONSE :" + parsed.topScoringIntent.intent)
          val channel = RabbitFactory.setupIntentChannel(rabbit, parsed.topScoringIntent)
          val performedMessage = PerformedMessage(message.content, System.currentTimeMillis, parsed.topScoringIntent, message.mood, message.conversationId, parsed.entities)
          channel ! ChannelMessage((channel) => channel.basicPublish(RabbitFactory.exchange, "bot.intent." + parsed.topScoringIntent.intent, null, performedMessage.toJson.toString().getBytes()), dropIfNoChannel = false)
          ActionPerformed(message.uuid)
        })

    case GetMessage(uuid) =>
      sender() ! elastic.execute {
        searchWithType("bot-message" / "message") termQuery ("uuid", uuid)
      }.map({
        case Left(failure) => None
        case Right(results) => results.result.hits.hits.map(el => el.to[Conversation]).headOption
      })

  }
}
