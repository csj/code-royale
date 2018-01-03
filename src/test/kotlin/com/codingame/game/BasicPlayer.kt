package com.codingame.game

import java.io.InputStream
import java.io.PrintStream
import java.util.*

data class ObstacleInput(
  val location: Vector2,
  val radius: Int,
  val incomeOwner: Int,
  val incomeTimer: Int,
  val towerOwner: Int,
  val towerHealth: Int,
  val towerRange: Int
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
  val obstacles: List<ObstacleInput>
)

abstract class BasePlayer(stdin: InputStream) {
  private val scanner = Scanner(stdin)
  private fun readObstacle() = ObstacleInput(
    Vector2(scanner.nextInt(), scanner.nextInt()), scanner.nextInt(), scanner.nextInt(),
    scanner.nextInt(), scanner.nextInt(), scanner.nextInt(), scanner.nextInt()
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
    obstacles = (0 until scanner.nextInt()).map { readObstacle() }
  )
}

@Suppress("UNUSED_PARAMETER")
class BasicPlayer(stdin: InputStream, stdout: PrintStream, stderr: PrintStream): BasePlayer(stdin) {
  var turn = 0
  var nextZerglings = true

  init {
    while (true) {
      turn++

      val (kingLoc, engineerLoc, generalLoc, health, resources,
        enemyKingLoc, enemyEngineerLoc, enemyGeneralLoc, enemyHealth, enemyResources,
        obstacles) = readInputs()

      val kingToEnemyGeneral = kingLoc.distanceTo(enemyGeneralLoc)
      val kingTarget = if (kingToEnemyGeneral < 400) {
        // if King is close to enemy general, run away!
        kingLoc + (kingLoc - enemyGeneralLoc).resizedTo(100.0)
      } else {
        // King goes to nearest untagged obstacle under friendly tower influence that doesn't have an enemy tower, or else our engineer
        obstacles
          .filter { it.incomeOwner != 0 && it.towerOwner != 1 }
          .filter { target ->
            obstacles.any {
              it.towerOwner == 0 && it.location.distanceTo(target.location) - target.radius - it.towerRange < 50
            }
          }
          .minBy { it.location.distanceTo(kingLoc) }?.location ?: engineerLoc
      }

      stdout.println("MOVE ${kingTarget.x.toInt()} ${kingTarget.y.toInt()}")

      val closestObstacleToEng = obstacles
        .filter { it.towerOwner != 1 }
        .minBy { it.location.distanceTo(engineerLoc) }!!
      val dist = closestObstacleToEng.location.distanceTo(engineerLoc) - closestObstacleToEng.radius - 20
      val engTarget = if (dist < 5 && (closestObstacleToEng.towerOwner != 0 || closestObstacleToEng.towerHealth < 400))
        closestObstacleToEng.location
      else {
        obstacles
          .filter { it.towerOwner != 0 }
          .minBy { it.location.distanceTo(kingLoc) }!!.location
      }

      stdout.println("MOVE ${engTarget.x.toInt()} ${engTarget.y.toInt()}")

      // general chases enemy king
      when {
        nextZerglings && resources >= 40 -> { stdout.println("SPAWN ZERGLINGS"); nextZerglings = false }
        !nextZerglings && resources >= 70 -> { stdout.println("SPAWN ARCHERS"); nextZerglings = true }
        else -> stdout.println("MOVE ${enemyKingLoc.x.toInt()} ${enemyKingLoc.y.toInt()}")
      }
    }
  }
}