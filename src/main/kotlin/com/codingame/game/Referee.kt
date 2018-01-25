package com.codingame.game

import com.codingame.game.Constants.KING_RADIUS
import com.codingame.game.Constants.OBSTACLE_GAP
import com.codingame.game.Constants.TOWER_HP_INCREMENT
import com.codingame.game.Constants.TOWER_HP_INITIAL
import com.codingame.game.Constants.TOWER_HP_MAXIMUM
import com.codingame.game.Constants.UNIT_SPEED
import com.codingame.gameengine.core.AbstractPlayer
import com.codingame.gameengine.core.AbstractReferee
import com.codingame.gameengine.core.GameManager
import com.codingame.gameengine.module.entities.GraphicEntityModule
import com.google.inject.Inject
import java.util.*

@Suppress("unused")  // injected by magic
class Referee : AbstractReferee() {
  @Inject private lateinit var gameManager: GameManager<Player>
  @Inject private lateinit var entityManager: GraphicEntityModule

  private var obstacles: List<Obstacle> = listOf()

  private fun allUnits(): List<MyEntity> = gameManager.players.flatMap { it.allUnits() } + obstacles

  override fun init(params: Properties): Properties {
    theEntityManager = entityManager

    gameManager.players[0].enemyPlayer = gameManager.players[1]
    gameManager.players[1].enemyPlayer = gameManager.players[0]
    gameManager.players[1].inverted = true

    val obstaclePairs = (1..15).map {
      val rate = (1..5).sample()
      Pair(Obstacle(rate), Obstacle(rate))
    }
    obstacles = obstaclePairs.flatMap { listOf(it.first, it.second) }
    Barracks.allObstacles = obstacles

    do {
      obstaclePairs.forEach { (o1, o2) ->
        val mid = (o1.location + Vector2(1920-o2.location.x, 1080-o2.location.y)) / 2.0
        o1.location = mid
        o2.location = Vector2(1920-mid.x, 1080-mid.y)
        o2.radius = o1.radius
        o2.entity.radius = o1.entity.radius
      }

    } while (fixCollisions(OBSTACLE_GAP.toDouble(), obstacles, dontLoop = true))

//    obstacles = (0..29).map { Obstacle(entityManager) }
//    fixCollisions(OBSTACLE_GAP.toDouble(), obstacles)

    obstacles.forEach { it.updateEntities() }

    for ((activePlayer, invert) in gameManager.activePlayers.zip(listOf(false, true))) {
      val corner = if (invert) Vector2(1920- KING_RADIUS, 1080- KING_RADIUS) else Vector2(KING_RADIUS, KING_RADIUS)
      activePlayer.kingUnit = King(activePlayer).also { it.location = corner }
    }

    allUnits().forEach { it.updateEntity() }
    fixCollisions()

    // Params contains all the game parameters that has been to generate this game
    // For instance, it can be a seed number, the size of a grid/map, ...
    return params
  }

