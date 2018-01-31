package com.codingame.game

import com.codingame.game.Constants.WORLD_HEIGHT
import com.codingame.game.Constants.WORLD_WIDTH
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

lateinit var theRandom: Random

@Suppress("unused")  // injected by magic
class Referee : AbstractReferee() {
  @Inject private lateinit var gameManager: GameManager<Player>
  @Inject private lateinit var entityManager: GraphicEntityModule

  private var obstacles: List<Obstacle> = listOf()

  private fun allUnits(): List<MyEntity> = gameManager.players.flatMap { it.allUnits() } + obstacles

  override fun init(params: Properties): Properties {
    theRandom = (params["seed"] as? Long)?.let { Random(it) } ?: Random()

    theEntityManager = entityManager

    gameManager.players[0].enemyPlayer = gameManager.players[1]
    gameManager.players[1].enemyPlayer = gameManager.players[0]
    gameManager.players[1].inverted = true

    val obstaclePairs = (1..15).map {
      val rate = (1..5).sample()
      Pair(Obstacle(rate), Obstacle(rate))
    }
    obstacles = obstaclePairs.flatMap { listOf(it.first, it.second) }

    do {
      obstaclePairs.forEach { (o1, o2) ->
        val mid = (o1.location + Vector2(WORLD_WIDTH -o2.location.x, WORLD_HEIGHT -o2.location.y)) / 2.0
        o1.location = mid
        o2.location = Vector2(WORLD_WIDTH -mid.x, WORLD_HEIGHT -mid.y)
        o2.radius = o1.radius
      }

    } while (fixCollisions(OBSTACLE_GAP.toDouble(), obstacles, dontLoop = true))

    obstacles.forEach { it.updateEntities() }

    for ((activePlayer, invert) in gameManager.activePlayers.zip(listOf(false, true))) {
      val spawnDistance = 200
      val corner = if (invert)
        Vector2(WORLD_WIDTH - spawnDistance, WORLD_HEIGHT - spawnDistance)
      else
        Vector2(spawnDistance, spawnDistance)

      activePlayer.kingUnit = King(activePlayer).also { it.location = corner }
    }

    fixCollisions()
//    allUnits().forEach { it.updateEntity() }

    gameManager.activePlayers.forEach { player ->
      player.sendInputLine(obstacles.size.toString())
      obstacles.forEach { player.printObstacleInit(it) }
    }

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
        val rad = u1.radius.toDouble()
        val clampDist = if (u1.mass == 0) OBSTACLE_GAP + rad else rad
        u1.location = u1.location.clampWithin(clampDist, WORLD_WIDTH -clampDist, clampDist, WORLD_HEIGHT -clampDist)

        for (u2 in allUnits) {
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

  override fun gameTurn(turn: Int) {
    // 1. Send game states
    for (activePlayer in gameManager.activePlayers) {
      activePlayer.sendInputLine("${activePlayer.kingUnit.location.x.toInt()} ${activePlayer.kingUnit.location.y.toInt()} ${activePlayer.health} ${activePlayer.resources}")
      activePlayer.sendInputLine("${activePlayer.enemyPlayer.kingUnit.location.x.toInt()} ${activePlayer.enemyPlayer.kingUnit.location.y.toInt()} ${activePlayer.enemyPlayer.health} ${activePlayer.enemyPlayer.resources}")
      obstacles.forEach { activePlayer.printObstaclePerTurn(it) }

      for (player in listOf(activePlayer, activePlayer.enemyPlayer)) {
        activePlayer.sendInputLine(player.activeCreeps.size.toString())
        player.activeCreeps.forEach {
          activePlayer.sendInputLine("${it.location.x.toInt()} ${it.location.y.toInt()} ${it.health} ${it.creepType.ordinal}")
        }
      }
      activePlayer.execute()
    }

    // 2. Process player actions

    val obstaclesAttemptedToBuildUpon = mutableListOf<Obstacle>()
    val structuresToBuild = mutableListOf<()->Unit>()
    class PlayerInputException(message: String): Exception(message)

    playerLoop@ for (player in gameManager.activePlayers) {
      try {
        // Process building creeps
        val buildingBarracks = player.outputs[1].split(" ").drop(1)
          .map { obsIdStr -> obsIdStr.toIntOrNull() ?: throw PlayerInputException("Couldn't process obstacleId: $obsIdStr") }
          .map { obsId -> obstacles.find { it.obstacleId == obsId } ?: throw PlayerInputException("No obstacle with id = $obsId") }
          .map { obs ->
            val struc = obs.structure as? Barracks ?: throw PlayerInputException("Cannot spawn from ${obs.obstacleId}: not a barracks")
            if (struc.owner != player) throw PlayerInputException("Cannot spawn from ${obs.obstacleId}: not owned")
            if (struc.isTraining) throw PlayerInputException("Barracks ${obs.obstacleId} is training")
            struc
          }

        val sum = buildingBarracks.sumBy { it.creepType.cost }
        if (sum > player.resources) throw PlayerInputException("Building too many creeps")

        player.resources -= sum
        buildingBarracks.forEach { barracks ->
          barracks.progress = 0
          barracks.isTraining = true
          barracks.onComplete = {
            repeat(barracks.creepType.count) {
              player.activeCreeps += when (barracks.creepType) {
                CreepType.RANGED, CreepType.MELEE ->
                  KingChasingCreep(barracks.owner, barracks.creepType)
                CreepType.GIANT ->
                  TowerBustingCreep(barracks.owner, barracks.creepType, obstacles)
              }.also { it.location = barracks.obstacle.location }
            }
          }
        }

        val line = player.outputs[0]
        val toks = line.split(" ").iterator()
        val command = toks.next()
        when (command) {
          "WAIT" -> { }
          "MOVE" -> {
            val x = toks.next().toInt()
            val y = toks.next().toInt()
            player.kingUnit.location = player.kingUnit.location.towards(Vector2(x, y), UNIT_SPEED.toDouble())
          }
          "MOVEOBSTACLE" -> {
            val obsId = toks.next().toInt()
            val obs = obstacles.find { it.obstacleId == obsId } ?: throw PlayerInputException("ObstacleId $obsId does not exist")
            player.kingUnit.location = player.kingUnit.location.towards(obs.location, UNIT_SPEED.toDouble())
          }
          "BUILD" -> {
            val king = player.kingUnit
            val obsK = obstacles.minBy { it.location.distanceTo(king.location) - it.radius }!!

            val dist = obsK.location.distanceTo(king.location) - king.radius - obsK.radius
            if (dist > 10) throw PlayerInputException("Cannot build: too far away ($dist)")

            val struc = obsK.structure
            if (struc?.owner == player.enemyPlayer) throw PlayerInputException("Cannot build: owned by enemy player")
            if (struc is Barracks && struc.owner == player && struc.progress > 0)
              throw PlayerInputException("Cannot rebuild: training is in progress")

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
          else -> throw PlayerInputException("Didn't understand command: $command")
        }
      } catch (e: AbstractPlayer.TimeoutException) {
        e.printStackTrace()
        player.deactivate("${player.nicknameToken}: timeout!")
      } catch (e: PlayerInputException) {
        e.printStackTrace()
        player.deactivate(e.message)
      }
    }

    // If they're both building onto the same one, then actually build neither
    if (obstaclesAttemptedToBuildUpon.size == 2 && obstaclesAttemptedToBuildUpon[0] == obstaclesAttemptedToBuildUpon[1])
      structuresToBuild.clear()

    // Execute builds that remain
    structuresToBuild.forEach { it.invoke() }

    // 3. Creeps move and deal damage
    val allCreeps = gameManager.activePlayers.flatMap { it.activeCreeps }.toList()
    allCreeps.forEach { it.damage(1) }
    repeat(5) {
      allCreeps.forEach { it.move() }
      fixCollisions(dontLoop = true)
    }
    allCreeps.forEach { it.dealDamage() }

    // Tear down enemy mines
    allCreeps.forEach { creep ->
      val closestObstacle = obstacles.minBy { it.location.distanceTo(creep.location) }!!
      if (closestObstacle.location.distanceTo(creep.location) - closestObstacle.radius - creep.radius > 5) return@forEach
      val struc = closestObstacle.structure
      if (struc is Mine && struc.owner != creep.owner) closestObstacle.structure = null
    }

    // 4. Structures work
    gameManager.players.forEach { player ->
      obstacles
        .filter { it.structure?.owner == player }
        .forEach { it.act() }
    }

//    allUnits().forEach { it.updateEntity() }

    // 5. Check end game
    gameManager.activePlayers.forEach { it.checkKingHealth() }
    if (gameManager.activePlayers.size < 2) {
      gameManager.endGame()
    }

    gameManager.players.forEach {
      gameManager.addToGameSummary("${it.nicknameToken} Health: ${it.health} Resources: ${it.resources}")
    }
  }
}

