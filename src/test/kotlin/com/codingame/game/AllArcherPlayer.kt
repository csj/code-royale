import com.codingame.game.BasePlayer
import com.codingame.game.Constants.QUEEN_RADIUS
import com.codingame.game.Constants.TOUCHING_DELTA
import com.codingame.game.ObstacleInput
import java.io.InputStream
import java.io.PrintStream

class AllArcherPlayer(stdin: InputStream, stdout: PrintStream, stderr: PrintStream): BasePlayer(stdin, stdout, stderr) {

  private fun myBarracks(): List<ObstacleInput> = obstacles.filter { it.owner == 0 && it.structureType == 2 }

  init {

    while (true) {
      val (queenLoc, _, _, _, _, _, obstacles, _, _) = readInputs()

      // strategy:
      // build all mines
      // build barracks, knight, anytime our income exceeds our ability to spam units
      // spam knight units forever

      fun getQueenAction(): String {

        // if touching a mine that isn't at max capacity, keep growing it
        val growingMine = obstacles
            .filter { it.owner == 0 && it.structureType == 0 && it.incomeRateOrHealthOrCooldown < it.maxMineSize }
            .firstOrNull { it.location.distanceTo(queenLoc).toDouble - it.radius - QUEEN_RADIUS < TOUCHING_DELTA }

        if (growingMine != null) {
          stderr.println("Max: ${growingMine.maxMineSize}")
          return "BUILD ${growingMine.obstacleId} MINE"
        }

        val queenTarget = obstacles
          .filter { it.owner == -1 }
          .minBy { it.location.distanceTo(queenLoc) } ?: return "WAIT"

        val income = obstacles
          .filter { it.owner == 0 && it.structureType == 0 }
          .sumBy { it.incomeRateOrHealthOrCooldown }

        val maxUnitSpend = (myBarracks().size + 0.5) * 16 //  = 80/5
        val needsBarracks = income >= maxUnitSpend

        return "BUILD ${queenTarget.obstacleId} ${if(needsBarracks) "BARRACKS-ARCHER" else "MINE"}"
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
