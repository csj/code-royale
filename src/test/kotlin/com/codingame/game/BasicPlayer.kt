package com.codingame.game

import java.io.InputStream
import java.io.PrintStream
import java.util.*

data class CreepInput(
  val location: Vector2,
  val health: Int,
  val creepType: CreepType
)

data class ObstacleInput(
  val location: Vector2,
  val radius: Int,
  val minerals: Int,
  val structureType: Int,  // 0 = Mine, 1 = Tower, -1 = None
  val owner: Int,          // 0 = Us, 1 = Enemy
  val incomeRateOrHealth: Int,
  val attackRadius: Int
)

data class AllInputs(
  val kingLoc: Vector2,
  val engineerLoc: Vector2,
  val generalLoc: Vector2,
  val health: Int,
  val resources: Int,
  val enemyKingLoc: Vector2,
  val enemyEngineerLoc: Vector2,
  val enemyGeneralLoc: Vector2,
  val enemyHealth: Int,
  val enemyResources: Int,
  val obstacles: List<ObstacleInput>,
  val friendlyCreeps: List<CreepInput>,
  val enemyCreeps: List<CreepInput>
)

abstract class BasePlayer(stdin: InputStream, val stdout: PrintStream, val stderr: PrintStream) {
  private val scanner = Scanner(stdin)
  private fun readObstacle() = ObstacleInput(
    Vector2(scanner.nextInt(), scanner.nextInt()), scanner.nextInt(), scanner.nextInt(),
    scanner.nextInt(), scanner.nextInt(), scanner.nextInt(), scanner.nextInt()
  )

  private fun readCreep() = CreepInput(
    Vector2(scanner.nextInt(), scanner.nextInt()), scanner.nextInt(), CreepType.values()[scanner.nextInt()]
  )

  protected fun readInputs() = AllInputs(
    kingLoc = Vector2(scanner.nextInt(), scanner.nextInt()),
    engineerLoc = Vector2(scanner.nextInt(), scanner.nextInt()),
    generalLoc = Vector2(scanner.nextInt(), scanner.nextInt()),
    health = scanner.nextInt(),
    resources = scanner.nextInt(),
    enemyKingLoc = Vector2(scanner.nextInt(), scanner.nextInt()),
    enemyEngineerLoc = Vector2(scanner.nextInt(), scanner.nextInt()),
    enemyGeneralLoc = Vector2(scanner.nextInt(), scanner.nextInt()),
    enemyHealth = scanner.nextInt(),
    enemyResources = scanner.nextInt(),
    obstacles = (0 until scanner.nextInt()).map { readObstacle() },
    friendlyCreeps = (0 until scanner.nextInt()).map { readCreep() },
    enemyCreeps = (0 until scanner.nextInt()).map { readCreep() }
  )
}

@Suppress("UNUSED_PARAMETER")
class BasicPlayer(stdin: InputStream, stdout: PrintStream, stderr: PrintStream): BasePlayer(stdin, stdout, stderr) {
  var turn = 0
  var nextZerglings = true

  init {
    while (true) {
      turn++

      val (kingLoc, engineerLoc, generalLoc, health, resources,
        enemyKingLoc, enemyEngineerLoc, enemyGeneralLoc, enemyHealth, enemyResources,
        obstacles, friendlyCreeps, enemyCreeps) = readInputs()

      val kingToEnemyGeneral = kingLoc.distanceTo(enemyGeneralLoc)
      val kingTarget = if (kingToEnemyGeneral < 400) {
        // if King is close to enemy general, run away!
        kingLoc + (kingLoc - enemyGeneralLoc).resizedTo(100.0)
      } else {
        // King goes to nearest unowned obstacle that has money, or else our engineer
        obstacles
          .filter { it.owner == -1 && it.minerals > 0 }
          .minBy { it.location.distanceTo(kingLoc) }?.location ?: engineerLoc
      }

      stdout.println("MOVE ${kingTarget.x.toInt()} ${kingTarget.y.toInt()}")

      // Engineer goes to untowered obstacle nearest the midpoint between our King and their General
      val midPoint = (kingLoc + enemyGeneralLoc) / 2.0

      val closestObstacleToEng = obstacles
        .filter { obs ->
          val isTower = obs.structureType == 1 && obs.owner == 0

          val dist = obs.location.distanceTo(midPoint) - obs.radius - 20
          val building = dist < 20 && isTower && obs.incomeRateOrHealth < 400

          !isTower || building
        }
        .minBy { it.location.distanceTo(midPoint) }!!

      stdout.println("MOVE ${closestObstacleToEng.location.x.toInt()} ${closestObstacleToEng.location.y.toInt()}")

      // general chases enemy king
      when {
        nextZerglings && resources >= CreepType.ZERGLING.cost -> { stdout.println("SPAWN ${CreepType.ZERGLING}"); nextZerglings = false }
        !nextZerglings && resources >= CreepType.ARCHER.cost -> { stdout.println("SPAWN ${CreepType.ARCHER}"); nextZerglings = true }
        else -> stdout.println("MOVE ${enemyKingLoc.x.toInt()} ${enemyKingLoc.y.toInt()}")
      }
    }
  }
}