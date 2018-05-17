package fr.hurrycane.routes

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.{ get, post }
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.settings.RoutingSettings
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import fr.hurrycane.entity.ActionPerformed
import fr.hurrycane.registry.ConversationRegistryActor._
import fr.hurrycane.registry.{ Conversation, Conversations }
import fr.hurrycane.tools.JsonSupport

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

trait ConversationRoutes extends JsonSupport {

  implicit lazy val userTimeout: Timeout = Timeout(5.seconds)

  def conversationRoutes(conversationRegistryActor: ActorRef, askTimeout: FiniteDuration)(implicit mat: ActorMaterializer, ec: ExecutionContext, rs: RoutingSettings): Route =

    pathPrefix("conversation") {
      concat(
        pathEnd {
          concat(
            get {
              val users: Future[Future[Conversations]] =
                (conversationRegistryActor ? GetConversation).mapTo[Future[Conversations]]
              complete(users)
            },
            post {
              val userCreated: Future[ActionPerformed] =
                (conversationRegistryActor ? CreateConversation).mapTo[ActionPerformed]
              onSuccess(userCreated) { performed =>
                complete((StatusCodes.Created, performed.description))

              }
            })
        },
        path(Segment) { name =>
          concat(
            get {
              val maybeUser: Future[Future[Option[Conversation]]] =
                (conversationRegistryActor ? GetConversations).mapTo[Future[Option[Conversation]]]
              rejectEmptyResponse {
                complete(maybeUser)
              }
            })
        })
    }
}