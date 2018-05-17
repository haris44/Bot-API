package fr.hurrycane.tools

import akka.actor.ActorRef
import com.newmotion.akka.rabbitmq._
import fr.hurrycane.entity.LuisIntent

object RabbitFactory {

  val exchange = sys.env("RABBIT_EXCHANGE")
  val messageQueue = sys.env("RABBIT_QUEUE_MESSAGE")
  val messageBinding = sys.env("RABBIT_BINDING_MESSAGE")

  def getFactory = {
    val factory = new ConnectionFactory()

    factory.setUsername(sys.env("RABBIT_USERNAME"))
    factory.setPassword(sys.env("RABBIT_PASSWORD"))
    factory.setHost(sys.env("RABBIT_HOST"))
    factory.setPort(sys.env("RABBIT_PORT").toInt)

    factory
  }

  def setupChannel(channel: Channel, self: ActorRef) {

    channel.queueDeclare(messageQueue, true, false, false, null)
    channel.queueBind(messageQueue, exchange, messageBinding)

  }

  def setupIntentChannel(connectionActor: ActorRef, intent: LuisIntent): ActorRef = {
    def setupChannel(channel: Channel, self: ActorRef) {
      channel.queueDeclare("bot-intent-" + intent.intent, true, false, false, null)
      channel.queueBind("bot-intent-" + intent.intent, exchange, "bot.intent." + intent.intent)
    }
    connectionActor.createChannel(ChannelActor.props(setupChannel))
  }

  def fromBytes(x: Array[Byte]) = new String(x, "UTF-8")

  def toBytes(x: Long) = x.toString.getBytes("UTF-8")

}
