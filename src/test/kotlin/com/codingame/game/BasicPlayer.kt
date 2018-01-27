package com.codingame.game

import com.codingame.game.Constants.KING_RADIUS
import java.io.InputStream
import java.io.PrintStream
import java.util.*
import kotlin.coroutines.experimental.buildIterator

fun<T> List<T>.sample() = this[Random().nextInt(size)]

data class CreepInput(
  val location: Vector2,
  val health: Int,
  val creepType: CreepType
)

data class ObstacleInput(
  val obstacleId: Int,
  val location: Vector2,
  val radius: Int,
  val minerals: Int,
  val structureType: Int,  // -1 = None, 0 = Mine, 1 = Tower, 2 = Barracks
  val owner: Int,          // 0 = Us, 1 = Enemy
  val incomeRateOrHealthOrCooldown: Int,  // mine / tower / barracks
  val attackRadiusOrCreepType: Int   // tower / barracks
)

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
  private fun readObstacle() = ObstacleInput(
    scanner.nextInt(),
    Vector2(scanner.nextInt(), scanner.nextInt()),
    scanner.nextInt(), scanner.nextInt(),
    scanner.nextInt(), scanner.nextInt(), scanner.nextInt(), scanner.nextInt()
  )//.also { stderr.println("Read obstacle: $it")}

  private fun readCreep() = CreepInput(
    Vector2(scanner.nextInt(), scanner.nextInt()), scanner.nextInt(), CreepType.values()[scanner.nextInt()]
  )//.also { stderr.println("Read creep: $it")}

  protected fun readInputs() = AllInputs(
    kingLoc = Vector2(scanner.nextInt(), scanner.nextInt()),
    health = scanner.nextInt(),
    resources = scanner.nextInt(),
    enemyKingLoc = Vector2(scanner.nextInt(), scanner.nextInt()),
    enemyHealth = scanner.nextInt(),
    enemyResources = scanner.nextInt(),
    obstacles = (0 until scanner.nextInt()).map { readObstacle() },
    friendlyCreeps = (0 until scanner.nextInt()).map { readCreep() },
    enemyCreeps = (0 until scanner.nextInt()).map { readCreep() }
  )//.also { stderr.println("Read inputs: $it")}
}

@Suppress("UNUSED_PARAMETER")
class BasicPlayer(stdin: InputStream, stdout: PrintStream, stderr: PrintStream): BasePlayer(stdin, stdout, stderr) {
  var turn = 0

  init {
    // Rotate between behaviours
    val behaviours = buildIterator { while (true) { yieldAll(listOf(
      { "BUILD MINE" },
      { "BUILD MINE" },
      { "BUILD TOWER" },
      { "BUILD BARRACKS MELEE" },
      { "BUILD MINE" },
      { "BUILD MINE" },
      { "BUILD BARRACKS RANGED" },
      { "BUILD TOWER" },
      { "BUILD TOWER" },
      { "BUILD MINE" },
      { "BUILD MINE" },
      { "BUILD BARRACKS GIANT" }
    ))}}
    var nextBehaviour = behaviours.next()

    while (true) {
      turn++

      val (kingLoc, health, resources,
        enemyKingLoc, enemyHealth, enemyResources,
        obstacles, friendlyCreeps, enemyCreeps) = readInputs()

      fun getKingAction(): String {
        try {
          // if touching a tower that isn't at max health, keep growing it
          val growingTower = obstacles
            .filter { it.owner == 0 && it.structureType == 1 && it.incomeRateOrHealthOrCooldown < 400 }
            .firstOrNull { it.location.distanceTo(kingLoc) - it.radius - KING_RADIUS < 5 }

          if (growingTower != null) return "BUILD TOWER"

          // King goes to nearest unowned obstacle
          val kingTarget = obstacles
            .filter { it.owner == -1 }
            .minBy { it.location.distanceTo(kingLoc) - it.radius }

          if (kingTarget != null) {
            // if in range, do something there
            val dist = kingTarget.location.distanceTo(kingLoc) - Constants.KING_RADIUS - kingTarget.radius

            return if (dist < 5) {
              nextBehaviour().also { nextBehaviour = behaviours.next() }
            } else {
              // move to it
              "MOVE ${kingTarget.location.x.toInt()} ${kingTarget.location.y.toInt()}"
            }
          }

          return "MOVE 1000 500"  // if none, just go to middle of the map
        } catch (ex: Exception) {
          ex.printStackTrace(stderr)
          throw ex
        }
      }

      fun getBuildOrders(): List<ObstacleInput> {
        if (resources < 240) return listOf()
        val barracks = obstacles.filter { it.structureType == 2 && it.owner == 0 && it.incomeRateOrHealthOrCooldown == 0 }
        if (barracks.isEmpty()) return listOf()
        val rando = barracks.sample()
        return listOf(rando)
      }

      stdout.println(getKingAction())
      stdout.println("TRAIN${getBuildOrders().joinToString("") { " " + it.obstacleId }}")
    }
  }
}