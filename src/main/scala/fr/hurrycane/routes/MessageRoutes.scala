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
import fr.hurrycane.dto.RequestDto
import fr.hurrycane.entity.ActionPerformed
import fr.hurrycane.registry.MessageRegistryActor._
import fr.hurrycane.registry.{ Message, Messages }
import fr.hurrycane.tools.JsonSupport

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

trait MessageRoutes extends JsonSupport {

  implicit lazy val messageTimeout: Timeout = Timeout(5.seconds)

  def messageRoutes(messageRegistryActor: ActorRef, askTimeout: FiniteDuration)(implicit mat: ActorMaterializer, ec: ExecutionContext, rs: RoutingSettings): Route =

    pathPrefix("message") {
      concat(
        pathEnd {
          concat(
            get {
              val messages: Future[Future[Messages]] =
                (messageRegistryActor ? GetMessages).mapTo[Future[Messages]]
              complete(messages)
            },
            post {
              entity(as[RequestDto]) { message =>
                println("RECEIVE POST REQUEST")
                val userCreated: Future[Future[ActionPerformed]] =
                  (messageRegistryActor ? SendMessage(message)).mapTo[Future[ActionPerformed]]
                onSuccess(userCreated) { performed =>
                  println("COMPLETE REQUEST")
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        path(Segment) { name =>
          concat(
            get {
              val maybeUser: Future[Future[Option[Message]]] =
                (messageRegistryActor ? GetMessage(name)).mapTo[Future[Option[Message]]]
              rejectEmptyResponse {
                complete(maybeUser)
              }
            })
        })
    }
}