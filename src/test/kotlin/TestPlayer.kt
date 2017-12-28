import java.io.InputStream
import java.io.PrintStream
import java.util.*

class TestPlayer(stdin: InputStream, stdout: PrintStream, stderr: PrintStream) {
  init {
    val scanner = Scanner(stdin)

    fun readTwoNumberOffLine(): List<Int> {
      return listOf(scanner.nextInt(), scanner.nextInt())
    }

    var turn = 0

    while (true) {
      turn++
      val (kx, ky) = readTwoNumberOffLine()
      val (ex, ey) = readTwoNumberOffLine()
      val (gx, gy) = readTwoNumberOffLine()

      val (kx2, ky2) = readTwoNumberOffLine()
      val (ex2, ey2) = readTwoNumberOffLine()
      val (gx2, gy2) = readTwoNumberOffLine()

      when {
        turn > 350 -> stdout.println("MOVE 100 100") // king goes somewhere specific
        turn > 300 -> stdout.println("MOVE 1600 800") // king goes somewhere specific
        turn > 250 -> stdout.println("MOVE 700 300") // king goes somewhere specific
        turn > 200 -> stdout.println("MOVE 1400 200") // king goes somewhere specific
        turn > 150 -> stdout.println("MOVE 300 700") // king goes somewhere specific
        turn > 100 -> stdout.println("MOVE 800 400") // king goes somewhere specific
        turn > 50 -> stdout.println("MOVE 100 800") // king goes somewhere specific
        else -> stdout.println("MOVE 1800 300") // king goes somewhere specific
      }

      stdout.println("MOVE $kx $ky") // engineer chases our king
      when {
        turn % 15 == 0 -> stdout.println("SPAWN ZERGLINGS")
        turn % 23 == 0 -> stdout.println("SPAWN ARCHERS")
        else -> stdout.println("MOVE 1000 800") // general goes somewhere specific
      }
    }
  }
}