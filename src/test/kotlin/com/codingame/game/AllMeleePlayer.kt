import com.codingame.game.BasePlayer
import com.codingame.game.Constants
import com.codingame.game.ObstacleInput
import java.io.InputStream
import java.io.PrintStream

class AllMeleePlayer(stdin: InputStream, stdout: PrintStream, stderr: PrintStream): BasePlayer(stdin, stdout, stderr) {

  private fun myBarracks(): List<ObstacleInput> = obstacles.filter { it.owner == 0 && it.structureType == 2 }

  init {

    while (true) {
      val (queenLoc, _, _, _, _, _, obstacles, _, _) = readInputs()

      // strategy:
      // build all mines
      // build barracks, melee, anytime our income exceeds our ability to spam units
      // spam melee units forever

      fun getQueenAction(): String {

        // if touching a mine that isn't at max capacity, keep growing it
        val growingMine = obstacles
            .filter { it.owner == 0 && it.structureType == 0 && it.incomeRateOrHealthOrCooldown < it.maxResourceRate }
            .firstOrNull { it.location.distanceTo(queenLoc) - it.radius - Constants.QUEEN_RADIUS < 5 }

        if (growingMine != null) {
          stderr.println("Max: ${growingMine.maxResourceRate}")
          return "BUILD MINE"
        }

        val queenTarget = obstacles
          .filter { it.owner == -1 }
          .minBy { it.location.distanceTo(queenLoc) - it.radius } ?: return "WAIT"

        val income = obstacles
          .filter { it.owner == 0 && it.structureType == 0 }
          .sumBy { it.incomeRateOrHealthOrCooldown }

        val maxUnitSpend = (myBarracks().size + 0.5) * 16 //  = 80/5
        val needsBarracks = income >= maxUnitSpend

        return "BUILDONOBSTACLE ${queenTarget.obstacleId} ${if(needsBarracks) "BARRACKS MELEE" else "MINE"}"
      }

      try {
        stdout.println(getQueenAction())
        stdout.println("TRAIN${myBarracks().joinToString("") { " " + it.obstacleId }}")
      } catch (ex: Exception) {
        ex.printStackTrace(stderr)
        throw ex
      }

    }
  }
}
