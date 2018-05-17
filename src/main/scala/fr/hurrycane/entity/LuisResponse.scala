package fr.hurrycane.entity

case class LuisResponse(query: String, topScoringIntent: LuisIntent, intents: Seq[LuisIntent], entities: Seq[LuisEntity])