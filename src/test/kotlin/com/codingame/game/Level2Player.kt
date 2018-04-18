import com.codingame.game.BasePlayer
import com.codingame.game.ObstacleInput
import java.io.InputStream
import java.io.PrintStream

class Level2Player(stdin: InputStream, stdout: PrintStream, stderr: PrintStream) : BasePlayer(stdin, stdout, stderr) {

  private fun myBarracks(): List<ObstacleInput> = obstacles.filter { it.owner == 0 && it.structureType == 2 }

  init {

    while (true) {
      val (queenLoc, _, _, _, _, _, obstacles, _, _) = readInputs()

      fun getQueenAction(): String {
        val queenTarget = obstacles
          .filter { it.owner == -1 || (it.owner == 0 && it.structureType == 1 && it.incomeRateOrHealthOrCooldown < 400) }
          .minBy { it.location.distanceTo(queenLoc) } ?: return "WAIT"

        val needsKnight = !obstacles.any { it.structureType == 2 && it.owner == 0 && it.attackRadiusOrCreepType == 0 }
        val needsArcher = !obstacles.any { it.structureType == 2 && it.owner == 0 && it.attackRadiusOrCreepType == 1 }
        val needsGiant = !obstacles.any { it.structureType == 2 && it.owner == 0 && it.attackRadiusOrCreepType == 2 }
        val needsTower = obstacles.count { it.structureType == 1 && it.owner == 0 } < 3

        return when {
          needsKnight -> "BUILD ${queenTarget.obstacleId} BARRACKS-KNIGHT"
          needsArcher -> "BUILD ${queenTarget.obstacleId} BARRACKS-ARCHER"
          needsGiant -> "BUILD ${queenTarget.obstacleId} BARRACKS-GIANT"
          needsTower -> "BUILD ${queenTarget.obstacleId} TOWER"
          else -> "WAIT"
        }
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
