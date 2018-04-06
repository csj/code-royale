package com.codingame.game

import com.codingame.game.Constants.WORLD_HEIGHT
import com.codingame.game.Constants.WORLD_WIDTH
import com.codingame.game.Constants.TOUCHING_DELTA
import com.codingame.game.Constants.TOWER_HP_INCREMENT
import com.codingame.game.Constants.TOWER_HP_INITIAL
import com.codingame.game.Constants.TOWER_HP_MAXIMUM
import com.codingame.game.Constants.WOOD_FIXED_INCOME
import com.codingame.gameengine.core.AbstractPlayer
import com.codingame.gameengine.core.AbstractReferee
import com.codingame.gameengine.core.GameManager
import com.codingame.gameengine.module.entities.GraphicEntityModule
import tooltipModule.TooltipModule
import com.google.inject.Inject
import java.util.*
import kotlin.math.roundToInt

@Suppress("unused")  // injected by magic
class Referee : AbstractReferee() {
  @Inject private lateinit var gameManager: GameManager<Player>
  @Inject private lateinit var entityManager: GraphicEntityModule
  @Inject private lateinit var tooltipModule: TooltipModule

  private var obstacles: List<Obstacle> = listOf()

  private fun allEntities(): List<MyEntity> = gameManager.players.flatMap { it.allUnits() } + obstacles

  override fun init(params: Properties): Properties {

    theEntityManager = entityManager
    theTooltipModule = tooltipModule
    theGameManager = gameManager
    theGameManager.maxTurns = 250

    when (gameManager.leagueLevel) {
      1 -> { Leagues.mines = false; Leagues.fixedIncome = WOOD_FIXED_INCOME; Leagues.towers = false; Leagues.giants = false }
      2 -> { Leagues.mines = false; Leagues.fixedIncome = WOOD_FIXED_INCOME }
      else -> { }
    }

    gameManager.frameDuration = 750

    gameManager.players[0].enemyPlayer = gameManager.players[1]
    gameManager.players[1].enemyPlayer = gameManager.players[0]
    gameManager.players[1].isSecondPlayer = true

    val theRandom = (params["seed"] as? String)?.let { Random(it.toLong()) } ?: Random()
    obstacles = buildMap(theRandom)

    for ((activePlayer, invert) in gameManager.activePlayers.zip(listOf(false, true))) {
      val spawnDistance = 200
      val corner = if (invert)
        Vector2(WORLD_WIDTH - spawnDistance, WORLD_HEIGHT - spawnDistance)
      else
        Vector2(spawnDistance, spawnDistance)

      activePlayer.queenUnit = Queen(activePlayer).also { it.location = corner }
    }

    fixCollisions(allEntities())

    gameManager.activePlayers.forEach { it.hud.update() }

    gameManager.activePlayers.forEach { player ->
      player.sendInputLine(obstacles.size.toString())
      obstacles.forEach { player.printObstacleInit(it) }
    }

    // Params contains all the game parameters that has been to generate this game
    // For instance, it can be a seed number, the size of a grid/map, ...
    return params
  }

