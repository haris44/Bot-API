package fr.hurrycane.registry

import akka.actor.{ Actor, ActorLogging, Props }
import akka.stream.ActorMaterializer
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.sprayjson._
import fr.hurrycane.entity.{ ActionPerformed, Offer }
import fr.hurrycane.tools.JsonSupport
import java.util.UUID.randomUUID

final case class Offers(offers: Seq[Offer])

object OfferRegistryActor {
  final case object GetOffers
  final case class CreateOffer(offer: Offer)
  final case class GetOffer(uuid: String)

  def props: Props = Props[OfferRegistryActor]
}

class OfferRegistryActor extends Actor with ActorLogging with JsonSupport {
  import OfferRegistryActor._
  import context.dispatcher

  private val elastic = HttpClient("elasticsearch://" + sys.env("ELASTIC_HOST") + ":" + sys.env("ELASTIC_PORT") + "?ssl=true")

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def receive: Receive = {
    case GetOffers =>
      sender() ! elastic.execute {
        searchWithType("bot-offer" / "offer")
      }.map({
        case Left(failure) => throw new Exception(failure.toString + " - Cannot get offer")
        case Right(results) => Offers(results.result.hits.hits.map(el => el.to[Offer]).toSeq)
      })

    case CreateOffer(offer) =>
      elastic.execute {
        indexInto("bot-offer" / "offer").doc[Offer](offer.copy(uuid = randomUUID().toString)).refresh(RefreshPolicy.IMMEDIATE)
      }
      sender() ! ActionPerformed(s"User ${offer.uuid} created.")

    case GetOffer(uuid) =>
      sender() ! elastic.execute {
        searchWithType("bot-offer" / "offer") termQuery ("uuid", uuid)
      }.map({
        case Left(failure) => None
        case Right(results) => results.result.hits.hits.map(el => el.to[Offer]).headOption
      })
  }
}
