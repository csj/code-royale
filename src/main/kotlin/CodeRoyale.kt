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
  private fun fixOwner(player: CodeRoyalePlayer?) = when (player) { null -> -1; this -> 0; else -> 1 }

  fun printLocation(location: Vector2) {
    val (x,y) = fixLocation(location)
    sendInputLine("${x.toInt()} ${y.toInt()}")
  }

  fun printObstacle(obstacle: Obstacle) {
    val (x,y) = fixLocation(obstacle.location)
    val toks = listOf(
      x.toInt(),y.toInt(),
      obstacle.radius,
      fixOwner(obstacle.incomeOwner),
      obstacle.incomeTimer ?: -1,
      fixOwner(obstacle.towerOwner),
      obstacle.towerHealth,
      obstacle.towerAttackRadius
    )
    sendInputLine(toks.joinToString(" "))
  }


  val units by lazy { listOf(kingUnit, engineerUnit, generalUnit) }
  val activeCreeps = mutableListOf<Creep>()

  fun allUnits() = units + activeCreeps

  var health = 200
  val maxHealth = 200

  fun checkKingHealth(): Boolean {
    kingUnit.entity.fillAlpha = 0.8 * health / maxHealth + 0.2
    if (health <= 0) kingUnit.entity.fillAlpha = 0.0
    return health > 0
  }

  var resources = 0
}

abstract class MyEntity {
  var location = Vector2.Zero
  abstract val mass: Int   // 0 := immovable
  abstract val entity: Circle

  open fun updateEntity() {
    entity.x = location.x.toInt()
    entity.y = location.y.toInt()
    entity.zIndex = 50
  }
}

abstract class MyOwnedEntity(val owner: CodeRoyalePlayer) : MyEntity()

class Zergling(entityManager: EntityManager, owner: CodeRoyalePlayer, location: Vector2? = null):
  Creep(entityManager, owner, 80, 0, 10, 400, 30, location)

class Archer(entityManager: EntityManager, owner: CodeRoyalePlayer, location: Vector2? = null):
  Creep(entityManager, owner, 60, 200, 15, 900, 45, location)

class Obstacle(entityManager: EntityManager): MyEntity() {
  override val mass = 0

  val radius = (Math.random() * 50.0 + 60.0).toInt()
  private val area = Math.PI * radius * radius

  var incomeOwner: CodeRoyalePlayer? = null
  var incomeTimer: Int? = null

  var towerOwner: CodeRoyalePlayer? = null
  var towerAttackRadius: Int = 0
  var towerHealth: Int = 0

  override val entity: Circle = entityManager.createCircle()
    .setRadius(radius)
    .setFillColor(0)
    .setFillAlpha(0.5)
    .setZIndex(100)

  private val towerRangeCircle = entityManager.createCircle()
    .setFillAlpha(0.15)
    .setFillColor(0)
    .setLineColor(0)
    .setZIndex(10)

  private val towerSquare = entityManager.createRectangle()
    .setHeight(30).setWidth(30)
    .setLineColor(0xbbbbbb)
    .setLineWidth(4)
    .setFillColor(0)
    .setZIndex(150)

  fun updateEntities() {
    if (towerOwner != null) {
      towerRangeCircle.isVisible = true
      towerRangeCircle.fillColor = towerOwner!!.color
      towerRangeCircle.radius = towerAttackRadius
      towerSquare.isVisible = true
      towerSquare.fillColor = towerOwner!!.color
    } else {
      entity.fillColor = 0
      towerRangeCircle.isVisible = false
      towerSquare.isVisible = false
    }
    if (incomeOwner == null) entity.fillColor = 0
    else entity.fillColor = incomeOwner!!.color



    towerSquare.x = location.x.toInt() - 15
    towerSquare.y = location.y.toInt() - 15
    towerRangeCircle.x = location.x.toInt()
    towerRangeCircle.y = location.y.toInt()
  }

  fun act() {
    if (towerOwner != null) {
      val closestEnemy = towerOwner!!.enemyPlayer.activeCreeps.minBy { it.location.distanceTo(location) }
      if (closestEnemy != null && closestEnemy.location.distanceTo(location) < towerAttackRadius) {
        closestEnemy.damage(6 + (Math.random() * 3).toInt())
      }

      val enemyGeneral = towerOwner!!.enemyPlayer.generalUnit
      if (enemyGeneral.location.distanceTo(location) < towerAttackRadius) {
        enemyGeneral.location += (enemyGeneral.location - location).resizedTo(20.0)
      }

      towerHealth -= 10
      towerAttackRadius = Math.sqrt((towerHealth * 1000 + area) / Math.PI).toInt()

      if (towerHealth < 0) {
        towerHealth = -1
        towerOwner = null
      }
    }
    if (incomeOwner != null) {
      incomeOwner!!.resources += 1
    }
    updateEntities()
  }

  init {
    this.location = Vector2.random(1920, 1080)
  }

  fun setTower(owner: CodeRoyalePlayer, health: Int) {
    towerOwner = owner
    this.towerHealth = health
  }
}

