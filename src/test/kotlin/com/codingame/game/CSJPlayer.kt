import com.codingame.game.*
import com.codingame.game.BasePlayer
import com.codingame.game.Constants.QUEEN_RADIUS
import java.io.InputStream
import java.io.PrintStream

class CSJPlayer(stdin: InputStream, stdout: PrintStream, stderr: PrintStream): BasePlayer(stdin, stdout, stderr) {
  var turn = 0

  init {

    while (true) {
      turn++

      val (queenLoc, _, resources, _, _, obstacles, _, enemyCreeps) = readInputs()

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
        .sumBy { CreepType.values()[it.attackRadiusOrCreepType].let { it.cost / it.buildTime } }

      val danger = enemyCreeps.any { it.location.distanceTo(queenLoc) < 300 }

      fun getQueenAction(): String {
        // if touching a tower that isn't at max health, keep growing it
        val growingTower = obstacles
          .filter { it.owner == 0 && it.structureType == 1 && it.incomeRateOrHealthOrCooldown < 400 }
          .firstOrNull { it.location.distanceTo(queenLoc) - it.radius - QUEEN_RADIUS < 5 }
        if (growingTower != null) return "BUILD ${growingTower.obstacleId} TOWER"

        // if touching a mine that isn't at max capacity, keep growing it
        val growingMine = obstacles
            .filter { it.owner == 0 && it.structureType == 0 && it.incomeRateOrHealthOrCooldown < it.maxResourceRate }
            .firstOrNull { it.location.distanceTo(queenLoc) - it.radius - QUEEN_RADIUS < 5 }
        if (growingMine != null) {
          return "BUILD ${growingMine.obstacleId} MINE"
        }

        val queenTarget = obstacles
          .filter { it.owner == -1 }
          .filter { target -> !obstacles.any {
            it.owner == 1 && it.structureType == 1 &&
              it.location.distanceTo(target.location) - it.attackRadiusOrCreepType - target.radius < -30 }}
          .minBy { it.location.distanceTo(queenLoc) - it.radius }

        if (queenTarget == null) {
          // bear toward closest friendly tower
          val closestTower = obstacles
            .filter { it.owner == 0 && it.structureType == 1 }
            .minBy { it.location.distanceTo(queenLoc) - it.radius }

          return closestTower?.let { "BUILD ${it.obstacleId} TOWER" } ?: "WAIT"
        }

        val dist = queenTarget.location.distanceTo(queenLoc) - QUEEN_RADIUS - queenTarget.radius

        if (dist < 5) {
          // Touching an obstacle; do something here
          if (danger) return "BUILD ${queenTarget.obstacleId} TOWER"
          if (totalIncome * 1.5 <= totalProduction)
            return if (queenTarget.minerals > 0) "BUILD ${queenTarget.obstacleId} MINE" else "BUILD ${queenTarget.obstacleId} TOWER"

          // count enemy towers; make sure we have a giant if they have more than 3
          val ourMelees = obstacles.count { it.owner == 0 && it.structureType == 2 && it.attackRadiusOrCreepType == 0 }
          val ourRanged = obstacles.count { it.owner == 0 && it.structureType == 2 && it.attackRadiusOrCreepType == 1 }
          val ourGiants = obstacles.count { it.owner == 0 && it.structureType == 2 && it.attackRadiusOrCreepType == 2 }
          val theirTowers = obstacles.count { it.owner == 1 && it.structureType == 1 }

          val barracksType = when {
            theirTowers >= 2 && ourGiants == 0 -> CreepType.GIANT
            ourMelees > ourRanged -> CreepType.RANGED
            else -> CreepType.MELEE
          }
          return "BUILD ${queenTarget.obstacleId} BARRACKS-$barracksType"
        }

        return queenTarget.let { "BUILD ${it.obstacleId} TOWER"}
      }

      fun getTrainOrders(): List<ObstacleInput> {
        val myBarracks = obstacles.filter { it.owner == 0 && it.structureType == 2 }

        if (myBarracks.isEmpty()) return listOf()
        if (myBarracks.any { it.incomeRateOrHealthOrCooldown > 0 }) return listOf()
        val totalCost = myBarracks.sumBy { CreepType.values()[it.attackRadiusOrCreepType].cost }
        if (resources < totalCost) return listOf()
        return myBarracks
      }

      try {
        stdout.println(getQueenAction())
        stdout.println("TRAIN${getTrainOrders().joinToString("") { " " + it.obstacleId }}")
      } catch (ex: Exception) {
        ex.printStackTrace(stderr)
        throw ex
      }

    }
  }
}
