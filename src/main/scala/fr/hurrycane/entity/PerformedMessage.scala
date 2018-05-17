package fr.hurrycane.entity

case class PerformedMessage(content: String, timestamp: Long, intent: LuisIntent, mood: String, conversationId: String)
