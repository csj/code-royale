package com.codingame.game

import com.codingame.gameengine.core.AbstractPlayer
import com.codingame.gameengine.core.AbstractReferee
import com.codingame.gameengine.core.GameManager
import com.codingame.gameengine.module.entities.GraphicEntityModule
import com.google.inject.Inject
import java.util.*

@Suppress("unused")  // injected by magic
class Referee : AbstractReferee() {
  @Inject private lateinit var gameManager: GameManager<MyPlayer>
  @Inject private lateinit var entityManager: GraphicEntityModule

  private var obstacles: List<Obstacle> = listOf()
  private var resources = mutableListOf<Resource>()
  private val random = Random()

  private fun allUnits(): List<MyUnit> = gameManager.players.flatMap { it.allUnits() } + resources

  override fun init(params: Properties): Properties {

    val firstPlayer = gameManager.activePlayers[0]
    val secondPlayer = gameManager.activePlayers[1]

    for (i in 0..2) {
      val locX = random.nextInt(gridNumX / 2) //0 to (gridNumX / 2 - 1) inclusive
      val locY = random.nextInt(gridNumY)

      //TODO: add check to see if any exists already

      val otherLocX = gridNumX - locX - 1

      when (i) {
        0 -> {
          firstPlayer.kingUnit = OwnedUnit(locX, locY, firstPlayer, KING_UNIT)
          secondPlayer.kingUnit = OwnedUnit(otherLocX, locY, secondPlayer, KING_UNIT)
        }
        1 -> {
          firstPlayer.engineerUnit = OwnedUnit(locX, locY, firstPlayer, ENGINEER_UNIT)
          secondPlayer.engineerUnit = OwnedUnit(otherLocX, locY, secondPlayer, ENGINEER_UNIT)
        }
        2 -> {
          firstPlayer.generalUnit = OwnedUnit(locX, locY, firstPlayer, GENERAL_UNIT)
          firstPlayer.generalUnit = OwnedUnit(locX, locY, secondPlayer, GENERAL_UNIT)
        }
      }
    }

    repeat(RESOURCES_PER_SIDE, {

      var notFinished : Boolean

      do {

        val locX = random.nextInt(gridNumX / 2) //0 to (gridNumX / 2 - 1) inclusive
        val locY = random.nextInt(gridNumY)

        notFinished = allUnits().any { it.x == locX && it.y == locY }

        if (notFinished)
          continue

        val resourceAmount = random.nextInt(MAX_RESOURCE_AMOUNT - MIN_RESOURCE_AMOUNT) + MIN_RESOURCE_AMOUNT

        val otherLocX = gridNumX - locX - 1

        resources.add(Resource(locX, locY, resourceAmount))
        resources.add(Resource(otherLocX, locY, resourceAmount))

      } while (notFinished)

    })

    // Params contains all the game parameters that has been to generate this game
    // For instance, it can be a seed number, the size of a grid/map, ...
    return params
  }

  override fun gameTurn(turn: Int) {
    // Code your game logic.
    // See README.md if you want some code to bootstrap your project.

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
    gameManager.activePlayers.flatMap { it.activeCreeps }.forEach { it.move() }
    fixCollisions(0.0)
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
          val fixedLocation = activePlayer.fixLocation(it.location)
          activePlayer.sendInputLine("${fixedLocation.x.toInt()} ${fixedLocation.y.toInt()} ${it.health} ${it.creepType.ordinal}")
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
              unit.location = unit.location.towards(player.fixLocation(Vector2(x, y)), UNIT_SPEED.toDouble())
            }
            "SPAWN" -> {
              // TODO: Check if it's the general
              // TODO: Check if enough resources
              // TODO: Ensure it's a proper creep type
              val creepType = CreepType.valueOf(toks[1])
              repeat(creepType.count, { player.activeCreeps += when (creepType) {
                CreepType.ARCHER, CreepType.ZERGLING ->
                  KingChasingCreep(entityManager, player, unit.location, creepType)
                CreepType.GIANT ->
                  TowerBustingCreep(entityManager, player, unit.location, creepType, obstacles)
              }})
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
  ZERGLING(4, 40, 80, 0, 10, 400, 30, "bug.png", "bugfill.png"),
  ARCHER(2, 70, 60, 200, 15, 900, 45, "bug2.png", "bug2fill.png"),
  GIANT(1, 80, 40, 0, 25, 2000, 200, "bug.png", "bugfill.png")
}


enum class UnitType() {
  RESOURCE,
  KING,
  GENERAL,
  ENGINEER
}