abstract class Creep(
  entityManager: EntityManager,
  owner: CodeRoyalePlayer,
  val speed: Int,
  val attackRange: Int,
  radius: Int,
  override val mass: Int,
  var health: Int,
  location: Vector2?
) : MyOwnedEntity(owner) {

  val maxHealth = health

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
    this.location = location ?: Vector2.random(1920, 1080)
    updateEntity()
  }

  override fun updateEntity() {
    super.updateEntity()
    entity.fillAlpha = health.toDouble() / maxHealth * 0.8 + 0.2
  }

  fun damage(hp: Int) {
    health -= hp
    if (health <= 0) destroy()
  }

  fun destroy() {
    entity.isVisible = false
    owner.activeCreeps.remove(this)
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
      val makeUnit: (Int, Int, Vector2) -> MyOwnedEntity = { radius, mass, location ->
        object : MyOwnedEntity(activePlayer) {
          override val entity = entityManager.createCircle()
            .setRadius(radius)
            .setFillColor(activePlayer.color)

          override val mass = mass

          init {
            this.location = location
          }
        }
      }

      activePlayer.kingUnit = makeUnit(30, 10000, activePlayer.fixLocation(Vector2.Zero))
      activePlayer.engineerUnit = makeUnit(20, 6400, activePlayer.fixLocation(Vector2.Zero))
      activePlayer.generalUnit = makeUnit(25, 3600, activePlayer.fixLocation(Vector2.Zero))

      activePlayer.allUnits().forEach { it.updateEntity() }
    }

    obstacles = (0..29).map { Obstacle(entityManager) }
    fixCollisions(60.0)
    obstacles.forEach { it.updateEntities() }

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

    gameManager.players.forEach {
      val king = it.kingUnit
      val obsK = obstacles.minBy { it.location.distanceTo(king.location) }!!

      // TODO: What if both kings are touching the same one!
      if (obsK.location.distanceTo(king.location) - obsK.radius - 30 < 10) {
        obsK.incomeOwner = it
        obsK.incomeTimer = 40
      }

      // TODO: What if both engineers are touching the same one!
      val eng = it.engineerUnit
      val obsE = obstacles.minBy { it.location.distanceTo(eng.location) }!!
      if (obsE.location.distanceTo(eng.location) - obsE.radius - eng.entity.radius < 10) {
        if (obsE.towerOwner == it) {
          obsE.towerHealth += 100
        } else if (obsE.towerOwner == null) {
          obsE.setTower(it, 200)
        }
      }
    }
    gameManager.activePlayers.flatMap { it.activeCreeps }.toList().forEach { it.damage(1) }
    obstacles.forEach { it.act() }
    gameManager.activePlayers.flatMap { it.activeCreeps }.forEach { it.act() }
    fixCollisions(0.0)
    gameManager.activePlayers.flatMap { it.activeCreeps }.forEach {
      val enemyKing = it.owner.enemyPlayer.kingUnit
      if (it.location.distanceTo(enemyKing.location) < it.entity.radius + enemyKing.entity.radius + it.attackRange + 10) {
        it.owner.enemyPlayer.health -= 1
      }
    }

    gameManager.activePlayers.forEach {
      if (!it.checkKingHealth()) {
        it.deactivate("Dead king")
      }
    }

    if (gameManager.activePlayers.size < 2) {
      gameManager.endGame()
    }

    allUnits().forEach { it.updateEntity() }

    for (activePlayer in gameManager.activePlayers) {
      activePlayer.units.forEach { activePlayer.printLocation(it.location) }
      activePlayer.sendInputLine("${activePlayer.health} ${activePlayer.resources}")
      activePlayer.enemyPlayer.units.forEach { activePlayer.printLocation(it.location) }
      activePlayer.sendInputLine("${activePlayer.enemyPlayer.health} ${activePlayer.enemyPlayer.resources}")
      activePlayer.sendInputLine(obstacles.size.toString())
      obstacles.forEach { activePlayer.printObstacle(it) }
      activePlayer.execute()
    }

    for (player in gameManager.activePlayers) {
      try {
        val outputs = player.outputs
        for ((unit, line) in player.units.zip(outputs)) {
          val toks = line.split(" ")
          when (toks[0]) {
            "MOVE" -> {
              val (x, y) = toks.drop(1).map { Integer.valueOf(it) }
              unit.location = unit.location.towards(player.fixLocation(Vector2(x, y)), 40.0)
            }
            "SPAWN" -> {
              // TODO: Check if it's the general
              // TODO: Check if enough resources
              when (toks[1]) {
                "ZERGLINGS" -> {
                  repeat(4, { player.activeCreeps += Zergling(entityManager, player, unit.location) })
                  player.resources -= 40
                }
                "ARCHERS" -> {
                  repeat(2, { player.activeCreeps += Archer(entityManager, player, unit.location) })
                  player.resources -= 70
                }
              }
            }
          }
        }
      } catch (e: AbstractPlayer.TimeoutException) {
        e.printStackTrace()
        player.deactivate("${player.nickname}: timeout!")
      }
    }
  }
}
