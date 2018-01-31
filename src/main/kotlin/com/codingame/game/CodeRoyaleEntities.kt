package com.codingame.game

import com.codingame.game.Constants.GIANT_BUST_RATE
import com.codingame.game.Constants.KING_HP
import com.codingame.game.Constants.KING_MASS
import com.codingame.game.Constants.KING_RADIUS
import com.codingame.game.Constants.WORLD_HEIGHT
import com.codingame.game.Constants.WORLD_WIDTH
import com.codingame.game.Constants.OBSTACLE_RADIUS_RANGE
import com.codingame.game.Constants.TOWER_COVERAGE_PER_HP
import com.codingame.game.Constants.TOWER_CREEP_DAMAGE_RANGE
import com.codingame.game.Constants.TOWER_MELT_RATE
import com.codingame.gameengine.module.entities.Circle
import com.codingame.gameengine.module.entities.Entity
import com.codingame.gameengine.module.entities.GraphicEntityModule
import java.lang.Integer.max

lateinit var theEntityManager: GraphicEntityModule

val viewportX = 0..1920
val viewportY = 0..950

fun worldToScreen(worldLocation: Vector2): Vector2 {
  return Vector2(
    worldLocation.x + viewportX.first,
    worldLocation.y + viewportY.first
  )
}

var <T : Entity<*>?> Entity<T>.location: Vector2
  get() { return Vector2(x, y) }
  set(value) {
    val screenLoc = worldToScreen(value)
    x = screenLoc.x.toInt(); y = screenLoc.y.toInt()
  }

abstract class MyEntity {
  open var location = Vector2.Zero
  open var radius = 0

  abstract val mass: Int   // 0 := immovable
}

abstract class MyOwnedEntity(val owner: Player) : MyEntity()

class King(owner: Player) : MyOwnedEntity(owner) {
  override val mass = KING_MASS
  override var radius = KING_RADIUS

  private val kingOutline = theEntityManager.createCircle()
    .setRadius(KING_RADIUS)
    .setLineColor(owner.colorToken)
    .setLineWidth(2)!!

  private val kingSprite = theEntityManager.createSprite()
    .setImage("king.png")
    .setZIndex(40)
    .setAnchor(0.5)!!

  private val kingFillSprite = theEntityManager.createSprite()
    .setImage("king-fill.png")
    .setTint(owner.colorToken)
    .setAnchor(0.5)
    .setZIndex(30)!!

  fun setHealth(health: Int) {
    when {
      health <= 0 -> kingFillSprite.alpha = 0.0
      else -> kingFillSprite.alpha = 0.8 * health / KING_HP + 0.2
    }
  }

  override var location: Vector2
    get() = super.location
    set(value) {
      super.location = value
      kingSprite.location = location
      kingFillSprite.location = location
      kingOutline.location = location
    }
}

fun IntRange.sample(): Int {
  return theRandom.nextInt(last-first) + first
}

interface Structure {
  val owner: Player
  fun updateEntities()
  fun hideEntities()
}

class Mine(private val obstacle: Obstacle, override val owner: Player, val incomeRate: Int) : Structure {

  private val text = theEntityManager.createText("+$incomeRate")
    .setFillColor(owner.colorToken)!!
    .also { it.location = obstacle.location + Vector2(-7,0) }

  private val pickaxeSprite = theEntityManager.createSprite()
    .setImage("pickaxe.png")
    .setZIndex(40)
    .also { it.location = obstacle.location + Vector2(-20, 15) }
    .setAnchor(0.5)!!

  private val mineralBarOutline = theEntityManager.createRectangle()
    .also { it.location = obstacle.location + Vector2(-40, -30) }
    .setHeight(25)
    .setWidth(80)
    .setLineColor(0xFFFFFF)
    .setLineWidth(1)
    .setZIndex(400)!!

  private val mineralBarFill = theEntityManager.createRectangle()
    .also { it.location = obstacle.location + Vector2(-40, -30) }
    .setHeight(25)
    .setWidth(80)
    .setFillColor(0xffbf00)
    .setLineWidth(0)!!

  override fun hideEntities() {
    text.isVisible = false
    pickaxeSprite.isVisible = false
    mineralBarOutline.isVisible = false
    mineralBarFill.isVisible = false
  }

  override fun updateEntities() {
    text.isVisible = true
    pickaxeSprite.isVisible = true
    mineralBarOutline.isVisible = true
    mineralBarFill.isVisible = true
    mineralBarFill.width = 80 * obstacle.minerals / 300
  }
}

