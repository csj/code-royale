import com.codingame.gameengine.runner.GameRunner
import java.io.InputStream
import java.io.PrintStream
import java.util.*


object Main {
  @JvmStatic
  fun main(args: Array<String>) {

    val gameRunner = GameRunner()

    // Adds as many player as you need to test your game
    gameRunner.addJavaPlayer(CSJPlayer::class.java)
    gameRunner.addJavaPlayer(CSJPlayer::class.java)

    // gameRunner.addCommandLinePlayer("python3 /home/user/player.py");

    gameRunner.start()
  }
}


class CSJPlayer(stdin: InputStream, stdout: PrintStream, stderr: PrintStream) {
  init {
    val scanner = Scanner(stdin)
    stderr.println("Hi there")

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

      // king goes to 0,0
      stdout.println("1000 1000")
      stdout.println("$kx $ky") // engineer chases our king
      stdout.println("$kx2 $ky2") // general chases their king
    }
  }
}