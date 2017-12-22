import com.codingame.gameengine.core.AbstractPlayer
import com.codingame.gameengine.core.GameManager
import com.codingame.gameengine.core.Referee
import com.codingame.gameengine.module.entities.Circle
import com.codingame.gameengine.module.entities.EntityManager
import com.google.inject.Inject
import java.util.*

class CodeRoyalePlayer : AbstractPlayer() {
  override fun getExpectedOutputLines(): Int = 3
  lateinit var kingUnit: MyOwnedEntity
  lateinit var engineerUnit: MyOwnedEntity
  lateinit var generalUnit: MyOwnedEntity
  lateinit var enemyPlayer: CodeRoyalePlayer
  var inverted: Boolean = false

  fun fixLocation(location: Vector2) = if (inverted) Vector2(1920, 1080) - location else location

  fun printLocation(location: Vector2) {
    val (x,y) = fixLocation(location)
    sendInputLine("${x.toInt()} ${y.toInt()}")
  }

  val units by lazy { listOf(kingUnit, engineerUnit, generalUnit) }
  val activeCreeps = mutableListOf<Creep>()

  fun allUnits() = units + activeCreeps

  var health = 50
  var resources = 0
}

abstract class MyEntity {
  var location = Vector2.Zero
  abstract val entity: Circle

  fun updateEntity() {
    entity.x = location.x.toInt()
    entity.y = location.y.toInt()
  }
}

abstract class MyOwnedEntity(val owner: CodeRoyalePlayer) : MyEntity()

class Creep(entityManager: EntityManager, owner: CodeRoyalePlayer) : MyOwnedEntity(owner) {
  override val entity = entityManager.createCircle()
    .setRadius(20)
    .setFillColor(owner.color)

  fun act() {
    // move toward enemy king
    location = location.towards(owner.enemyPlayer.kingUnit.location, 80.0)
  }

  init {
    location = Vector2.random(1920, 1080)
  }
}

@Suppress("unused")  // injected by magic
class CodeRoyaleReferee : Referee {
  @Inject private lateinit var gameManager: GameManager<CodeRoyalePlayer>
  @Inject private lateinit var entityManager: EntityManager

  private var playerCount: Int = 2

  fun allUnits() = gameManager.players.flatMap { it.allUnits() }

  override fun init(playerCount: Int, params: Properties): Properties {
    this.playerCount = playerCount

    gameManager.players[0].enemyPlayer = gameManager.players[1]
    gameManager.players[1].enemyPlayer = gameManager.players[0]
    gameManager.players[1].inverted = true

    for (activePlayer in gameManager.activePlayers) {
      val makeUnit: (Int) -> MyOwnedEntity = { radius ->
        object : MyOwnedEntity(activePlayer) {
          override val entity = entityManager.createCircle()
            .setRadius(radius)
            .setFillColor(activePlayer.color)

          init {
            location = Vector2.random(1920, 1080)
          }
        }
      }

      activePlayer.kingUnit = makeUnit(100)
      activePlayer.engineerUnit = makeUnit(80)
      activePlayer.generalUnit = makeUnit(60)

      repeat(10, { activePlayer.activeCreeps += Creep(entityManager, activePlayer) })

      activePlayer.allUnits().forEach { it.updateEntity() }
    }

    // Params contains all the game parameters that has been to generate this game
    // For instance, it can be a seed number, the size of a grid/map, ...
    return params
  }

  override fun gameTurn(turn: Int) {
    // Code your game logic.
    // See README.md if you want some code to bootstrap your project.

    gameManager.activePlayers
      .flatMap { it.activeCreeps }
      .forEach { it.act() }

    // fix collisions: mass ~= radius^2
    do {
      var foundAny = false

      allUnits().forEach { u1 ->
        allUnits().forEach { u2 ->
          if (u1 != u2) {
            if (u1.location.distanceTo(u2.location) - u1.entity.radius - u2.entity.radius < 0) {
              foundAny = true
              u1.location += (u1.location - u2.location).resizedTo(10000.0 / u1.entity.radius / u1.entity.radius)
            }
          }
        }
      }
    } while (foundAny)

    gameManager.activePlayers
      .flatMap { it.allUnits() }
      .forEach { it.updateEntity() }

    for (activePlayer in gameManager.activePlayers) {
      activePlayer.units.forEach { activePlayer.printLocation(it.location) }
      activePlayer.enemyPlayer.units.forEach { activePlayer.printLocation(it.location) }

      activePlayer.execute()
    }

    for (player in gameManager.activePlayers) {
      try {
        val outputs = player.outputs
        for ((unit, line) in player.units.zip(outputs)) {
          val (x,y) = line.split(" ").map { Integer.valueOf(it) }
          unit.location = unit.location.towards(player.fixLocation(Vector2(x,y)), 40.0)
        }
      } catch (e: AbstractPlayer.TimeoutException) {
        e.printStackTrace()
        player.deactivate("${player.nickname}: timeout!")
      }
    }
  }
}
