import com.codingame.game.BasePlayer
import com.codingame.game.Constants
import java.io.InputStream
import java.io.PrintStream

class AllTowersPlayer(stdin: InputStream, stdout: PrintStream, stderr: PrintStream): BasePlayer(stdin, stdout, stderr) {
  init {
    while (true) {
      val (kingLoc, _, _, _, _, _, obstacles, _, _) = readInputs()

      fun getKingAction(): String {
        // if touching a tower that isn't at max health, keep growing it
        val growingTower = obstacles
          .filter { it.owner == 0 && it.structureType == 1 && it.incomeRateOrHealthOrCooldown < 400 }
          .firstOrNull { it.location.distanceTo(kingLoc) - it.radius - Constants.KING_RADIUS < 5 }

        if (growingTower != null) return "BUILD TOWER"

        val kingTarget = obstacles
          .filter { it.owner == -1 }
          .minBy { it.location.distanceTo(kingLoc) - it.radius } ?: return "WAIT"

        return "BUILDONOBSTACLE ${kingTarget.obstacleId} TOWER"
      }

      stdout.println(getKingAction())
      stdout.println("TRAIN")
    }
  }
}