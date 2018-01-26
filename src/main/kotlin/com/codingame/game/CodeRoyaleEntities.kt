package com.codingame.game

import com.codingame.game.Constants.GIANT_BUST_RATE
import com.codingame.game.Constants.KING_MASS
import com.codingame.game.Constants.KING_RADIUS
import com.codingame.game.Constants.OBSTACLE_RADIUS_RANGE
import com.codingame.game.Constants.TOWER_COVERAGE_PER_HP
import com.codingame.game.Constants.TOWER_CREEP_DAMAGE_RANGE
import com.codingame.game.Constants.TOWER_MELT_RATE
import com.codingame.gameengine.module.entities.Circle
import com.codingame.gameengine.module.entities.GraphicEntityModule
import java.lang.Integer.max

lateinit var theEntityManager: GraphicEntityModule

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

abstract class MyOwnedEntity(val owner: Player) : MyEntity()

class King(owner: Player) : MyOwnedEntity(owner) {
  override val mass: Int = KING_MASS
  override val entity = theEntityManager.createCircle()
    .setRadius(KING_RADIUS)
    .setFillColor(owner.colorToken)!!
}

fun IntRange.sample(): Int {
  return (Math.random() * (last-first+1) + first).toInt()
}

interface Structure {
  val owner: Player
  fun updateEntities()
  fun hideEntities()
}

class Mine(private val obstacle: Obstacle, override val owner: Player, val incomeRate: Int) : Structure {
  private val text = theEntityManager.createText("+$incomeRate")
    .setFillColor(owner.colorToken)!!
    .setX(obstacle.location.x.toInt() - 7)
    .setY(obstacle.location.y.toInt())

  private val pickaxeSprite = theEntityManager.createSprite()
    .setImage("pickaxe.png")
    .setZIndex(40)
    .setX(obstacle.location.x.toInt() - 20)
    .setY(obstacle.location.y.toInt() + 15)
    .setAnchor(0.5)!!

  private val mineralBarOutline by lazy {
    theEntityManager.createRectangle()
      .setX(obstacle.location.x.toInt() - 40)
      .setY(obstacle.location.y.toInt() - 30)
      .setHeight(25)
      .setWidth(80)
      .setLineColor(0xFFFFFF)
      .setLineWidth(1)
      .setZIndex(400)!!
  }

  private val mineralBarFill by lazy {
    theEntityManager.createRectangle()
      .setX(obstacle.location.x.toInt() - 40)
      .setY(obstacle.location.y.toInt() - 30)
      .setHeight(25)
      .setWidth(80)
      .setFillColor(0x8888FF)
      .setLineWidth(0)!!
  }

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
    .setX(obstacle.location.x.toInt())
    .setY(obstacle.location.y.toInt())

  private val sprite = theEntityManager.createSprite()
    .setImage("tower.png")
    .setZIndex(40)
    .setX(obstacle.location.x.toInt())
    .setY(obstacle.location.y.toInt())
    .setAnchor(0.5)!!

  private val fillSprite = theEntityManager.createSprite()
    .setImage("tower-fill.png")
    .setZIndex(30)
    .setX(obstacle.location.x.toInt())
    .setY(obstacle.location.y.toInt())
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

  private val progressBarOutline by lazy {
    theEntityManager.createRectangle()
      .setX(obstacle.location.x.toInt() - 40)
      .setY(obstacle.location.y.toInt() + 10)
      .setHeight(25)
      .setWidth(80)
      .setLineColor(0xFFFFFF)
      .setLineWidth(1)
      .setZIndex(400)
  }

  private val progressBarFill by lazy {
    theEntityManager.createRectangle()
      .setX(obstacle.location.x.toInt() - 40)
      .setY(obstacle.location.y.toInt() + 10)
      .setHeight(25)
      .setWidth(80)
      .setFillColor(0xFFA500)
      .setZIndex(401)
  }

  private var cooldownMax = creepType.cooldown
  var cooldown = 0; set(value) { field = max(0, value) }

  private val creepSprite = theEntityManager.createSprite()
    .setAnchor(0.5)
    .setZIndex(40)
    .setX(obstacle.location.x.toInt())
    .setY(obstacle.location.y.toInt() - 20)
    .setScale(2.0)!!

  private val creepFillSprite = theEntityManager.createSprite()
    .setTint(owner.colorToken)
    .setAnchor(0.5)
    .setZIndex(30)
    .setX(obstacle.location.x.toInt())
    .setY(obstacle.location.y.toInt() - 20)
    .setTint(owner.colorToken)
    .setScale(2.0)!!