  private fun fixCollisions(acceptableGap: Double = 0.0, entities: List<MyEntity>? = null, dontLoop: Boolean = false): Boolean {
    val allUnits = entities ?: allUnits()
    var foundAny = false

    for (iter in 0..999) {
      var loopAgain = false

      for (u1 in allUnits) {
        val rad = u1.entity.radius.toDouble()
        val clampDist = if (u1.mass == 0) OBSTACLE_GAP + rad else rad
        u1.location = u1.location.clampWithin(clampDist, 1920-clampDist, clampDist, 1080-clampDist)

        for (u2 in allUnits) {
          if (u1 != u2) {
            val overlap = u1.entity.radius + u2.entity.radius + acceptableGap - u1.location.distanceTo(u2.location)
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

  override fun gameTurn(turn: Int) {
    // TODO: Rearrange turn order so that visual state matches what bot sees
    // TODO: Remove location inversion

    // 2. Creeps move and deal damage
    val allCreeps = gameManager.activePlayers.flatMap { it.activeCreeps }.toList()
    allCreeps.forEach { it.damage(1) }

    repeat(5) {
      allCreeps.forEach { it.move() }
      fixCollisions(dontLoop = true)
    }

    allCreeps.forEach { it.dealDamage() }

    allCreeps.forEach { creep ->
      val closestObstacle = obstacles.minBy { it.location.distanceTo(creep.location) }!!
      if (closestObstacle.location.distanceTo(creep.location) - closestObstacle.radius - creep.entity.radius > 5) return@forEach
      val struc = closestObstacle.structure
      if (struc is Mine && struc.owner != creep.owner) closestObstacle.structure = null
    }

    // 1. Existing structures work and barracks spawn new creeps
    gameManager.players.forEach { player ->
      val resourcesPerBarracks = {
        val numBarracks = obstacles.count { it.structure is Barracks && (it.structure as Barracks).owner == player }
        if (numBarracks == 0) 0 else player.resources / numBarracks
      }()

      obstacles
        .filter { it.structure?.owner == player }
        .forEach { it.act(resourcesPerBarracks)?.let { player.activeCreeps += it } }
    }

    allUnits().forEach { it.updateEntity() }

    // 3. Check end game
    gameManager.activePlayers.forEach {
      if (!it.checkKingHealth()) {
        it.deactivate("Dead king")
      }
    }

    if (gameManager.activePlayers.size < 2) {
      gameManager.endGame()
    }

    // 4. Send game states
    for (activePlayer in gameManager.activePlayers) {
      activePlayer.printLocation(activePlayer.kingUnit.location)
      activePlayer.sendInputLine("${activePlayer.health} ${activePlayer.resources}")
      activePlayer.printLocation(activePlayer.enemyPlayer.kingUnit.location)
      activePlayer.sendInputLine("${activePlayer.enemyPlayer.health} ${activePlayer.enemyPlayer.resources}")
      activePlayer.sendInputLine(obstacles.size.toString())
      obstacles.forEach { activePlayer.printObstacle(it) }

      for (player in listOf(activePlayer, activePlayer.enemyPlayer)) {
        activePlayer.sendInputLine(player.activeCreeps.size.toString())
        player.activeCreeps.forEach {
          activePlayer.sendInputLine("${it.location.x.toInt()} ${it.location.y.toInt()} ${it.health} ${it.creepType.ordinal}")
        }
      }
      activePlayer.execute()
    }

    // 5. Process player actions

    val obstaclesAttemptedToBuildUpon = mutableListOf<Obstacle>()
    val structuresToBuild = mutableListOf<()->Unit>()

    playerLoop@ for (player in gameManager.activePlayers) {
      try {
        val line = player.outputs[0]
        val toks = line.split(" ").iterator()
        when (toks.next()) {
          "MOVE" -> {
            val x = toks.next().toInt()
            val y = toks.next().toInt()
            player.kingUnit.location = player.kingUnit.location.towards(Vector2(x, y), UNIT_SPEED.toDouble())
          }
          "BUILD" -> {
            val king = player.kingUnit
            val obsK = obstacles.minBy { it.location.distanceTo(king.location) - it.radius }!!
            val dist = obsK.location.distanceTo(king.location) - king.entity.radius - obsK.radius
            if (dist > 10) {
              player.deactivate("Cannot build: too far away ($dist)")
              continue@playerLoop
            }
            val struc = obsK.structure

            if (struc?.owner == player.enemyPlayer) {
              player.deactivate("Cannot build: owned by enemy player")
              continue@playerLoop
            }

            obstaclesAttemptedToBuildUpon += obsK
            structuresToBuild += {
              when (toks.next()) {
                "MINE" -> obsK.setMine(player)
                "TOWER" -> {
                  if (struc is Tower) {
                    struc.health += TOWER_HP_INCREMENT
                    if (struc.health > TOWER_HP_MAXIMUM) struc.health = TOWER_HP_MAXIMUM
                  } else {
                    obsK.setTower(player, TOWER_HP_INITIAL)
                  }
                }
                "BARRACKS" -> {
                  val creepType = CreepType.valueOf(toks.next())
                  obsK.setBarracks(player, creepType)
                }
              }
            }
          }
        }
      } catch (e: AbstractPlayer.TimeoutException) {
        e.printStackTrace()
        player.deactivate("${player.nicknameToken}: timeout!")
      }
    }

    // If they're both building onto the same one, then actually build neither
    if (obstaclesAttemptedToBuildUpon.size == 2 && obstaclesAttemptedToBuildUpon[0] == obstaclesAttemptedToBuildUpon[1])
      structuresToBuild.clear()

    // Execute builds that remain
    structuresToBuild.forEach { it.invoke() }

    gameManager.players.forEach {
      gameManager.addToGameSummary("${it.nicknameToken} Health: ${it.health} Resources: ${it.resources}")
    }
  }
}

enum class CreepType(val count: Int, val cost: Int, val speed: Int, val range: Int, val radius: Int,
                     val mass: Int, val hp: Int, val assetName: String, val fillAssetName: String) {
  ZERGLING(4, 120, 20, 0,   10, 400,  30,  "bug.png",  "bugfill.png"),
  ARCHER(  2, 210, 13, 200, 15, 900,  45,  "archer.png", "archer-fill.png"),
  GIANT(   1, 240, 10, 0,   25, 2000, 200, "bulldozer.png", "bulldozer-fill.png")
}

object Constants {
  val UNIT_SPEED = 60
  val TOWER_HP_INITIAL = 200
  val TOWER_HP_INCREMENT = 100
  val TOWER_HP_MAXIMUM = 800
  val TOWER_GENERAL_REPEL_FORCE = 20.0
  val TOWER_CREEP_DAMAGE_RANGE = 6..8

  val GIANT_BUST_RATE = 80
  val INCOME_TIMER = 50

  val OBSTACLE_GAP = 60
  val OBSTACLE_RADIUS_RANGE = 60..110

  val KING_RADIUS = 20
  val ENGINEER_RADIUS = 20
  val GENERAL_RADIUS = 25

  val KING_MASS = 10000
  val ENGINEER_MASS = 6400
  val GENERAL_MASS = 3600

  val KING_HP = 200

  val TOWER_MELT_RATE = 10
  val TOWER_COVERAGE_PER_HP = 1000
}