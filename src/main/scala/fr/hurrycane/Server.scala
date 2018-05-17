package fr.hurrycane

import java.util.concurrent.TimeUnit

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpEntity, HttpMethods, HttpRequest, MediaTypes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.stream.ActorMaterializer
import com.newmotion.akka.rabbitmq._
import fr.hurrycane.db.DatabaseCreator
import fr.hurrycane.entity.PerformedMessage
import fr.hurrycane.registry.{ ClusterMemberShipRegistry, ConversationRegistryActor, MessageRegistryActor, OfferRegistryActor }
import fr.hurrycane.routes.{ ClusterRoutes, ConversationRoutes, MessageRoutes, OfferRoutes }
import fr.hurrycane.tools.RabbitFactory
import spray.json._

import scala.concurrent.duration._

object Server extends App with ConversationRoutes with MessageRoutes with OfferRoutes with ClusterRoutes {

  DatabaseCreator.checkAll()

  val actorSystemName = sys.env("AKKA_ACTOR_SYSTEM_NAME")

  implicit val actorSystem = ActorSystem(actorSystemName)
  implicit val mat = ActorMaterializer()
  import actorSystem.dispatcher
  implicit val http = Http(actorSystem)
  implicit val routingSettings = RoutingSettings(actorSystem)

  private val rabbit: ActorRef = actorSystem.actorOf(ConnectionActor.props(RabbitFactory.getFactory, reconnectionDelay = 10.seconds), "rabbit-connexion")
  rabbit ! CreateChannel(ChannelActor.props(RabbitFactory.setupChannel))

  def setupIntentSubscriber(channel: Channel, self: ActorRef) {
    channel.queueBind(RabbitFactory.messageQueue, RabbitFactory.exchange, RabbitFactory.messageBinding)

    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]) {
        val response = RabbitFactory.fromBytes(body).parseJson.convertTo[PerformedMessage]
        val entity = HttpEntity(MediaTypes.`application/json`, response.toJson.toString())
        println("RECEIVE RESPONSE")
        Http(actorSystem).singleRequest(HttpRequest(
          method = HttpMethods.POST,
          uri = sys.env("RESPONSE_URL") + "conversation/" + response.conversationId + "/callback",
          entity = entity))
      }
    }
    channel.basicConsume(RabbitFactory.messageQueue, true, consumer)
  }

  rabbit ! CreateChannel(ChannelActor.props(setupIntentSubscriber), Some("bot-intent-api"))

  val clusterMembershipAskTimeout = FiniteDuration(sys.env("CLUSTER_MEMBERSHIP_ASK_TIMEOUT").toLong, TimeUnit.MILLISECONDS)

  val conversationRegistryActor: ActorRef = actorSystem.actorOf(ConversationRegistryActor.props, "userRegistryActor")
  val messageRegistryActor: ActorRef = actorSystem.actorOf(MessageRegistryActor.props, "messageRegistryActor")
  val offerRegistryActor: ActorRef = actorSystem.actorOf(OfferRegistryActor.props, "offerRegistryActor")
  val clusterMemberShipRegistryActor: ActorRef = actorSystem.actorOf(ClusterMemberShipRegistry.props, "clusterRegistryActor")

  val offerRoute: Route = offerRoutes(offerRegistryActor, clusterMembershipAskTimeout)
  val conversationRoute: Route = conversationRoutes(conversationRegistryActor, clusterMembershipAskTimeout)
  val messageRoute: Route = messageRoutes(messageRegistryActor, clusterMembershipAskTimeout)
  val membersRoute: Route = clusterRoutes(clusterMemberShipRegistryActor, clusterMembershipAskTimeout)

  lazy val routes: Route = conversationRoute ~ messageRoute ~ offerRoute ~ membersRoute

  http.bindAndHandle(routes, sys.env("HTTP_HOST"), sys.env("HTTP_PORT").toInt)

  println(s"Server online at http://" + sys.env("HTTP_HOST") + ":" + sys.env("HTTP_PORT").toInt + "/")

}

