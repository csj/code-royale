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
  private val addedTowers = mutableListOf<KillableUnit>()
  private var playerBuildLocation : Pair<Int, Int>? = null
  private var playerSpawnLocation : Pair<Int, Int>? = null

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

    val player = gameManager.activePlayers[turn % 2]

    var list = allUnits()
    resourcesToRemove.clear()
    addedTowers.clear()
    
    player.sendInputLine("${player.health}")
    player.sendInputLine("${player.resources}")
    player.sendInputLine("${player.enemyPlayer.health}")
    player.sendInputLine("${player.enemyPlayer.resources}")
    player.sendInputLine("${list.size}")
    list.forEach { player.sendInputLine(it.sendInput(player)) }

    player.execute()

    //TODO: don't end game until max turns or king dies because it is possible to have no valid moves but still win

    playerBuildLocation = null
    playerSpawnLocation = null

    //Necessary for making sure savedX and savedY reflect original values (if unit doesn't move)
    allUnits().forEach(MyUnit::save)

    val properOutput = handleOutputs(player, list)

    if (!properOutput) {
      gameManager.endGame()
      return
    }

    if (playerSpawnLocation != null) {
      player.resources -= MINION_COST
      player.subUnits.add(Minion(playerSpawnLocation!!.first,
              playerSpawnLocation!!.second, GLOBAL_ID++, entityManager, player, MINION_TYPE))
    }


    resourcesToRemove.forEach { resources.remove(it) }

    if (playerBuildLocation != null && !allUnits().any { it.x == playerBuildLocation!!.first
            && it.y == playerBuildLocation!!.second }) {
      val tower = Tower(playerBuildLocation!!.first,
              playerBuildLocation!!.second, GLOBAL_ID++, entityManager, player, TOWER_TYPE)
      addedTowers.add(tower)
      player.subUnits.add(tower)
      player.resources -= TOWER_BUILD_COST
    }

    list = allUnits() //Main and sub units plus resource units

    player.mainUnits.forEach {
      val tempX = it.savedX //Saved values were set when processing moves
      val tempY = it.savedY
      it.save()
      it.x = tempX
      it.y = tempY
    }

    player.subUnits.forEach(MyUnit::save) //Make sure to save some new units (towers and minions)

    var foundCollision = true

    while (foundCollision) {
      foundCollision = false

      player.mainUnits.forEach { mainUnit ->
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


    player.subUnits.forEach {
      var bestX = it.x
      var bestY = it.y
      var shortestDist = dist(bestX, bestY, it.owner.enemyPlayer.kingUnit.x, it.owner.enemyPlayer.kingUnit.y)
      if (it is Minion) {
        for (deltaX in -it.moveDist..it.moveDist) {
          for (deltaY in -it.moveDist..it.moveDist) {
            val targetX = it.x + deltaX
            val targetY = it.y + deltaY
            val dist = dist(targetX, targetY, it.owner.enemyPlayer.kingUnit.x, it.owner.enemyPlayer.kingUnit.y)
            if (inBounds(targetX, targetY) && dist < shortestDist) {
              val isCollision = list.any { unit -> unit.x == it.x && unit.y == it.y }
              if (!isCollision) {
                bestX = targetX
                bestY = targetY
                shortestDist = dist
              }
            }
          }
        }
        it.x = bestX
        it.y = bestY
      }
    }

    //Collisions at this point will only be because of main units that moved to a minion's starting location expecting
    //the minion to move which it didn't due to it being in the optimal position already or no other spots available
    while (foundCollision) {
      foundCollision = false

      player.mainUnits.forEach { mainUnit ->
        val collisions = list.filter { unit -> unit.x == mainUnit.x && unit.y == mainUnit.y && mainUnit != unit }
        if (collisions.isNotEmpty()) {
          foundCollision = true
          mainUnit.reset()
          collisions.forEach(MyUnit::reset)
        }
      }
    }

    player.subUnits.forEach {
      if (it is Tower && dist(player.engineerUnit.x, player.engineerUnit.y, it.x, it.y) <= ENGINEER_HEAL_RADIUS) {
        it.health += ENGINEER_HEAL_AMOUNT
      }
    }

    player.subUnits.forEach {
      if (!addedTowers.contains(it)) {
        it.damage(gameManager)
      }
    }

    if (player.health < 0) {
      player.deactivate("King died")
      gameManager.endGame()
      return
    }

    if (player.enemyPlayer.health < 0) {
      player.deactivate("King died")
      gameManager.endGame()
      return
    }

    destroyDeadUnits(player.subUnits)
    destroyDeadUnits(player.enemyPlayer.subUnits)

    player.subUnits.forEach {
      it.health = minOf(MAX_HEALTH, it.health)
    }

    draw()

    if (gameManager.activePlayers.size < 2)
      gameManager.endGame()

//    gameManager.setGameSummary(gameManager.players.map { "${it.nicknameToken} Health: ${it.health} Resources: ${it.resources}"})

  }

  fun deactivatePlayer(reason: String, player: Player) {
    player.deactivate(reason)
  }

  override fun onEnd() {
    for (player in gameManager.players)
      player.score = if(player.isActive) 1 else 0
  }


  fun destroyDeadUnits(list: MutableList<KillableUnit>) {
    list.removeIf {
      val remove = it.health <= 0
      if (remove)
        it.entity.isVisible = false
      remove
    }
  }
  
  
  fun handleOutputs(player : Player, list: List<MyUnit> ) : Boolean {

    try {
      val outputs = player.outputs

      for ((unit, line) in player.mainUnits.zip(outputs)) {
        val toks = line.split(" ")
        if (toks.size < 3) {
          player.deactivate("Invalid output")
          return false
        }
        val targetX = toks[1].toInt()
        val targetY = toks[2].toInt()
        when (toks[0]) {
          "MOVE" -> {
            if (dist(unit.x , unit.y, targetX, targetY) > unit.moveDist) {
              player.deactivate("Move target too far away")
              return false
            }
            //Don't move yet because minions need to be spawned and towers need to be built
            //and they check for occupied units
            unit.savedX = targetX
            unit.savedY = targetY
          }
          "HARVEST" -> {
            if (unit.unitType != KING_UNIT) {
              player.deactivate("Can only harvest with King unit")
              return false
            }
            val foundResource = resources.find { it.x == targetX && it.y == targetY}
            if (foundResource == null) {
              player.deactivate("No resource exists at this location")
              return false
            }
            else if (dist(unit.x, unit.y, targetX, targetY) > 1) {
              player.deactivate("Resource is too far away")
              return false
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
              return false
            }
            else if (player.resources < TOWER_BUILD_COST) {
              player.deactivate("Too little resources to build")
              return false
            }
            else if (dist(unit.x, unit.y, targetX, targetY) > 1) {
              player.deactivate("Build location too far away")
              return false
            }
            //Right now player can harvest resource with King and build onto it with engineer on same turn
            //If later I give bonus for building onto resource this aspect may want to be checked
            val target = resources.find { it.x == targetX && it.y == targetY }
            if (target != null) {
                playerBuildLocation = Pair(targetX, targetY)
            }
            else {
              playerBuildLocation = availableSpots.find { it.first == targetX && it.second == targetY }
              if (playerBuildLocation == null) {
                player.deactivate("Cannot build on this spot")
                return false
              }
              else {
                val foundUnit = list.find { it.x == targetX && it.y == targetY }
                if (foundUnit != null) {
                  player.deactivate("The build location is occupied")
                  return false
                }
              }
            }
          }
          "SPAWN" -> {
            //TODO: use minion type
            if (toks.size < 4) {
              player.deactivate("Invalid output, must provide minion type")
              return false
            }
            if (unit.unitType != GENERAL_UNIT) {
              player.deactivate("Can only spawn minions with general")
              return false
            }
            else if (player.resources < MINION_COST) {
              player.deactivate("Too little resources to spawn minion")
              return false
            }
            else if (dist(unit.x, unit.y, targetX, targetY) > 1) {
              player.deactivate("Spawn location too far away")
              return false
            }
            val occupyingUnit = list.find { it.x == targetX && it.y == targetY }
            if (occupyingUnit != null) {
              player.deactivate("The spawn location is occupied")
              return false
            }

            playerSpawnLocation = Pair(targetX, targetY)
          }
        }
      }

    } catch (e: AbstractPlayer.TimeoutException) {
      e.printStackTrace()
      player.deactivate("${player.nicknameToken}: timeout!")
      return false
    }
    return true
  }
}