class Tower(obstacle: Obstacle, override val owner: Player, var attackRadius: Int, var health: Int) : Structure {
  private val towerRangeCircle = theEntityManager.createCircle()
    .setFillAlpha(0.15)
    .setFillColor(0)
    .setLineColor(0)
    .setZIndex(10)
    .also { it.location = obstacle.location }

  private val sprite = theEntityManager.createSprite()
    .setImage("tower.png")
    .setZIndex(40)
    .also { it.location = obstacle.location }
    .setAnchor(0.5)!!

  private val fillSprite = theEntityManager.createSprite()
    .setImage("tower-fill.png")
    .setZIndex(30)
    .also { it.location = obstacle.location }
    .setAnchor(0.5)!!

  override fun hideEntities() {
    towerRangeCircle.isVisible = false
    sprite.isVisible = false
    fillSprite.isVisible = false
  }

  override fun updateEntities()
  {
    towerRangeCircle.isVisible = true
    towerRangeCircle.fillColor = owner.colorToken
    towerRangeCircle.radius = attackRadius
    sprite.isVisible = true
    fillSprite.isVisible = true
    fillSprite.tint = owner.colorToken
  }

}

class Barracks(val obstacle: Obstacle, override val owner: Player, var creepType: CreepType) : Structure {

  private val progressOutline = theEntityManager.createRectangle()
    .also { it.location = obstacle.location + Vector2(-40,20) }
    .setHeight(25)
    .setWidth(80)
    .setLineColor(0xFFFFFF)
    .setLineWidth(1)
    .setZIndex(400)

  private val progressFill = theEntityManager.createRectangle()
    .also { it.location = obstacle.location + Vector2(-40,20) }
    .setHeight(25)
    .setWidth(80)
    .setFillColor(owner.colorToken)
    .setZIndex(401)

  var progressMax = creepType.buildTime
  var progress = 0
  var isTraining = false

  var onComplete: () -> Unit = { }

  private val creepSprite = theEntityManager.createSprite()
    .setAnchor(0.5)
    .setZIndex(40)
    .also { it.location = obstacle.location + Vector2(0, -20) }
    .setScale(2.0)!!

  private val creepFillSprite = theEntityManager.createSprite()
    .setTint(owner.colorToken)
    .setAnchor(0.5)
    .setZIndex(30)
    .also { it.location = obstacle.location + Vector2(0, -20) }
    .setTint(owner.colorToken)
    .setScale(2.0)!!

  override fun updateEntities() {
    creepSprite.isVisible = true
    creepSprite.image = creepType.assetName
    creepSprite.location = obstacle.location + Vector2(0, if (isTraining) -20 else 0)
    creepFillSprite.location = obstacle.location + Vector2(0, if (isTraining) -20 else 0)
    creepFillSprite.isVisible = true
    creepFillSprite.image = creepType.fillAssetName

    progressOutline.isVisible = isTraining
    progressFill.isVisible = isTraining
    progressFill.width = (80 * progress / (progressMax-1))
  }

  override fun hideEntities() {
    creepFillSprite.isVisible = false
    creepSprite.isVisible = false
    progressOutline.isVisible = false
    progressFill.isVisible = false
  }
}

var nextObstacleId = 1
class Obstacle(private val mineralRate: Int): MyEntity() {
  val obstacleId = nextObstacleId++
  override val mass = 0
  var minerals = 300; set(value) { field = max(value, 0) }

  private val outline: Circle = theEntityManager.createCircle()
    .setLineWidth(3)
    .setLineColor(0xbbbbbb)
    .setFillColor(0x222222)
    .setZIndex(-20)

  override var radius: Int = 0
    set(value) {
      field = value
      outline.radius = value
    }

  override var location: Vector2
    get() = super.location
    set(value) {
      super.location = value
      outline.location = location
    }

  init {
    radius = OBSTACLE_RADIUS_RANGE.sample()
  }

  private val area = Math.PI * radius * radius

  var structure: Structure? = null
    set(value) {
      structure?.hideEntities()
      field = value
      structure?.updateEntities()
    }

  fun updateEntities() {
    structure?.updateEntities()
  }

