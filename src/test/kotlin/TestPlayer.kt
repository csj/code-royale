import java.io.InputStream
import java.io.PrintStream
import java.util.*

data class ObstacleInput(
  val location: Vector2,
  val radius: Int,
  val incomeOwner: Int,
  val incomeTimer: Int,
  val towerOwner: Int,
  val towerHealth: Int,
  val towerRange: Int
)

class TestPlayer(stdin: InputStream, stdout: PrintStream, stderr: PrintStream) {
  init {
    val scanner = Scanner(stdin)
    fun readTwoNumbers() = Pair(scanner.nextInt(), scanner.nextInt())
    fun readObstacle() = ObstacleInput(
      Vector2(scanner.nextInt(), scanner.nextInt()), scanner.nextInt(), scanner.nextInt(),
      scanner.nextInt(), scanner.nextInt(), scanner.nextInt(), scanner.nextInt()
    )

    var turn = 0
    var nextZerglings = true

    while (true) {
      turn++

      val (kx, ky) = readTwoNumbers(); val kingLoc = Vector2(kx, ky)
      val (ex, ey) = readTwoNumbers(); val engLoc = Vector2(ex, ey)
      val (gx, gy) = readTwoNumbers()
      val (health, resources) = readTwoNumbers()

      val (kx2, ky2) = readTwoNumbers()
      val (ex2, ey2) = readTwoNumbers()
      val (gx2, gy2) = readTwoNumbers(); val enemyGenLoc = Vector2(gx2, gy2)
      val (enemyHealth, enemyResources) = readTwoNumbers()

      val numObstacles = scanner.nextInt()
      val obstacles = (0 until numObstacles).map { readObstacle() }

      val kingToEnemyGeneral = kingLoc.distanceTo(enemyGenLoc)
      val kingTarget = if (kingToEnemyGeneral < 200) {
        // if King is close to enemy general, run away!
        kingLoc + (kingLoc - enemyGenLoc).resizedTo(100.0)
      } else {
        // King goes to nearest untagged obstacle under friendly tower influence that doesn't have an enemy tower, or else our engineer
        obstacles
          .filter { it.incomeOwner != 0 && it.towerOwner != 1 }
          .filter { target ->
            obstacles.any {
              it.towerOwner == 0 && it.location.distanceTo(target.location) - target.radius - it.towerRange < 50
            }
          }
          .minBy { it.location.distanceTo(kingLoc) }?.location ?: engLoc
      }

      stdout.println("MOVE ${kingTarget.x.toInt()} ${kingTarget.y.toInt()}")

      // TODO: engineer grows towers to 200 health near our king
      val closestObstacleToEng = obstacles
        .filter { it.towerOwner != 1 }
        .minBy { it.location.distanceTo(engLoc) }!!
      val dist = closestObstacleToEng.location.distanceTo(engLoc) - closestObstacleToEng.radius - 20
      val engTarget = if (dist < 5 && (closestObstacleToEng.towerOwner != 0 || closestObstacleToEng.towerHealth < 400))
        closestObstacleToEng.location
      else {
        obstacles
          .filter { it.towerOwner != 0 }
          .minBy { it.location.distanceTo(kingLoc) }!!.location
      }

      stdout.println("MOVE ${engTarget.x.toInt()} ${engTarget.y.toInt()}")

      // general chases enemy king
      when {
        nextZerglings && resources >= 40 -> { stdout.println("SPAWN ZERGLINGS"); nextZerglings = false }
        !nextZerglings && resources >= 70 -> { stdout.println("SPAWN ARCHERS"); nextZerglings = true }
        else -> stdout.println("MOVE $kx2 $ky2")
      }
    }
  }
}