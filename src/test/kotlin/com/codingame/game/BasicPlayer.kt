import com.codingame.game.BasePlayer
import com.codingame.game.Constants.KING_RADIUS
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
            val dist = kingTarget.location.distanceTo(kingLoc) - KING_RADIUS - kingTarget.radius

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