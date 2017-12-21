import java.io.InputStream
import java.io.PrintStream
import java.util.*

class TestPlayer(stdin: InputStream, stdout: PrintStream, stderr: PrintStream) {
  init {
    val scanner = Scanner(stdin)

    fun readTwoNumberOffLine(): List<Int> {
      return listOf(scanner.nextInt(), scanner.nextInt())
    }

    while (true) {
      val (kx, ky) = readTwoNumberOffLine()
      val (ex, ey) = readTwoNumberOffLine()
      val (gx, gy) = readTwoNumberOffLine()

      val (kx2, ky2) = readTwoNumberOffLine()
      val (ex2, ey2) = readTwoNumberOffLine()
      val (gx2, gy2) = readTwoNumberOffLine()

      stdout.println("1000 1000") // king goes somewhere specific
      stdout.println("$kx $ky") // engineer chases our king
      stdout.println("$kx2 $ky2") // general chases their king
    }
  }
}