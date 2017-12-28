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
        turn > 350 -> stdout.println("100 100") // king goes somewhere specific
        turn > 300 -> stdout.println("1600 800") // king goes somewhere specific
        turn > 250 -> stdout.println("700 300") // king goes somewhere specific
        turn > 200 -> stdout.println("1400 200") // king goes somewhere specific
        turn > 150 -> stdout.println("300 700") // king goes somewhere specific
        turn > 100 -> stdout.println("800 400") // king goes somewhere specific
        turn > 50 -> stdout.println("100 800") // king goes somewhere specific
        else -> stdout.println("1800 300") // king goes somewhere specific
      }

      stdout.println("$kx $ky") // engineer chases our king
      stdout.println("$kx2 $ky2") // general chases their king
    }
  }
}