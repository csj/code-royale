package com.codingame.game

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

  private var playerCount: Int = 2

  private var obstacles: List<Obstacle> = listOf()

  private fun allUnits(): List<MyEntity> = gameManager.players.flatMap { it.allUnits() } + obstacles

  override fun init(params: Properties): Properties {

    gameManager.players[0].enemyPlayer = gameManager.players[1]
    gameManager.players[1].enemyPlayer = gameManager.players[0]
    gameManager.players[1].inverted = true

    for (activePlayer in gameManager.activePlayers) {
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

      activePlayer.kingUnit = makeUnit(30, 10000, activePlayer.fixLocation(Vector2.Zero))
      activePlayer.engineerUnit = makeUnit(20, 6400, activePlayer.fixLocation(Vector2.Zero))
      activePlayer.generalUnit = makeUnit(25, 3600, activePlayer.fixLocation(Vector2.Zero))

      activePlayer.allUnits().forEach { it.updateEntity() }
    }

    obstacles = (0..29).map { Obstacle(entityManager) }
    fixCollisions(60.0)
    obstacles.forEach { it.updateEntities() }

    allUnits().forEach { it.updateEntity() }

    // Params contains all the game parameters that has been to generate this game
    // For instance, it can be a seed number, the size of a grid/map, ...
    return params
  }

  private fun fixCollisions(acceptableGap: Double) {
    for (iter in 0..999) {
      var foundAny = false

      for (u1 in allUnits()) {
        val rad = u1.entity.radius.toDouble()
        val clampDist = if (u1.mass == 0) 60 + rad else rad
        u1.location = u1.location.clampWithin(clampDist, 1920-clampDist, clampDist, 1080-clampDist)

        for (u2 in allUnits()) {
          if (u1 != u2) {
            val overlap = u1.entity.radius + u2.entity.radius + acceptableGap - u1.location.distanceTo(u2.location)
            if (overlap > 0) {
              val (d1, d2) = when {
                u1.mass == 0 && u2.mass == 0 -> Pair(0.5, 0.5)
                u1.mass == 0 -> Pair(0.0, 1.0)
                u2.mass == 0 -> Pair(1.0, 0.0)
                else -> Pair(u2.mass.toDouble() / (u1.mass + u2.mass), u1.mass.toDouble() / (u1.mass + u2.mass))
              }

              val u1tou2 = u2.location - u1.location
              val gap = if (u1.mass == 0 && u2.mass == 0) 20 else 1

              u1.location -= u1tou2.resizedTo(d1 * overlap + if (u1.mass == 0 && u2.mass > 0) 0 else gap)
              u2.location += u1tou2.resizedTo(d2 * overlap + if (u2.mass == 0 && u1.mass > 0) 0 else gap)

              foundAny = true
            }
          }
        }
      }
      if (!foundAny) break
    }
  }

  override fun gameTurn(turn: Int) {
    // Code your game logic.
    // See README.md if you want some code to bootstrap your project.

    gameManager.players.forEach {
      val king = it.kingUnit
      val obsK = obstacles.minBy { it.location.distanceTo(king.location) }!!

      // TODO: What if both kings are touching the same one!
      if (obsK.location.distanceTo(king.location) - obsK.radius - 30 < 10) {
        obsK.incomeOwner = it
        obsK.incomeTimer = 40
      }

      // TODO: What if both engineers are touching the same one!
      val eng = it.engineerUnit
      val obsE = obstacles.minBy { it.location.distanceTo(eng.location) }!!
      if (obsE.location.distanceTo(eng.location) - obsE.radius - eng.entity.radius < 10) {
        if (obsE.towerOwner == it) {
          obsE.towerHealth += 100
        } else if (obsE.towerOwner == null) {
          obsE.setTower(it, 200)
        }
      }
    }
    gameManager.activePlayers.flatMap { it.activeCreeps }.toList().forEach { it.damage(1) }
    obstacles.forEach { it.act() }
    gameManager.activePlayers.flatMap { it.activeCreeps }.forEach { it.act() }
    fixCollisions(0.0)
    gameManager.activePlayers.flatMap { it.activeCreeps }.forEach {
      val enemyKing = it.owner.enemyPlayer.kingUnit
      if (it.location.distanceTo(enemyKing.location) < it.entity.radius + enemyKing.entity.radius + it.attackRange + 10) {
        it.owner.enemyPlayer.health -= 1
      }
    }

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
              unit.location = unit.location.towards(player.fixLocation(Vector2(x, y)), 40.0)
            }
            "SPAWN" -> {
              // TODO: Check if it's the general
              // TODO: Check if enough resources
              when (toks[1]) {
                "ZERGLINGS" -> {
                  repeat(4, { player.activeCreeps += Zergling(entityManager, player, unit.location) })
                  player.resources -= 40
                }
                "ARCHERS" -> {
                  repeat(2, { player.activeCreeps += Archer(entityManager, player, unit.location) })
                  player.resources -= 70
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
  }
}