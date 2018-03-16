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

  private fun readUnit() = UnitInput(
    Vector2(scanner.nextInt(), scanner.nextInt()), scanner.nextInt() == 0, {
      val type = scanner.nextInt()
      when (type) {
        -1 -> null
        else -> CreepType.values()[type]
      }
    }(), scanner.nextInt()
  )//.also { stderr.println("Read creep: $it")}

  init {
    obstacles = (0 until scanner.nextInt()).map { readObstacleInit() }
  }

  protected fun readInputs(): AllInputs {
    val resources = scanner.nextInt()
    val obstacles = (0 until obstacles.size).map { applyObstacleUpdate(readObstaclePerTurn()) }
    val units = (0 until scanner.nextInt()).map { readUnit() }
    return AllInputs(
      units.single { it.isFriendly && it.creepType == null }.let { it.location },
      units.single { it.isFriendly && it.creepType == null }.let { it.health },
      resources,
      units.single { !it.isFriendly && it.creepType == null }.let { it.location },
      units.single { !it.isFriendly && it.creepType == null }.let { it.health },
      obstacles,
      units.filter { it.isFriendly && it.creepType != null },
      units.filter { !it.isFriendly && it.creepType != null }
    )
  }//.also { stderr.println("Read inputs: $it")}

  private fun applyObstacleUpdate(update: ObstaclePerTurnInput): ObstacleInput {
    val matchingObstacle = obstacles.find { it.obstacleId == update.obstacleId }!!
    matchingObstacle.applyUpdate(update)
    return matchingObstacle
  }
}