  override fun gameTurn(turn: Int) {
    fun sendGameStates() {
      for (activePlayer in gameManager.activePlayers) {
        activePlayer.sendInputLine("${activePlayer.resources}")
        obstacles.forEach { activePlayer.printObstaclePerTurn(it) }

        val units = gameManager.activePlayers.flatMap { it.activeCreeps + it.queenUnit }
        activePlayer.sendInputLine(units.size.toString())
        units.forEach {
          val toks = listOf(it.location.x.roundToInt(), it.location.y.roundToInt(),
            if (it.owner == activePlayer) 0 else 1) +
            when(it) {
              is Queen -> listOf(-1, it.owner.health)
              is Creep -> listOf(it.creepType.ordinal, it.health)
              else -> throw IllegalArgumentException("Unrecognized entity type: $it")
            }

          activePlayer.sendInputLine(toks.joinToString(" "))
        }
        activePlayer.execute()
      }
    }

    fun processPlayerActions() {
      val obstaclesAttemptedToBuildUpon = mutableListOf<Obstacle>()
      val scheduledBuildings = mutableListOf<Pair<Player, ()->Unit>>()
      class PlayerInputException(message: String): Exception(message)
      class PlayerInputWarning(message: String): Exception(message)

      fun scheduleBuilding(player: Player, obs: Obstacle, strucType: String) {
        val struc = obs.structure
        if (struc?.owner == player.enemyPlayer) throw PlayerInputWarning("Cannot build: owned by enemy player")
        if (struc is Barracks && struc.owner == player && struc.progress > 0)
          throw PlayerInputWarning("Cannot rebuild: training is in progress")

        obstaclesAttemptedToBuildUpon += obs
        val toks = strucType.split('-').iterator()

        scheduledBuildings += player to {
          if (!toks.hasNext()) throw PlayerInputException("Structure type must be specified")
          val firstToken = toks.next()
          when {
            firstToken == "MINE" && Leagues.mines ->
              if (struc is Mine) {
                struc.incomeRate++
                if (struc.incomeRate > obs.maxMineralRate) struc.incomeRate = obs.maxMineralRate
              } else {
                obs.setMine(player)
              }
            firstToken == "TOWER" && Leagues.towers -> {
              if (struc is Tower) {
                struc.health += TOWER_HP_INCREMENT
                if (struc.health > TOWER_HP_MAXIMUM) struc.health = TOWER_HP_MAXIMUM
              } else {
                obs.setTower(player, TOWER_HP_INITIAL)
              }
            }
            firstToken == "BARRACKS" -> {
              if (!toks.hasNext()) throw PlayerInputException("BARRACKS type must be specified")
              val creepInputType = toks.next()
              val creepType = try {
                CreepType.valueOf(creepInputType)
                  .also { if (!Leagues.giants && it == CreepType.GIANT) throw Exception("GIANTS")}
              } catch (e:Exception) {
                throw PlayerInputException("Invalid BARRACKS type: $creepInputType")
              }
              obs.setBarracks(player, creepType)
            }
            else -> throw PlayerInputException("Invalid structure type: $firstToken")
          }
        }
      }

      playerLoop@ for (player in gameManager.activePlayers) {
        val queen = player.queenUnit
        try {
          try {
            val toks = player.outputs[1].split(" ")
            if (toks[0] != "TRAIN") throw PlayerInputException("Expected TRAIN on the second line")

            // Process building creeps
            val buildingBarracks = toks.drop(1)
              .map { obsIdStr -> obsIdStr.toIntOrNull() ?: throw PlayerInputException("Couldn't process obstacleId: $obsIdStr") }
              .map { obsId -> obstacles.find { it.obstacleId == obsId } ?: throw PlayerInputException("No obstacle with id = $obsId") }
              .map { obs ->
                val struc = obs.structure as? Barracks ?: throw PlayerInputWarning("Cannot spawn from ${obs.obstacleId}: not a barracks")
                if (struc.owner != player) throw PlayerInputWarning("Cannot spawn from ${obs.obstacleId}: not owned")
                if (struc.isTraining) throw PlayerInputWarning("Barracks ${obs.obstacleId} is training")
                struc
              }

            if (buildingBarracks.size > buildingBarracks.toSet().size)
              throw PlayerInputWarning("Training from some barracks more than once")

            val sum = buildingBarracks.sumBy { it.creepType.cost }
            if (sum > player.resources) throw PlayerInputWarning("Training too many creeps ($sum total resources requested)")

            player.resources -= sum
            buildingBarracks.forEach { barracks ->
              barracks.progress = 0
              barracks.isTraining = true
              barracks.onComplete = {
                repeat(barracks.creepType.count) { iter ->
                  player.activeCreeps += when (barracks.creepType) {
                    CreepType.MELEE ->
                      MeleeCreep(barracks.owner, barracks.creepType)
                    CreepType.RANGED ->
                      RangedCreep(barracks.owner, barracks.creepType)
                    CreepType.GIANT ->
                      GiantCreep(barracks.owner, barracks.creepType, obstacles)
                  }.also {
                    it.location = barracks.obstacle.location + Vector2(iter, iter)
                    it.finalizeFrame()
                    it.location = it.location.towards(barracks.owner.enemyPlayer.queenUnit.location, 30.0)
                    it.finalizeFrame()
                    it.commitState(0.0)
                  }
                }
                fixCollisions(allEntities())
              }
            }
          }
          catch (e: PlayerInputWarning) {
            gameManager.addToGameSummary("WARNING: ${e.message}")
          }

          // Process queen command
          try {
            val line = player.outputs[0]
            val toks = line.split(" ").iterator()
            val command = toks.next()

            when (command) {
              "WAIT" -> {
              }
              "MOVE" -> {
                try {
                  val x = toks.next().toInt()
                  val y = toks.next().toInt()
                  queen.moveTowards(Vector2(x, y))
                } catch (e: Exception) {
                  throw PlayerInputException("In MOVE command, x and y must be integers")
                }
              }
              "BUILD" -> {
                val obsId = try { toks.next().toInt() } catch (e:Exception) { throw PlayerInputException("Could not parse obstacleId")}
                val obs = obstacles.find { it.obstacleId == obsId } ?: throw PlayerInputException("ObstacleId $obsId does not exist")
                val strucType = toks.next()

                val dist = obs.location.distanceTo(queen.location)
                if (dist < queen.radius + obs.radius + TOUCHING_DELTA) {
                  scheduleBuilding(player, obs, strucType)
                } else {
                  queen.moveTowards(obs.location)
                }
              }
              else -> throw PlayerInputException("Didn't understand command: $command")
            }
          } catch (e: PlayerInputWarning) {
            gameManager.addToGameSummary("WARNING: ${e.message}")
          }
        } catch (e: AbstractPlayer.TimeoutException) {
          e.printStackTrace()
          player.kill("Timeout!")
        } catch (e: PlayerInputException) {
          System.err.println("WARNING: Terminating ${player.nicknameToken}, because of:")
          e.printStackTrace()
          player.kill("${e.message}")
        }
      }

      // If they're both building onto the same one, then actually build only one: depending on parity of the turn number
      if (obstaclesAttemptedToBuildUpon.size == 2 && obstaclesAttemptedToBuildUpon[0] == obstaclesAttemptedToBuildUpon[1]) {
        scheduledBuildings.removeAt(turn % 2)
      }

      // Execute builds that remain
      scheduledBuildings.forEach { (player: Player, callback: () -> Unit) ->
        try { callback.invoke() }
        catch (e: PlayerInputException) {
          System.err.println("WARNING: Deactivating ${player.nicknameToken} because of:")
          e.printStackTrace()
          player.kill("${e.message}")
        }
      }
    }

    fun processCreeps() {
      val allCreeps = gameManager.activePlayers.flatMap { it.activeCreeps }.sortedBy { it.creepType }.toList()
      repeat(5) {
        allCreeps.forEach { it.move(1.0 / 5) }
        fixCollisions(allEntities(), 1)
      }
      allCreeps.forEach { it.dealDamage() }

      // Tear down enemy mines
      allCreeps.forEach { creep ->
        val closestObstacle = obstacles.minBy { it.location.distanceTo(creep.location) }!!
        if (closestObstacle.location.distanceTo(creep.location) >= closestObstacle.radius + creep.radius + TOUCHING_DELTA) return@forEach
        val struc = closestObstacle.structure
        if (struc is Mine && struc.owner != creep.owner) closestObstacle.structure = null
      }

      allCreeps.forEach { it.damage(1) }
      allCreeps.forEach { it.finalizeFrame() }

      // Queens tear down enemy structures (not TOWERs)
      gameManager.activePlayers.forEach {
        val queen = it.queenUnit
        val closestObstacle = obstacles.minBy { it.location.distanceTo(queen.location) }!!
        if (closestObstacle.location.distanceTo(queen.location) >= closestObstacle.radius + queen.radius + TOUCHING_DELTA) return@forEach
        val struc = closestObstacle.structure
        if ((struc is Mine || struc is Barracks) && struc.owner != queen.owner) closestObstacle.structure = null
      }
    }

    gameManager.activePlayers.forEach { it.resourcesPerTurn = 0 }

    sendGameStates()
    processPlayerActions()
    processCreeps()

    // Process structures
    obstacles.forEach { it.act() }

    Leagues.fixedIncome?.also { income ->
      gameManager.activePlayers.forEach {
        it.resourcesPerTurn = income
        it.resources += income
      }
    }

    // Remove dead creeps
    gameManager.activePlayers.forEach { it.activeCreeps.removeIf { it.health == 0 } }

    // Check end game
    gameManager.activePlayers.forEach { it.checkQueenHealth() }
    if (gameManager.activePlayers.size < 2) {
      gameManager.endGame()
    }

    // Snap entities to integer coordinates
    allEntities().forEach { it.location = it.location.snapToIntegers() }
  }
}

