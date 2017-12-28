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
  abstract val mass: Int   // 0 := immovable
  abstract val entity: Circle

  fun updateEntity() {
    entity.x = location.x.toInt()
    entity.y = location.y.toInt()
  }
}

abstract class MyOwnedEntity(val owner: CodeRoyalePlayer) : MyEntity()

class Zergling(entityManager: EntityManager, owner: CodeRoyalePlayer):
  Creep(entityManager, owner, 80, 0, 10, 400)

class Archer(entityManager: EntityManager, owner: CodeRoyalePlayer):
  Creep(entityManager, owner, 60, 200, 15, 900)

class Obstacle(entityManager: EntityManager): MyEntity() {
  override val mass = 0

  private val radius = (Math.random() * 70.0 + 50.0).toInt()

  override val entity = entityManager.createCircle()
    .setRadius(radius)
    .setFillColor(0)!!

  init {
    this.location = Vector2.random(1920, 1080)
  }
}

abstract class Creep(
  entityManager: EntityManager,
  owner: CodeRoyalePlayer,
  val speed: Int,
  val attackRange: Int,
  radius: Int,
  override val mass: Int
) : MyOwnedEntity(owner) {

  override val entity = entityManager.createCircle()
    .setRadius(radius)
    .setFillColor(owner.color)!!

  fun act() {
    val enemyKing = owner.enemyPlayer.kingUnit
    // move toward enemy king, if not yet in range
    if (location.distanceTo(enemyKing.location) - entity.radius - enemyKing.entity.radius > attackRange)
      location = location.towards((enemyKing.location + (location - enemyKing.location).resizedTo(3.0)), speed.toDouble())
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

  private var obstacles: List<Obstacle> = listOf()

  fun allUnits(): List<MyEntity> = gameManager.players.flatMap { it.allUnits() } + obstacles

  override fun init(playerCount: Int, params: Properties): Properties {
    this.playerCount = playerCount

    gameManager.players[0].enemyPlayer = gameManager.players[1]
    gameManager.players[1].enemyPlayer = gameManager.players[0]
    gameManager.players[1].inverted = true

    for (activePlayer in gameManager.activePlayers) {
      val makeUnit: (Int, Int) -> MyOwnedEntity = { radius, mass ->
        object : MyOwnedEntity(activePlayer) {
          override val entity = entityManager.createCircle()
            .setRadius(radius)
            .setFillColor(activePlayer.color)

          override val mass = mass

          init {
            location = Vector2.random(1920, 1080)
          }
        }
      }

      activePlayer.kingUnit = makeUnit(30, 10000)
      activePlayer.engineerUnit = makeUnit(20, 6400)
      activePlayer.generalUnit = makeUnit(25, 3600)

      repeat(5, { activePlayer.activeCreeps += Zergling(entityManager, activePlayer) })
      repeat(5, { activePlayer.activeCreeps += Archer(entityManager, activePlayer) })

      activePlayer.allUnits().forEach { it.updateEntity() }
    }

    obstacles = (0..29).map { Obstacle(entityManager) }
    fixCollisions(60.0)

    allUnits().forEach { it.updateEntity() }

    // Params contains all the game parameters that has been to generate this game
    // For instance, it can be a seed number, the size of a grid/map, ...
    return params
  }

  fun fixCollisions(acceptableGap: Double) {
    loop@ for (iter in 0..999) {
      var foundAny = false

      for (u1 in allUnits()) {
        val rad = u1.entity.radius.toDouble()
        val clampDist = if (u1.mass == 0) 60 + rad else rad
        u1.location = u1.location.clampWithin(clampDist, 1920-clampDist, clampDist, 1080-clampDist)

        for (u2 in allUnits()) {
          if (u1 != u2) {
            val overlap = u1.entity.radius + u2.entity.radius + acceptableGap - u1.location.distanceTo(u2.location)
            if (overlap > 0) {
              val (d1, d2) = when {
                u1.mass == 0 && u2.mass == 0 -> Pair(0.5, 0.5)
                u1.mass == 0 -> Pair(0.0, 1.0)
                u2.mass == 0 -> Pair(1.0, 0.0)
                else -> Pair(u2.mass.toDouble() / (u1.mass + u2.mass), u1.mass.toDouble() / (u1.mass + u2.mass))
              }

//              System.err.println("${u1.location} ${u2.location} before; overlap is $overlap, d1 = $d1, d2 = $d2")
              val u1tou2 = u2.location - u1.location
              val gap = if (u1.mass == 0 && u2.mass == 0) 20 else 1

              u1.location -= u1tou2.resizedTo(d1 * overlap + if (u1.mass == 0 && u2.mass > 0) 0 else gap)
              u2.location += u1tou2.resizedTo(d2 * overlap + if (u2.mass == 0 && u1.mass > 0) 0 else gap)
//              System.err.println("${u1.location} ${u2.location} after")

              foundAny = true
//              continue@loop
            }
          }
        }
      }
      if (!foundAny) break
    }
  }

  override fun gameTurn(turn: Int) {
    // Code your game logic.
    // See README.md if you want some code to bootstrap your project.

    gameManager.activePlayers
      .flatMap { it.activeCreeps }
      .forEach { it.act() }

    fixCollisions(0.0)

    allUnits().forEach { it.updateEntity() }

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
