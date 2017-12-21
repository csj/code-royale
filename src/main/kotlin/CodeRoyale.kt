import com.codingame.gameengine.core.AbstractPlayer
import com.codingame.gameengine.core.GameManager
import com.codingame.gameengine.core.Referee
import com.codingame.gameengine.module.entities.Circle
import com.codingame.gameengine.module.entities.EntityManager
import com.google.inject.Inject
import java.util.*

class CodeRoyalePlayer : AbstractPlayer() {
  override fun getExpectedOutputLines(): Int = 3
  lateinit var kingUnit: Circle
  lateinit var engineerUnit: Circle
  lateinit var generalUnit: Circle

  val units by lazy { listOf(kingUnit, engineerUnit, generalUnit) }

  var health = 50
  var resources = 0
}

@Suppress("unused")  // injected by magic
class CodeRoyaleReferee : Referee {
  @Inject private lateinit var gameManager: GameManager<CodeRoyalePlayer>
  @Inject private lateinit var entityManager: EntityManager

  private var playerCount: Int = 2

  override fun init(playerCount: Int, params: Properties): Properties {
    this.playerCount = playerCount

    for (activePlayer in gameManager.activePlayers) {
      activePlayer.kingUnit = entityManager.createCircle()
        .setRadius(100)
        .setFillColor(activePlayer.color)
        .setX((Math.random() * 1920).toInt())
        .setY((Math.random() * 1080).toInt())

      activePlayer.engineerUnit = entityManager.createCircle()
        .setRadius(80)
        .setFillColor(activePlayer.color)
        .setX((Math.random() * 1920).toInt())
        .setY((Math.random() * 1080).toInt())

      activePlayer.generalUnit = entityManager.createCircle()
        .setRadius(60)
        .setFillColor(activePlayer.color)
        .setX((Math.random() * 1920).toInt())
        .setY((Math.random() * 1080).toInt())
    }

    // Params contains all the game parameters that has been to generate this game
    // For instance, it can be a seed number, the size of a grid/map, ...
    return params
  }

  override fun gameTurn(turn: Int) {
    // Code your game logic.
    // See README.md if you want some code to bootstrap your project.

    for (activePlayer in gameManager.activePlayers) {
      activePlayer.sendInputLine("${activePlayer.kingUnit.x} ${activePlayer.kingUnit.y}")
      activePlayer.sendInputLine("${activePlayer.engineerUnit.x} ${activePlayer.engineerUnit.y}")
      activePlayer.sendInputLine("${activePlayer.generalUnit.x} ${activePlayer.generalUnit.y}")

      for (otherPlayer in (gameManager.players - activePlayer)) {
        activePlayer.sendInputLine("${otherPlayer.kingUnit.x} ${otherPlayer.kingUnit.y}")
        activePlayer.sendInputLine("${otherPlayer.engineerUnit.x} ${otherPlayer.engineerUnit.y}")
        activePlayer.sendInputLine("${otherPlayer.generalUnit.x} ${otherPlayer.generalUnit.y}")
      }

      activePlayer.execute()
    }

    for (player in gameManager.activePlayers) {
      try {
        val outputs = player.outputs
        for ((unit, line) in player.units.zip(outputs)) {
          val (x,y) = line.split(" ").map { Integer.valueOf(it) }
          val current = Vector2(unit.x, unit.y)
          val desired = Vector2(x,y)
          val maxDist = 100.0
          if (current.distanceTo(desired) < maxDist) {
            unit.x = x
            unit.y = y
          } else {
            val newPos = current + (desired-current).resizedTo(maxDist)
            unit.x = newPos.x.toInt()
            unit.y = newPos.y.toInt()
          }
        }
      } catch (e: AbstractPlayer.TimeoutException) {
        e.printStackTrace()
        player.deactivate("${player.nickname}: timeout!")
      }
    }
  }
}
