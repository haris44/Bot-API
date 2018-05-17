package fr.hurrycane.registry

import java.net.URLEncoder

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.sprayjson._
import fr.hurrycane.entity.{ ActionPerformed, LuisResponse, PerformedMessage }
import fr.hurrycane.tools.{ JsonSupport, RabbitFactory }

import concurrent.duration._
import spray.json._

final case class Message(uuid: String, content: String, mood: String, conversationId: String)
final case class Messages(messages: Seq[Message])

object MessageRegistryActor {
  final case object GetMessages
  final case class SendMessage(message: Message)
  final case class GetMessage(uuid: String)

  def props: Props = Props[MessageRegistryActor]
}

class MessageRegistryActor extends Actor with ActorLogging with JsonSupport {
  import com.newmotion.akka.rabbitmq._
  import MessageRegistryActor._
  import context.dispatcher

  private val rabbit: ActorRef = context.actorOf(ConnectionActor.props(RabbitFactory.getFactory, reconnectionDelay = 10.seconds), "rabbit-connexion")
  private val elastic = HttpClient("elasticsearch://" + sys.env("ELASTIC_HOST") + ":" + sys.env("ELASTIC_PORT") + "?ssl=true")

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def receive: Receive = {
    case GetMessages =>
      sender() ! elastic.execute {
        searchWithType("bot-message" / "message")
      }.map({
        case Left(failure) => throw new Exception(failure.toString + " - Cannot get messages")
        case Right(results) => Messages(results.result.hits.hits.map(el => el.to[Message]).toSeq)
      })

    case SendMessage(message) =>

      elastic.execute {
        indexInto("bot-message" / "message").doc[Message](message).refresh(RefreshPolicy.IMMEDIATE)
      }
      sender() ! Http()(context.system).singleRequest(HttpRequest(uri = sys.env("LUIS_URL") + URLEncoder.encode(message.content, "UTF-8")))
        .flatMap(response => Unmarshal(response.entity).to[LuisResponse])
        .map(parsed => {
          val channel = RabbitFactory.setupIntentChannel(rabbit, parsed.topScoringIntent)
          val performedMessage = PerformedMessage(message.content, System.currentTimeMillis, parsed.topScoringIntent, message.mood, message.conversationId)
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
