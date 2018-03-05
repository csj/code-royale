package com.codingame.game

import java.io.InputStream
import java.io.PrintStream
import java.util.*

@Suppress("unused")
abstract class BasePlayer(stdin: InputStream, val stdout: PrintStream, val stderr: PrintStream) {
  private val scanner = Scanner(stdin)

  var obstacles = listOf<ObstacleInput>()

  private fun readObstacleInit() = ObstacleInput(
    scanner.nextInt(),
    Vector2(scanner.nextInt(), scanner.nextInt()),
    scanner.nextInt()
  )//.also { stderr.println("Read obstacle: $it")}

  private fun readObstaclePerTurn() = ObstaclePerTurnInput(
    scanner.nextInt(), scanner.nextInt(), scanner.nextInt(), scanner.nextInt(),
      scanner.nextInt(), scanner.nextInt(), scanner.nextInt()
  )

  private fun readCreep() = CreepInput(
    Vector2(scanner.nextInt(), scanner.nextInt()), scanner.nextInt(), CreepType.values()[scanner.nextInt()]
  )//.also { stderr.println("Read creep: $it")}

  init {
    obstacles = (0 until scanner.nextInt()).map { readObstacleInit() }
  }

  protected fun readInputs() = AllInputs(
    queenLoc = Vector2(scanner.nextInt(), scanner.nextInt()),
    health = scanner.nextInt(),
    resources = scanner.nextInt(),
    enemyQueenLoc = Vector2(scanner.nextInt(), scanner.nextInt()),
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

