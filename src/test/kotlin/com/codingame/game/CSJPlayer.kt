import com.codingame.game.BasePlayer
import com.codingame.game.Constants
import com.codingame.game.CreepType
import com.codingame.game.ObstacleInput
import java.io.InputStream
import java.io.PrintStream

class CSJPlayer(stdin: InputStream, stdout: PrintStream, stderr: PrintStream): BasePlayer(stdin, stdout, stderr) {
  var turn = 0

  init {

    while (true) {
      turn++

      val (kingLoc, _, resources, _, _, _, obstacles, _, enemyCreeps) = readInputs()

      // strategy:
      // build mines
      // when they build their first barracks, build our first tower, and stay near it
      // fan out and add more mines
      // try to keep as much production as we have income (rotate creep types)
      // schedule barracks to fire at the same time

      val totalIncome = obstacles
        .filter { it.owner == 0 && it.structureType == 0 }
        .sumBy { it.incomeRateOrHealthOrCooldown }

      val totalProduction = obstacles
        .filter { it.owner == 0 && it.structureType == 2 }
        .sumBy { CreepType.values()[it.attackRadiusOrCreepType].let { it.cost / it.cooldown } }

      val danger = enemyCreeps.any { it.location.distanceTo(kingLoc) < 300 }

      fun getKingAction(): String {
        // if touching a tower that isn't at max health, keep growing it
        val growingTower = obstacles
          .filter { it.owner == 0 && it.structureType == 1 && it.incomeRateOrHealthOrCooldown < 400 }
          .firstOrNull { it.location.distanceTo(kingLoc) - it.radius - Constants.KING_RADIUS < 5 }

        if (growingTower != null) return "BUILD TOWER"

        val kingTarget = obstacles
          .filter { it.owner == -1 }
          .minBy { it.location.distanceTo(kingLoc) - it.radius }

        if (kingTarget == null) {
          // bear toward closest friendly tower
          val closestTower = obstacles
            .filter { it.owner == 0 && it.structureType == 1 }
            .minBy { it.location.distanceTo(kingLoc) - it.radius }

          return closestTower?.let { "MOVE ${it.location.x.toInt()} ${it.location.y.toInt()}" } ?: "WAIT"
        }

        val dist = kingTarget.location.distanceTo(kingLoc) - Constants.KING_RADIUS - kingTarget.radius

        if (dist < 5) {
          // Touching an obstacle; do something here
          if (danger) return "BUILD TOWER"
          if (totalIncome * 1.5 <= totalProduction && kingTarget.minerals > 0) return "BUILD MINE"

          // count enemy towers; make sure we have a giant if they have more than 3
          val ourMelees = obstacles.count { it.owner == 0 && it.structureType == 2 && it.attackRadiusOrCreepType == 0 }
          val ourRanged = obstacles.count { it.owner == 0 && it.structureType == 2 && it.attackRadiusOrCreepType == 1 }
          val ourGiants = obstacles.count { it.owner == 0 && it.structureType == 2 && it.attackRadiusOrCreepType == 2 }
          val theirTowers = obstacles.count { it.owner == 1 && it.structureType == 1 }

          val barracksType = when {
            theirTowers >= 3 && ourGiants == 0 -> CreepType.GIANT
            ourMelees > ourRanged -> CreepType.RANGED
            else -> CreepType.MELEE
          }
          return "BUILD BARRACKS $barracksType"
        }

        return kingTarget.let { "MOVE ${it.location.x.toInt()} ${it.location.y.toInt()}"}
      }

      fun getBuildOrders(): List<ObstacleInput> {
        val myBarracks = obstacles.filter { it.owner == 0 && it.structureType == 2 }

        if (myBarracks.isEmpty()) return listOf()
        if (myBarracks.any { it.incomeRateOrHealthOrCooldown > 0 }) return listOf()
        val totalCost = myBarracks.sumBy { CreepType.values()[it.attackRadiusOrCreepType].cost }
        if (resources < totalCost) return listOf()
        return myBarracks
      }

      try {
        stdout.println(getKingAction())
        stdout.println("TRAIN${getBuildOrders().joinToString("") { " " + it.obstacleId }}")
      } catch (ex: Exception) {
        ex.printStackTrace(stderr)
        throw ex
      }

    }
  }
}
