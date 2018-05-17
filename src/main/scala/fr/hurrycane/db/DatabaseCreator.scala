package fr.hurrycane.db

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global

object DatabaseCreator {

  private val elastic = HttpClient("elasticsearch://" + sys.env("ELASTIC_HOST") + ":" + sys.env("ELASTIC_PORT") + "?ssl=true")

  def checkAll(): Unit = {

    elastic.execute {
      searchWithType("bot-user" / "user")
    }.map({
      case Left(failure) => createUserIndex()
      case Right(results) => results
    }).await

    elastic.execute {
      searchWithType("bot-message" / "message")
    }.map({
      case Left(failure) => createMessageIndex()
      case Right(results) => results
    }).await

    elastic.execute {
      searchWithType("bot-offer" / "offer")
    }.map({
      case Left(failure) => createOfferIndex()
      case Right(results) => results
    }).await

  }

  def createUserIndex() = {
    elastic.execute {
      createIndex("bot-user").mappings(
        mapping("user") as (
          textField("nom"),
          textField("prenom"),
          textField("userid")))
    }.map({
      case Left(failure) => throw new Exception("Cannot populate user ")
      case Right(results) => results
    })
  }

  def createMessageIndex() = {
    elastic.execute {
      createIndex("bot-message").mappings(
        mapping("message") as (
          textField("nom"),
          textField("prenom"),
          textField("userid")))
    }.map({
      case Left(failure) => throw new Exception("Cannot populate user ")
      case Right(results) => results
    })

  }

  def createOfferIndex() = {
    elastic.execute {
      createIndex("bot-offer").mappings(
        mapping("offer") as (
          textField("uuid"),
          dateField("timestamp"),
          textField("typeOffer"),
          textField("domain"),
          textField("localisation"),
          textField("url"),
          textField("title")))
    }.map({
      case Left(failure) => throw new Exception("Cannot populate offer ")
      case Right(results) => results
    })

  }

}
