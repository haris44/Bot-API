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
import fr.hurrycane.entity.{ ActionPerformed, Offer }
import fr.hurrycane.registry.OfferRegistryActor.{ CreateOffer, GetOffer, GetOffers }
import fr.hurrycane.registry.Offers
import fr.hurrycane.tools.JsonSupport

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

trait OfferRoutes extends JsonSupport {

  implicit lazy val offreTimeout: Timeout = Timeout(5.seconds)

  def offerRoutes(offerRegistryActor: ActorRef, askTimeout: FiniteDuration)(implicit mat: ActorMaterializer, ec: ExecutionContext, rs: RoutingSettings): Route =

    pathPrefix("offer") {
      concat(
        pathEnd {
          concat(
            get {
              val offers: Future[Future[Offers]] =
                (offerRegistryActor ? GetOffers).mapTo[Future[Offers]]
              complete(offers)
            },
            post {
              entity(as[Offer]) { offer =>
                val offerCreated: Future[ActionPerformed] =
                  (offerRegistryActor ? CreateOffer(offer)).mapTo[ActionPerformed]
                onSuccess(offerCreated) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        path(Segment) { uuid =>
          concat(
            get {
              val maybeOffer: Future[Future[Option[Offer]]] =
                (offerRegistryActor ? GetOffer(uuid)).mapTo[Future[Option[Offer]]]
              rejectEmptyResponse {
                complete(maybeOffer)
              }
            })
        })
    }
}