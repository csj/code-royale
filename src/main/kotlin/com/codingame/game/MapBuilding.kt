package com.codingame.game

import com.codingame.game.Constants.WORLD_HEIGHT
import com.codingame.game.Constants.WORLD_WIDTH
import java.util.*

fun buildMap(theRandom: Random): List<Obstacle> {
  fun IntRange.sample(): Int = theRandom.nextInt(last-first+1) + first

  var obstacles: List<Obstacle>

  loop@ do {
    nextObstacleId = 0

    val obstaclePairs = (1 until Constants.OBSTACLE_PAIRS).map {
      val rate = Constants.OBSTACLE_MINERAL_BASERATE_RANGE.sample()
      val resources = Constants.OBSTACLE_MINERAL_RANGE.sample()
      val radius = Constants.OBSTACLE_RADIUS_RANGE.sample()
      val l1 = Vector2.random(theRandom, WORLD_WIDTH, WORLD_HEIGHT)
      val l2 = Vector2(WORLD_WIDTH, WORLD_HEIGHT) - l1
      Pair(Obstacle(rate, resources, radius, l1), Obstacle(rate, resources, radius, l2))
    }
    obstacles = obstaclePairs.flatMap { listOf(it.first, it.second) }

    for (iter in 1..100) {
      obstaclePairs.forEach { (o1, o2) ->
        val mid = (o1.location + Vector2(Constants.WORLD_WIDTH -o2.location.x, Constants.WORLD_HEIGHT -o2.location.y)) / 2.0
        o1.location = mid
        o2.location = Vector2(Constants.WORLD_WIDTH -mid.x, Constants.WORLD_HEIGHT -mid.y)
      }
      if (!fixCollisions(obstacles, Constants.OBSTACLE_GAP.toDouble(), dontLoop = true)) break@loop
    }
    obstacles.forEach { it.destroy() }
    System.err.println("abandoning")
  } while (true)

  val mapCenter = Vector2(viewportX.length / 2, viewportY.length / 2)
  obstacles.forEach {
    it.location = it.location.snapToIntegers()
    if (it.location.distanceTo(mapCenter) < Constants.OBSTACLE_MINERAL_INCREASE_DISTANCE_1) { it.maxMineralRate++; it.minerals += Constants.OBSTACLE_MINERAL_INCREASE }
    if (it.location.distanceTo(mapCenter) < Constants.OBSTACLE_MINERAL_INCREASE_DISTANCE_2) { it.maxMineralRate++; it.minerals += Constants.OBSTACLE_MINERAL_INCREASE }
    it.updateEntities()
  }
  PlayerHUD.obstacles = obstacles
  return obstacles
}

/**
 * @return true if there is a correction
 */
fun fixCollisions(entities: List<MyEntity>, acceptableGap: Double = 0.0, dontLoop: Boolean = false): Boolean {
  var foundAny = false

  for (iter in 0..999) {
    var loopAgain = false

    for (u1 in entities) {
      val rad = u1.radius.toDouble()
      val clampDist = if (u1.mass == 0) Constants.OBSTACLE_GAP + rad else rad
      u1.location = u1.location.clampWithin(clampDist, Constants.WORLD_WIDTH -clampDist, clampDist, Constants.WORLD_HEIGHT -clampDist)

      for (u2 in entities) {
        if (u1 != u2) {
          val overlap = u1.radius + u2.radius + acceptableGap - u1.location.distanceTo(u2.location)
          if (overlap > 1e-6) {
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

            loopAgain = true
            foundAny = true
          }
        }
      }
    }
    if (dontLoop || !loopAgain) break
  }
  return foundAny
}
