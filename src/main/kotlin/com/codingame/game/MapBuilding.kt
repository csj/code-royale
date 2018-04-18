package com.codingame.game

import com.codingame.game.Constants.WORLD_HEIGHT
import com.codingame.game.Constants.WORLD_WIDTH
import java.util.*

val background = theEntityManager.createSprite()
  .setImage("Background.jpg")
  .setBaseWidth(Constants.WORLD_WIDTH).setBaseHeight(Constants.WORLD_HEIGHT)
  .setX(viewportX.first).setY(viewportY.first)
  .setZIndex(0)

val hudBackground = theEntityManager.createSprite()
  .setImage("Hud.png")
  .setBaseWidth(1920)
  .setX(0).setY(1080)
  .setAnchorY(1.0)
  .setZIndex(4000)

fun IntRange.sample(): Int = theRandom.nextInt(last-first+1) + first

fun buildMap(): List<Obstacle> {
  fun buildObstacles(): List<Obstacle>? {
    nextObstacleId = 0

    val obstaclePairs = (1..Leagues.obstacles).map {
      val rate = Constants.OBSTACLE_MINE_BASESIZE_RANGE.sample()
      val gold = Constants.OBSTACLE_GOLD_RANGE.sample()
      val radius = Constants.OBSTACLE_RADIUS_RANGE.sample()
      val l1 = Vector2.random(theRandom, WORLD_WIDTH, WORLD_HEIGHT)
      val l2 = Vector2(WORLD_WIDTH, WORLD_HEIGHT) - l1
      Pair(Obstacle(rate, gold, radius, l1), Obstacle(rate, gold, radius, l2))
    }
    val obstacles = obstaclePairs.flatMap { listOf(it.first, it.second) }

    if ((1..100).all {
      obstaclePairs.forEach { (o1, o2) ->
        val mid = (o1.location + Vector2(Constants.WORLD_WIDTH -o2.location.x, Constants.WORLD_HEIGHT -o2.location.y)) / 2.0
        o1.location = mid
        o2.location = Vector2(Constants.WORLD_WIDTH -mid.x, Constants.WORLD_HEIGHT -mid.y)
      }
      collisionCheck(obstacles, Constants.OBSTACLE_GAP.toDouble())
    }) {
      return obstacles
    }

    return obstacles
  }

  var obstacles: List<Obstacle>?
  do { obstacles = buildObstacles();  } while (obstacles == null)

  val mapCenter = Vector2(viewportX.length / 2, viewportY.length / 2)
  obstacles.forEach {
    it.location = it.location.snapToIntegers()
    if (it.location.distanceTo(mapCenter) < Constants.OBSTACLE_GOLD_INCREASE_DISTANCE_1) { it.maxMineSize++; it.gold += Constants.OBSTACLE_GOLD_INCREASE }
    if (it.location.distanceTo(mapCenter) < Constants.OBSTACLE_GOLD_INCREASE_DISTANCE_2) { it.maxMineSize++; it.gold += Constants.OBSTACLE_GOLD_INCREASE }
    it.updateEntities()
  }
  PlayerHUD.obstacles = obstacles
  return obstacles
}

fun fixCollisions(entities: List<FieldObject>, maxIterations: Int = 999) {
  repeat(maxIterations) { if (!collisionCheck(entities)) return }
}

/**
 * @return false if everything is ok; true if there was a correction
 */
fun collisionCheck(entities: List<FieldObject>, acceptableGap: Double = 0.0): Boolean {
  return entities.flatMap { u1 ->
    val rad = u1.radius.toDouble()
    val clampDist = if (u1.mass == 0) Constants.OBSTACLE_GAP + rad else rad
    u1.location = u1.location.clampWithin(clampDist, WORLD_WIDTH - clampDist, clampDist, WORLD_HEIGHT - clampDist)

    (entities-u1).map { u2 ->
      val overlap = u1.radius + u2.radius + acceptableGap - u1.location.distanceTo(u2.location).toDouble  // TODO: Fix this?
      if (overlap <= 1e-6) {
        false
      }
      else {
        val (d1, d2) = when {
          u1.mass == 0 && u2.mass == 0 -> Pair(0.5, 0.5)
          u1.mass == 0 -> Pair(0.0, 1.0)
          u2.mass == 0 -> Pair(1.0, 0.0)
          else -> Pair(u2.mass.toDouble() / (u1.mass + u2.mass), u1.mass.toDouble() / (u1.mass + u2.mass))
        }

        val u1tou2 = u2.location - u1.location
        val gap = if (u1.mass == 0 && u2.mass == 0) 20.0 else 1.0

        u1.location -= u1tou2.resizedTo(d1 * overlap + if (u1.mass == 0 && u2.mass > 0) 0.0 else gap)
        u2.location += u1tou2.resizedTo(d2 * overlap + if (u2.mass == 0 && u1.mass > 0) 0.0 else gap)
        true
      }
    }
  }.toList().any { it }
}
