import com.codingame.game.BasePlayer
import com.codingame.game.Constants.QUEEN_RADIUS
import com.codingame.game.ObstacleInput
import com.codingame.game.sample
import java.io.InputStream
import java.io.PrintStream
import kotlin.coroutines.experimental.buildIterator

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

      val (queenLoc, health, resources,
        enemyQueenLoc, enemyHealth, enemyResources,
        obstacles, friendlyCreeps, enemyCreeps) = readInputs()

      fun getQueenAction(): String {
        try {
          // if touching a tower that isn't at max health, keep growing it
          val growingTower = obstacles
            .filter { it.owner == 0 && it.structureType == 1 && it.incomeRateOrHealthOrCooldown < 400 }
            .firstOrNull { it.location.distanceTo(queenLoc) - it.radius - QUEEN_RADIUS < 5 }

          if (growingTower != null) return "BUILD TOWER"

          // Queen goes to nearest unowned obstacle
          val queenTarget = obstacles
            .filter { it.owner == -1 }
            .minBy { it.location.distanceTo(queenLoc) - it.radius }

          if (queenTarget != null) {
            // if in range, do something there
            val dist = queenTarget.location.distanceTo(queenLoc) - QUEEN_RADIUS - queenTarget.radius

            return if (dist < 5) {
              nextBehaviour().also { nextBehaviour = behaviours.next() }
            } else {
              // move to it
              "MOVE ${queenTarget.location.x.toInt()} ${queenTarget.location.y.toInt()}"
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

      stdout.println(getQueenAction())
      stdout.println("TRAIN${getBuildOrders().joinToString("") { " " + it.obstacleId }}")
    }
  }
}