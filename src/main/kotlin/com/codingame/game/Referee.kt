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

//  private var obstacles: List<Obstacle> = listOf()
  private val resources = mutableListOf<Resource>()
  private val random = Random()
  private val availableSpots = mutableListOf<Pair<Int, Int>>()
  private val resourcesToRemove = mutableSetOf<Resource>()
  private val addedTowers = mutableListOf<Tower>()

  private fun allUnits() = gameManager.players.flatMap { it.allUnits() } + resources

  private fun dist(sourceX : Int, sourceY: Int, targetX: Int, targetY: Int) =
          maxOf(Math.abs(sourceX - targetX ), Math.abs(sourceY - targetY))

  private fun inBounds(xCoord : Int, yCoord: Int) = xCoord > -1 && xCoord < gridNumX && yCoord > -1 && yCoord < gridNumY

  override fun init(params: Properties): Properties {

    val firstPlayer = gameManager.activePlayers[0]
    val secondPlayer = gameManager.activePlayers[1]
    firstPlayer.enemyPlayer = secondPlayer
    secondPlayer.enemyPlayer = firstPlayer

    val tempList = mutableListOf<MyUnit>()

    for (i in 0..2) {

      var notFinished: Boolean

      do {
        val locX = random.nextInt(gridNumX / 2) //0 to (gridNumX / 2 - 1) inclusive
        val locY = random.nextInt(gridNumY)

        notFinished = tempList.any { it.x == locX && it.y == locY }

        if (notFinished)
          continue

        val otherLocX = gridNumX - locX - 1

        when (i) {
          0 -> {
            firstPlayer.kingUnit = OwnedUnit(locX, locY, GLOBAL_ID++, entityManager, firstPlayer, KING_UNIT)
            secondPlayer.kingUnit = OwnedUnit(otherLocX, locY, GLOBAL_ID++, entityManager, secondPlayer, KING_UNIT)
            tempList.add(firstPlayer.kingUnit)
            tempList.add(secondPlayer.kingUnit)
          }
          1 -> {
            firstPlayer.engineerUnit = OwnedUnit(locX, locY, GLOBAL_ID++, entityManager, firstPlayer, ENGINEER_UNIT)
            secondPlayer.engineerUnit = OwnedUnit(otherLocX, locY, GLOBAL_ID++, entityManager, secondPlayer, ENGINEER_UNIT)
            tempList.add(firstPlayer.engineerUnit)
            tempList.add(secondPlayer.engineerUnit)
          }
          2 -> {
            firstPlayer.generalUnit = OwnedUnit(locX, locY, GLOBAL_ID++, entityManager, firstPlayer, GENERAL_UNIT)
            secondPlayer.generalUnit = OwnedUnit(otherLocX, locY, GLOBAL_ID++, entityManager, secondPlayer, GENERAL_UNIT)
            tempList.add(firstPlayer.generalUnit)
            tempList.add(secondPlayer.generalUnit)
          }
        }

      } while (notFinished)
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

        resources.add(Resource(locX, locY, GLOBAL_ID++, entityManager, resourceAmount))
        resources.add(Resource(otherLocX, locY, GLOBAL_ID++, entityManager, resourceAmount))

      } while (notFinished)

    })

    draw()

    // Params contains all the game parameters that has been to generate this game
    // For instance, it can be a seed number, the size of a grid/map, ...
    return params
  }

  fun draw() {
    allUnits().forEach(MyUnit::draw)
  }

  override fun gameTurn(turn: Int) {
    // Code your game logic.
    // See README.md if you want some code to bootstrap your project.

//    gameManager.players.forEach {
//      val king = it.kingUnit
//      val obsK = obstacles.minBy { it.location.distanceTo(king.location) }!!
//
//      // TODO: What if both kings are touching the same one!
//      if (obsK.location.distanceTo(king.location) - obsK.radius - king.entity.radius < 10) {
//        obsK.incomeOwner = it
//        obsK.incomeTimer = INCOME_TIMER
//      }
//
//      // TODO: What if both engineers are touching the same one!
//      val eng = it.engineerUnit
//      val obsE = obstacles.minBy { it.location.distanceTo(eng.location) }!!
//      if (obsE.location.distanceTo(eng.location) - obsE.radius - eng.entity.radius < 10) {
//        if (obsE.towerOwner == it) {
//          obsE.towerHealth += TOWER_HP_INCREMENT
//        } else if (obsE.towerOwner == null) {
//          obsE.setTower(it, TOWER_HP_INITIAL)
//        }
//      }
//    }
//    gameManager.activePlayers.flatMap { it.activeCreeps }.toList().forEach { it.damage(1) }
//    obstacles.forEach { it.act() }
//    gameManager.activePlayers.flatMap { it.activeCreeps }.forEach { it.move() }
//    fixCollisions(0.0)
//    gameManager.activePlayers.flatMap { it.activeCreeps }.forEach { it.dealDamage() }
//
//    gameManager.activePlayers.forEach {
//      if (!it.checkKingHealth()) {
//        it.deactivate("Dead king")
//      }
//    }
//
//    if (gameManager.activePlayers.size < 2) {
//      gameManager.endGame()
//    }
//
//    allUnits().forEach { it.updateEntity() }

    var list = allUnits()
    resourcesToRemove.clear()
    addedTowers.clear()

    for (activePlayer in gameManager.activePlayers) {
      activePlayer.sendInputLine("${activePlayer.health}")
      activePlayer.sendInputLine("${activePlayer.resources}")
      activePlayer.sendInputLine("${activePlayer.enemyPlayer.health}")
      activePlayer.sendInputLine("${activePlayer.enemyPlayer.resources}")
      activePlayer.sendInputLine("${list.size}")
      list.forEach { activePlayer.sendInputLine(it.sendInput(activePlayer)) }
//      activePlayer.units.forEach { activePlayer.printLocation(it.location) }
//      activePlayer.sendInputLine("${activePlayer.health} ${activePlayer.resources}")
//      activePlayer.enemyPlayer.units.forEach { activePlayer.printLocation(it.location) }
//      activePlayer.sendInputLine("${activePlayer.enemyPlayer.health} ${activePlayer.enemyPlayer.resources}")
//      activePlayer.sendInputLine(obstacles.size.toString())
//      obstacles.forEach { activePlayer.printObstacle(it) }
//
//      for (player in listOf(activePlayer, activePlayer.enemyPlayer)) {
//        activePlayer.sendInputLine(player.activeCreeps.size.toString())
//        player.activeCreeps.forEach {
//          val fixedLocation = activePlayer.fixLocation(it.location)
//          activePlayer.sendInputLine("${fixedLocation.x.toInt()} ${fixedLocation.y.toInt()} ${it.health} ${it.creepType.ordinal}")
//        }
//      }
      activePlayer.execute()
    }

    //TODO: don't end game until max turns or king dies because it is possible to have no valid moves but still win

    var firstPlayerBuildTowerLocation : Pair<Int, Int>? = null
    var firstPlayerSpawnLocation : Pair<Int, Int>? = null

    var secondPlayerBuildTowerLocation : Pair<Int, Int>? = null
    var secondPlayerSpawnLocation : Pair<Int, Int>? = null

    val secondPlayer = gameManager.activePlayers[1]

    //Necessary for making sure savedX and savedY reflect original values (if unit doesn't move)
    allUnits().forEach(MyUnit::save)

    outer@ for (player in gameManager.activePlayers) {
      try {
        val outputs = player.outputs

        for ((unit, line) in player.mainUnits.zip(outputs)) {
          val toks = line.split(" ")
          if (toks.size < 3) {
            player.deactivate("Invalid output")
            continue@outer
          }
          val targetX = toks[1].toInt()
          val targetY = toks[2].toInt()
          when (toks[0]) {
            "MOVE" -> {
              if (dist(unit.x , unit.y, targetX, targetY) > unit.moveDist) {
                player.deactivate("Move target too far away")
                continue@outer
              }
              //Don't move yet because minions need to be spawned and towers need to be built
              //and they check for occupied units
              unit.savedX = targetX
              unit.savedY = targetY
            }
            "HARVEST" -> {
              if (unit.unitType != KING_UNIT) {
                player.deactivate("Can only harvest with King unit")
                continue@outer
              }
              val foundResource = resources.find { it.x == targetX && it.y == targetY}
              if (foundResource == null) {
                player.deactivate("No resource exists at this location")
                continue@outer
              }
              else if (dist(unit.x, unit.y, targetX, targetY) > 1) {
                player.deactivate("Resource is too far away")
                continue@outer
              }
              else {
                player.resources += foundResource.resourceAmount
                resourcesToRemove.add(foundResource)
              }
            }
            "BUILD" -> {
              //TODO: check if build location is occupied from list, if it is deactivate
              //TODO: if not check if occupied from minions spawned this turn, if it is don't build but don't deactivate
              if (unit.unitType != ENGINEER_UNIT) {
                player.deactivate("Can only build with engineer unit")
                continue@outer
              }
              else if (player.resources < TOWER_BUILD_COST) {
                player.deactivate("Too little resources to build")
                continue@outer
              }
              else if (dist(unit.x, unit.y, targetX, targetY) > 1) {
                player.deactivate("Build location too far away")
                continue@outer
              }
              //Right now player can harvest resource with King and build onto it with engineer on same turn
              //If later I give bonus for building onto resource this aspect may want to be checked
              val target = resources.find { it.x == targetX && it.y == targetY }
              if (target != null) {
                if (player != secondPlayer)
                  firstPlayerBuildTowerLocation = Pair(targetX, targetY)
                else
                  secondPlayerBuildTowerLocation = Pair(targetX, targetY)
              }
              else {
                if (player != secondPlayer) {
                  firstPlayerBuildTowerLocation = availableSpots.find { it.first == targetX && it.second == targetY }
                  if (firstPlayerBuildTowerLocation == null) {
                    player.deactivate("Cannot build on this spot")
                    continue@outer
                  }
                  else {
                    val foundUnit = list.find { it.x == targetX && it.y == targetY }
                    if (foundUnit != null) {
                      player.deactivate("The build location is occupied")
                      continue@outer
                    }
                  }
                }
                else {
                  secondPlayerBuildTowerLocation = availableSpots.find { it.first == targetX && it.second == targetY }
                  if (secondPlayerBuildTowerLocation == null) {
                    player.deactivate("Cannot build on this spot")
                    continue@outer
                  }
                  else {
                    val foundUnit = list.find { it.x == targetX && it.y == targetY }
                    if (foundUnit != null) {
                      player.deactivate("The build location is occupied")
                      continue@outer
                    }
                  }
                }
              }
            }
            "SPAWN" -> {
              //TODO: use minion type
              if (toks.size < 4) {
                player.deactivate("Invalid output, must provide minion type")
                continue@outer
              }
              if (unit.unitType != GENERAL_UNIT) {
                player.deactivate("Can only spawn minions with general")
                continue@outer
              }
              else if (player.resources < MINION_COST) {
                player.deactivate("Too little resources to spawn minion")
                continue@outer
              }
              else if (dist(unit.x, unit.y, targetX, targetY) > 1) {
                player.deactivate("Spawn location too far away")
                continue@outer
              }
              val occupyingUnit = list.find { it.x == targetX && it.y == targetY }
              if (occupyingUnit != null) {
                player.deactivate("The spawn location is occupied")
                continue@outer
              }

              if (player != secondPlayer)
                firstPlayerSpawnLocation = Pair(targetX, targetY)
              else
                secondPlayerSpawnLocation = Pair(targetX, targetY)
            }
          }
        }

//        for ((unit, line) in player.units.zip(outputs)) {
//          val toks = line.split(" ")
//          when (toks[0]) {
//            "MOVE" -> {
//              val (x, y) = toks.drop(1).map { Integer.valueOf(it) }
//              unit.location = unit.location.towards(player.fixLocation(Vector2(x, y)), UNIT_SPEED.toDouble())
//            }
//            "SPAWN" -> {
//              // TODO: Check if it's the general
//              // TODO: Check if enough resources
//              // TODO: Ensure it's a proper creep type
//              val creepType = CreepType.valueOf(toks[1])
//              repeat(creepType.count, { player.activeCreeps += when (creepType) {
//                CreepType.ARCHER, CreepType.ZERGLING ->
//                  KingChasingCreep(entityManager, player, unit.location, creepType)
//                CreepType.GIANT ->
//                  TowerBustingCreep(entityManager, player, unit.location, creepType, obstacles)
//              }})
//              player.resources -= creepType.cost
//            }
//          }
//        }
      } catch (e: AbstractPlayer.TimeoutException) {
        e.printStackTrace()
        player.deactivate("${player.nicknameToken}: timeout!")
      }
    }

    if (firstPlayerSpawnLocation != null) {
      if (firstPlayerSpawnLocation != secondPlayerSpawnLocation)
        gameManager.activePlayers[0].subUnits.add(Minion(firstPlayerSpawnLocation.first,
                firstPlayerSpawnLocation.second, GLOBAL_ID++, entityManager, gameManager.activePlayers[0], MINION_TYPE))
    }

    if (secondPlayerSpawnLocation != null) {
      if (secondPlayerSpawnLocation != firstPlayerSpawnLocation)
        gameManager.activePlayers[1].subUnits.add(Minion(secondPlayerSpawnLocation.first,
                secondPlayerSpawnLocation.second, GLOBAL_ID++, entityManager, gameManager.activePlayers[1],
                MINION_TYPE))
    }

    resourcesToRemove.forEach { resources.remove(it) }

    if (firstPlayerBuildTowerLocation != null && firstPlayerBuildTowerLocation != secondPlayerBuildTowerLocation
            && !allUnits().any { it.x == firstPlayerBuildTowerLocation!!.first
            && it.y == firstPlayerBuildTowerLocation!!.second }) {
      val tower = Tower(firstPlayerBuildTowerLocation.first,
              firstPlayerBuildTowerLocation.second, GLOBAL_ID++, entityManager,
              gameManager.activePlayers[0], TOWER_TYPE)
      addedTowers.add(tower)
      gameManager.activePlayers[0].subUnits.add(tower)
    }

    if (secondPlayerBuildTowerLocation != null && secondPlayerBuildTowerLocation != firstPlayerBuildTowerLocation
            && !allUnits().any { it.x == secondPlayerBuildTowerLocation!!.first
            && it.y == secondPlayerBuildTowerLocation!!.second }) {
      val tower = Tower(secondPlayerBuildTowerLocation.first,
              secondPlayerBuildTowerLocation.second, GLOBAL_ID++, entityManager,
              gameManager.activePlayers[1], TOWER_TYPE)
      addedTowers.add(tower)
      gameManager.activePlayers[1].subUnits.add(tower)
    }

    list = allUnits() //Main and sub units plus resource units
    val allMainUnits = gameManager.activePlayers.flatMap { it.mainUnits } //General, King, Engineer
    val allSubUnits = gameManager.activePlayers.flatMap { it.subUnits } //Minions and towers

    allMainUnits.forEach {
      val tempX = it.savedX //Saved values were set when processing moves
      val tempY = it.savedY
      it.save()
      it.x = tempX
      it.y = tempY
    }

    allSubUnits.forEach(MyUnit::save)

    var foundCollision = true

    while (foundCollision) {
      foundCollision = false

      allMainUnits.forEach { mainUnit ->
        val collisions = list.filter { unit -> unit.x == mainUnit.x && unit.y == mainUnit.y && mainUnit != unit }
        if (collisions.isNotEmpty()) {
          if (collisions.size > 1 || collisions[0] !is Minion) { //If only colliding with minion leave it for now
            foundCollision = true
            mainUnit.reset()
            collisions.forEach(MyUnit::reset)
          }
        }
      }
    }

    //How to handle collisions and stuff????
    //Maybe resolve collisions between main units first and then have sub units avoid them auto? But also do main uni
    //But then how to resolve collisions between sub units?

    for (player in gameManager.activePlayers) {

      player.subUnits.forEach {
        var bestX = it.x
        var bestY = it.y
        var shortestDist = 10000
        if (it is Minion) {
          for (deltaX in -it.moveDist..it.moveDist) {
            for (deltaY in -it.moveDist..it.moveDist) {
              val targetX = it.x + deltaX
              val targetY = it.y + deltaY
              val dist = dist(targetX, targetY, it.owner.enemyPlayer.kingUnit.x, it.owner.enemyPlayer.kingUnit.y)
              if (inBounds(targetX, targetY) && dist < shortestDist) {
                val collisions = list.filter { unit -> unit.x == it.x && unit.y == it.y && it != unit &&
                        !(it is Minion && it}
                  bestX = targetX
                bestY = targetY
                shortestDist = dist
              }
            }
          }
        }
      }

    }



    allUnits().forEach(MyUnit::save)

    if (gameManager.activePlayers.size < 2)
      gameManager.endGame()

//    gameManager.setGameSummary(gameManager.players.map { "${it.nicknameToken} Health: ${it.health} Resources: ${it.resources}"})

  }

  override fun onEnd() {
    for (player in gameManager.players)
      player.score = if(player.isActive) 1 else 0
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