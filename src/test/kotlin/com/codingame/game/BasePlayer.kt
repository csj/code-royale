package com.codingame.game

import java.io.InputStream
import java.io.PrintStream
import java.util.*

fun<T> List<T>.sample() = this[Random().nextInt(size)]

data class CreepInput(
  val location: Vector2,
  val health: Int,
  val creepType: CreepType
)

data class ObstaclePerTurnInput(
  val obstacleId: Int,
  val structureType: Int,
  val owner: Int,
  val incomeRateOrHealthOrCooldown: Int,
  val attackRadiusOrCreepType: Int
)

data class ObstacleInput(
  val obstacleId: Int,
  val location: Vector2,
  val radius: Int,
  val minerals: Int,
  var structureType: Int = -1,                 // -1 = None, 0 = Mine, 1 = Tower, 2 = Barracks
  var owner: Int = -1,                         // 0 = Us, 1 = Enemy
  var incomeRateOrHealthOrCooldown: Int = -1,  // mine / tower / barracks
  var attackRadiusOrCreepType: Int = -1        // tower / barracks
) {
  fun applyUpdate(update: ObstaclePerTurnInput) {
    structureType = update.structureType
    owner = update.owner
    incomeRateOrHealthOrCooldown = update.incomeRateOrHealthOrCooldown
    attackRadiusOrCreepType = update.attackRadiusOrCreepType
  }
}


data class AllInputs(
  val kingLoc: Vector2,
  val health: Int,
  val resources: Int,
  val enemyKingLoc: Vector2,
  val enemyHealth: Int,
  val enemyResources: Int,
  val obstacles: List<ObstacleInput>,
  val friendlyCreeps: List<CreepInput>,
  val enemyCreeps: List<CreepInput>
)

abstract class BasePlayer(stdin: InputStream, val stdout: PrintStream, val stderr: PrintStream) {
  private val scanner = Scanner(stdin)

  var obstacles = listOf<ObstacleInput>()

  private fun readObstacleInit() = ObstacleInput(
    scanner.nextInt(),
    Vector2(scanner.nextInt(), scanner.nextInt()),
    scanner.nextInt(), scanner.nextInt()
  )//.also { stderr.println("Read obstacle: $it")}

  private fun readObstaclePerTurn() = ObstaclePerTurnInput(
    scanner.nextInt(), scanner.nextInt(), scanner.nextInt(), scanner.nextInt(), scanner.nextInt()
  )

  private fun readCreep() = CreepInput(
    Vector2(scanner.nextInt(), scanner.nextInt()), scanner.nextInt(), CreepType.values()[scanner.nextInt()]
  )//.also { stderr.println("Read creep: $it")}

  init {
    obstacles = (0 until scanner.nextInt()).map { readObstacleInit() }
  }

  protected fun readInputs() = AllInputs(
    kingLoc = Vector2(scanner.nextInt(), scanner.nextInt()),
    health = scanner.nextInt(),
    resources = scanner.nextInt(),
    enemyKingLoc = Vector2(scanner.nextInt(), scanner.nextInt()),
    enemyHealth = scanner.nextInt(),
    enemyResources = scanner.nextInt(),
    obstacles = (0 until obstacles.size).map { applyObstacleUpdate(readObstaclePerTurn()) },
    friendlyCreeps = (0 until scanner.nextInt()).map { readCreep() },
    enemyCreeps = (0 until scanner.nextInt()).map { readCreep() }
  )//.also { stderr.println("Read inputs: $it")}

  private fun applyObstacleUpdate(update: ObstaclePerTurnInput): ObstacleInput {
    val matchingObstacle = obstacles.find { it.obstacleId == update.obstacleId }!!
    matchingObstacle.applyUpdate(update)
    return matchingObstacle
  }
}

