package fr.hurrycane.registry

import java.util.UUID.randomUUID

import akka.actor.{ Actor, ActorLogging, Props }
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.sprayjson._
import fr.hurrycane.entity.ActionPerformed
import fr.hurrycane.tools.JsonSupport
final case class Conversation(conversationId: String)
final case class Conversations(users: Seq[Conversation])

object ConversationRegistryActor {
  final case object GetConversation
  final case class CreateConversation()
  final case class GetConversations(name: String)

  def props: Props = Props[ConversationRegistryActor]
}

class ConversationRegistryActor extends Actor with ActorLogging with JsonSupport {
  import ConversationRegistryActor._
  import context.dispatcher

  private val elastic = HttpClient("elasticsearch://" + sys.env("ELASTIC_HOST") + ":" + sys.env("ELASTIC_PORT") + "?ssl=true")

  var users = Set.empty[Conversation]

  def receive: Receive = {
    case GetConversation =>
      sender() ! elastic.execute {
        searchWithType("bot-conversation" / "conversation")
      }.map({
        case Left(failure) => throw new Exception(failure.toString + " - Cannot get conversation")
        case Right(results) => Conversations(results.result.hits.hits.map(el => el.to[Conversation]).toSeq)
      })

    case CreateConversation() =>
      val conversation = Conversation(randomUUID().toString)
      elastic.execute {
        indexInto("bot-conversation" / "conversation").doc[Conversation](conversation).refresh(RefreshPolicy.IMMEDIATE)
      }
      sender() ! ActionPerformed(conversation.conversationId)

    case GetConversations(userId) =>
      sender() ! elastic.execute {
        searchWithType("bot-conversation" / "conversation") termQuery ("conversationId", userId)
      }.map({
        case Left(failure) => None
        case Right(results) => results.result.hits.hits.map(el => el.to[Conversation]).headOption
      })

  }
}