  fun act() {
    structure?.also {
      when (it) {
        is Tower -> {
          val damage = TOWER_CREEP_DAMAGE_RANGE.sample()
          val closestEnemy = it.owner.enemyPlayer.activeCreeps.minBy { it.location.distanceTo(location) }
          if (closestEnemy != null && closestEnemy.location.distanceTo(location) < it.attackRadius) {
            closestEnemy.damage(damage)
          } else if (it.owner.enemyPlayer.kingUnit.location.distanceTo(location) < it.attackRadius) {
            it.owner.enemyPlayer.health -= 1
          }

          it.health -= TOWER_MELT_RATE
          it.attackRadius = Math.sqrt((it.health * TOWER_COVERAGE_PER_HP + area) / Math.PI).toInt()

          if (it.health <= 0) {
            it.hideEntities()
            structure = null
          }
        }
        is Mine -> {
          it.owner.resources += it.incomeRate
          minerals -= it.incomeRate
          if (minerals <= 0) {
            it.hideEntities()
            structure = null
          }
        }
        is Barracks -> {
          if (it.isTraining) {
            it.progress++
            if (it.progress == it.progressMax) {
              it.progress = 0
              it.isTraining = false
              it.onComplete()
            }
          }
          it.updateEntities()
        }
      }
    }

    updateEntities()
  }

  init {
    location = Vector2.random(WORLD_WIDTH, WORLD_HEIGHT)
  }

  fun setMine(owner: Player) {
    structure = Mine(this, owner, mineralRate)
  }

  fun setTower(owner: Player, health: Int) {
    structure = Tower(this, owner, 0, health)
  }

  fun setBarracks(owner: Player, creepType: CreepType) {
    structure = Barracks(this, owner, creepType)
  }
}

class TowerBustingCreep(
  owner: Player,
  creepType: CreepType,
  private val obstacles: List<Obstacle>
) : Creep(owner, creepType
) {
  override fun move() {
    obstacles
      .filter { it.structure != null && it.structure is Tower && (it.structure as Tower).owner == owner.enemyPlayer }
      .minBy { it.location.distanceTo(location) }
      ?.let {
        location = location.towards(it.location, speed.toDouble())
      }
  }

  override fun dealDamage() {
    obstacles
      .firstOrNull {
        it.structure != null
          && it.structure is Tower
          && (it.structure as Tower).owner == owner.enemyPlayer
          && it.location.distanceTo(location) - radius - it.radius < 10
      }?.let {
      (it.structure as Tower).health -= GIANT_BUST_RATE
    }

    val enemyKing = owner.enemyPlayer.kingUnit
    if (location.distanceTo(enemyKing.location) < radius + enemyKing.radius + attackRange + 10) {
      owner.enemyPlayer.health -= 1
    }
  }
}

class KingChasingCreep(owner: Player, creepType: CreepType)
  : Creep(owner, creepType) {

  override fun move() {
    val enemyKing = owner.enemyPlayer.kingUnit
    // move toward enemy king, if not yet in range
    if (location.distanceTo(enemyKing.location) - radius - enemyKing.radius > attackRange)
      location = location.towards((enemyKing.location + (location - enemyKing.location).resizedTo(3.0)), speed.toDouble())
  }

  override fun dealDamage() {
    val enemyKing = owner.enemyPlayer.kingUnit
    if (location.distanceTo(enemyKing.location) < radius + enemyKing.radius + attackRange + 10) {
      owner.enemyPlayer.health -= 1
    }
  }
}

abstract class Creep(
  owner: Player,
  val creepType: CreepType
) : MyOwnedEntity(owner) {

  protected val speed: Int = creepType.speed
  val attackRange: Int = creepType.range
  override val mass: Int = creepType.mass

  var health: Int = creepType.hp
    set(value) {
      field = value
      fillSprite.alpha = health.toDouble() / maxHealth * 0.8 + 0.2

      if (field <= 0) {
        field = 0
        sprite.alpha = 0.0
        fillSprite.alpha = 0.0
      }
    }

  private val maxHealth = health

  private val sprite = theEntityManager.createSprite()
    .setImage(creepType.assetName)
    .setAnchor(0.5)
    .setZIndex(40)!!

  private val fillSprite = theEntityManager.createSprite()
    .setImage(creepType.fillAssetName)
    .setTint(owner.colorToken)
    .setAnchor(0.5)
    .setZIndex(30)!!

  override var location: Vector2 = Vector2.Zero
    set(value) {
      field = value
      if (value != Vector2.Zero) {
        sprite.location = value
        fillSprite.location = value
      }
    }

  override var radius = creepType.radius

  fun damage(hp: Int) {
    health -= hp
    if (health <= 0) {
      owner.activeCreeps.remove(this)
    }
  }

  abstract fun dealDamage()
  abstract fun move()
}