  override fun updateEntities() {
    creepSprite.isVisible = true
    creepSprite.image = creepType.assetName
    creepSprite.y = obstacle.location.y.toInt() - if(cooldown > 0) 20 else 0
    creepFillSprite.y = obstacle.location.y.toInt() - if(cooldown > 0) 20 else 0
    creepFillSprite.isVisible = true
    creepFillSprite.image = creepType.fillAssetName

    progressBarOutline.isVisible = cooldown > 0
    progressBarFill.isVisible = cooldown > 0
    progressBarFill.width = (80 * cooldown / cooldownMax)
  }

  override fun hideEntities() {
    creepFillSprite.isVisible = false
    creepSprite.isVisible = false
    progressBarOutline.isVisible = false
    progressBarFill.isVisible = false
  }
}

var nextObstacleId = 1
class Obstacle(private val mineralRate: Int): MyEntity() {
  val obstacleId = nextObstacleId++
  override val mass = 0
  var minerals = 300

  var radius = OBSTACLE_RADIUS_RANGE.sample()
  private val area = Math.PI * radius * radius

  var structure: Structure? = null
    set(value) {
      structure?.hideEntities()
      field = value
      structure?.updateEntities()
    }

  override val entity: Circle = theEntityManager.createCircle()
    .setRadius(radius)
    .setLineWidth(3)
    .setFillColor(0)
    .setFillAlpha(0.0)
    .setZIndex(100)

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
          it.cooldown--
          it.updateEntities()
        }
      }
    }

    updateEntities()
  }

  init {
    location = Vector2.random(1920, 1080)
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
  location: Vector2,
  creepType: CreepType,
  private val obstacles: List<Obstacle>
  ) : Creep(owner, location, creepType
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
          && it.location.distanceTo(location) - entity.radius - it.radius < 10
      }?.let {
        (it.structure as Tower).health -= GIANT_BUST_RATE
      }

    val enemyKing = owner.enemyPlayer.kingUnit
    if (location.distanceTo(enemyKing.location) < entity.radius + enemyKing.entity.radius + attackRange + 10) {
      owner.enemyPlayer.health -= 1
    }
  }
}

class KingChasingCreep(
  owner: Player,
  location: Vector2,
  creepType: CreepType) : Creep(owner, location, creepType
) {
  override fun move() {
    val enemyKing = owner.enemyPlayer.kingUnit
    // move toward enemy king, if not yet in range
    if (location.distanceTo(enemyKing.location) - entity.radius - enemyKing.entity.radius > attackRange)
      location = location.towards((enemyKing.location + (location - enemyKing.location).resizedTo(3.0)), speed.toDouble())
  }

  override fun dealDamage() {
    val enemyKing = owner.enemyPlayer.kingUnit
    if (location.distanceTo(enemyKing.location) < entity.radius + enemyKing.entity.radius + attackRange + 10) {
      owner.enemyPlayer.health -= 1
    }
  }
}

abstract class Creep(
  owner: Player,
  location: Vector2,
  val creepType: CreepType
) : MyOwnedEntity(owner) {

  protected val speed: Int = creepType.speed
  val attackRange: Int = creepType.range
  private val radius: Int = creepType.radius
  override val mass: Int = creepType.mass

  var health: Int = creepType.hp
  set(value) {
    field = value
    if (field < 0) field = 0
  }

  private val maxHealth = health

  override val entity = theEntityManager.createCircle()
    .setRadius(radius)
    .setFillColor(owner.colorToken)
    .setVisible(false)!!

  private val sprite = theEntityManager.createSprite()
    .setImage(creepType.assetName)
    .setAnchor(0.5)
    .setZIndex(40)!!

  private val fillSprite = theEntityManager.createSprite()
    .setImage(creepType.fillAssetName)
    .setTint(owner.colorToken)
    .setAnchor(0.5)
    .setZIndex(30)!!

  init {
    this.location = location
    @Suppress("LeakingThis") updateEntity()
  }

  override fun updateEntity() {
    super.updateEntity()
    fillSprite.alpha = health.toDouble() / maxHealth * 0.8 + 0.2
    fillSprite.x = location.x.toInt()
    fillSprite.y = location.y.toInt()
    sprite.x = location.x.toInt()
    sprite.y = location.y.toInt()
  }

  fun damage(hp: Int) {
    health -= hp
    if (health <= 0) {
      entity.isVisible = false
      sprite.alpha = 0.0
      fillSprite.alpha = 0.0
      owner.activeCreeps.remove(this)
    }
  }

  abstract fun dealDamage()
  abstract fun move()
}

