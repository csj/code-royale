package com.codingame.game

import com.codingame.game.Constants.ENGINEER_MASS
import com.codingame.game.Constants.ENGINEER_RADIUS
import com.codingame.game.Constants.GENERAL_MASS
import com.codingame.game.Constants.GENERAL_RADIUS
import com.codingame.game.Constants.INCOME_TIMER
import com.codingame.game.Constants.KING_MASS
import com.codingame.game.Constants.KING_RADIUS
import com.codingame.game.Constants.OBSTACLE_GAP
import com.codingame.game.Constants.TOWER_HP_INCREMENT
import com.codingame.game.Constants.TOWER_HP_INITIAL
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

    gameManager.players[0].enemyPlayer = gameManager.players[1]
    gameManager.players[1].enemyPlayer = gameManager.players[0]
    gameManager.players[1].inverted = true

    val obstaclePairs = (1..15).map { Pair(Obstacle(entityManager), Obstacle(entityManager)) }
    obstacles = obstaclePairs.flatMap { listOf(it.first, it.second) }

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
      val makeUnit: (Int, Int, Vector2) -> MyOwnedEntity = { radius, mass, location ->
        object : MyOwnedEntity(activePlayer) {
          override val entity = entityManager.createCircle()
            .setRadius(radius)
            .setFillColor(activePlayer.colorToken)

          override val mass = mass

          init {
            this.location = location
          }
        }
      }

      val corner = if (invert) Vector2(1920, 1080) else Vector2.Zero

      activePlayer.kingUnit = makeUnit(KING_RADIUS, KING_MASS, corner)
      activePlayer.engineerUnit = makeUnit(ENGINEER_RADIUS, ENGINEER_MASS, corner)
      activePlayer.generalUnit = makeUnit(GENERAL_RADIUS, GENERAL_MASS, corner)
    }

    allUnits().forEach { it.updateEntity() }
//    fixCollisions(0.0)

    // Params contains all the game parameters that has been to generate this game
    // For instance, it can be a seed number, the size of a grid/map, ...
    return params
  }

  private fun fixCollisions(acceptableGap: Double, entities: List<MyEntity>? = null, dontLoop: Boolean): Boolean {
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

    gameManager.players.forEach {
      val king = it.kingUnit
      val obsK = obstacles.minBy { it.location.distanceTo(king.location) }!!

      // TODO: What if both kings are touching the same one!
      if (obsK.location.distanceTo(king.location) - obsK.radius - king.entity.radius < 10) {
        obsK.incomeOwner = it
        obsK.incomeTimer = INCOME_TIMER
      }

      // TODO: What if both engineers are touching the same one!
      val eng = it.engineerUnit
      val obsE = obstacles.minBy { it.location.distanceTo(eng.location) }!!
      if (obsE.location.distanceTo(eng.location) - obsE.radius - eng.entity.radius < 10) {
        if (obsE.towerOwner == it) {
          obsE.towerHealth += TOWER_HP_INCREMENT
        } else if (obsE.towerOwner == null) {
          obsE.setTower(it, TOWER_HP_INITIAL)
        }
      }
    }
    gameManager.activePlayers.flatMap { it.activeCreeps }.toList().forEach { it.damage(1) }
    obstacles.forEach { it.act() }

    repeat(5) {
      gameManager.activePlayers.flatMap { it.activeCreeps }.forEach { it.move() }
      fixCollisions(0.0, dontLoop = true)
    }

    gameManager.activePlayers.flatMap { it.activeCreeps }.forEach { it.dealDamage() }

    gameManager.activePlayers.forEach {
      if (!it.checkKingHealth()) {
        it.deactivate("Dead king")
      }
    }

    if (gameManager.activePlayers.size < 2) {
      gameManager.endGame()
    }

    allUnits().forEach { it.updateEntity() }

    for (activePlayer in gameManager.activePlayers) {
      activePlayer.units.forEach { activePlayer.printLocation(it.location) }
      activePlayer.sendInputLine("${activePlayer.health} ${activePlayer.resources}")
      activePlayer.enemyPlayer.units.forEach { activePlayer.printLocation(it.location) }
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

    for (player in gameManager.activePlayers) {
      try {
        val outputs = player.outputs
        for ((unit, line) in player.units.zip(outputs)) {
          val toks = line.split(" ")
          when (toks[0]) {
            "MOVE" -> {
              val (x, y) = toks.drop(1).map { Integer.valueOf(it) }
              unit.location = unit.location.towards(Vector2(x, y), UNIT_SPEED.toDouble())
            }
            "SPAWN" -> {
              // TODO: Check if it's the general
              // TODO: Check if enough resources
              // TODO: Ensure it's a proper creep type
              val creepType = CreepType.valueOf(toks[1])
              repeat(creepType.count) {
                player.activeCreeps += when (creepType) {
                  CreepType.ARCHER, CreepType.ZERGLING ->
                    KingChasingCreep(entityManager, player, unit.location, creepType)
                  CreepType.GIANT ->
                    TowerBustingCreep(entityManager, player, unit.location, creepType, obstacles)
                }
              }
              player.resources -= creepType.cost
            }
          }
        }
      } catch (e: AbstractPlayer.TimeoutException) {
        e.printStackTrace()
        player.deactivate("${player.nicknameToken}: timeout!")
      }
    }

    gameManager.setGameSummary(gameManager.players.map { "${it.nicknameToken} Health: ${it.health} Resources: ${it.resources}"})

  }
}

enum class CreepType(val count: Int, val cost: Int, val speed: Int, val range: Int, val radius: Int,
                     val mass: Int, val hp: Int, val assetName: String, val fillAssetName: String) {
  ZERGLING(4, 40, 20, 0,   10, 400,  30,  "bug.png",  "bugfill.png"),
  ARCHER(  2, 70, 13, 200, 15, 900,  45,  "bug2.png", "bug2fill.png"),
  GIANT(   1, 80, 10, 0,   25, 2000, 200, "bug.png",  "bugfill.png")
}

object Constants {
  val UNIT_SPEED = 40
  val TOWER_HP_INITIAL = 200
  val TOWER_HP_INCREMENT = 100
  val TOWER_GENERAL_REPEL_FORCE = 20.0
  val TOWER_CREEP_DAMAGE_RANGE = 6..8

  val GIANT_BUST_RATE = 80
  val INCOME_TIMER = 50

  val OBSTACLE_GAP = 60
  val OBSTACLE_RADIUS_RANGE = 60..110

  val KING_RADIUS = 30
  val ENGINEER_RADIUS = 20
  val GENERAL_RADIUS = 25

  val KING_MASS = 10000
  val ENGINEER_MASS = 6400
  val GENERAL_MASS = 3600

  val KING_HP = 200

  val TOWER_MELT_RATE = 10
  val TOWER_COVERAGE_PER_HP = 1000
}