package com.codingame.game

import com.codingame.game.Constants.GIANT_BUST_RATE
import com.codingame.game.Constants.INCOME_TIMER
import com.codingame.game.Constants.OBSTACLE_RADIUS_RANGE
import com.codingame.game.Constants.TOWER_COVERAGE_PER_HP
import com.codingame.game.Constants.TOWER_CREEP_DAMAGE_RANGE
import com.codingame.game.Constants.TOWER_GENERAL_REPEL_FORCE
import com.codingame.game.Constants.TOWER_MELT_RATE
import com.codingame.gameengine.module.entities.Circle
import com.codingame.gameengine.module.entities.GraphicEntityModule

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

fun IntRange.sample(): Int {
  return (Math.random() * (last-first+1) + first).toInt()
}

interface Structure {
  fun updateEntities()
  fun hideEntities()
}

class Mine(obstacle: Obstacle, val owner: Player, val incomeRate: Int) : Structure {
  private val text = theEntityManager.createText("+$incomeRate")
    .setFillColor(owner.colorToken)!!
    .setX(obstacle.location.x.toInt() - 10)
    .setY(obstacle.location.y.toInt())

  override fun hideEntities() {
    text.isVisible = false
  }

  override fun updateEntities() {
    text.isVisible = true
  }
}

class Tower(obstacle: Obstacle, val owner: Player, var attackRadius: Int, var health: Int) : Structure {
  private val towerRangeCircle = theEntityManager.createCircle()
    .setFillAlpha(0.15)
    .setFillColor(0)
    .setLineColor(0)
    .setZIndex(10)
    .setX(obstacle.location.x.toInt())
    .setY(obstacle.location.y.toInt())

  val sprite = theEntityManager.createSprite()
    .setImage("tower.png")
    .setZIndex(40)
    .setX(obstacle.location.x.toInt())
    .setY(obstacle.location.y.toInt())

  val fillSprite = theEntityManager.createSprite()
    .setImage("towerfill.png")
    .setZIndex(30)
    .setX(obstacle.location.x.toInt())
    .setY(obstacle.location.y.toInt())

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

class Obstacle(val mineralRate: Int): MyEntity() {
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

  val mineralBarOutline by lazy {
    theEntityManager.createRectangle()
      .setX(location.x.toInt() - 40)
      .setY(location.y.toInt() - 30)
      .setHeight(25)
      .setWidth(80)
      .setLineColor(0xFFFFFF)
      .setLineWidth(1)
      .setZIndex(400)
  }

  val mineralBarFill by lazy {
    theEntityManager.createRectangle()
      .setX(location.x.toInt() - 40)
      .setY(location.y.toInt() - 30)
      .setHeight(25)
      .setWidth(80)
      .setFillColor(0x8888FF)
      .setLineWidth(0)
  }

  fun updateEntities() {
    structure?.updateEntities()
    mineralBarOutline.isVisible = minerals > 0 && structure !is Tower
    mineralBarFill.isVisible = minerals > 0 && structure !is Tower
    mineralBarFill.width = 80 * minerals / 300
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

          val enemyGeneral = it.owner.enemyPlayer.generalUnit
          if (enemyGeneral.location.distanceTo(location) < it.attackRadius) {
            enemyGeneral.location += (enemyGeneral.location - location).resizedTo(TOWER_GENERAL_REPEL_FORCE)
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
      }
    }

    updateEntities()
  }

  init {
    this.location = Vector2.random(1920, 1080)
  }

  fun setMine(owner: Player) {
    structure = Mine(this, owner, mineralRate)
  }

  fun setTower(owner: Player, health: Int) {
    structure = Tower(this, owner, 0, health)
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
  private val maxHealth = health

  override val entity = theEntityManager.createCircle()
    .setRadius(radius)
    .setFillColor(owner.colorToken)
    .setVisible(false)!!

  val sprite = theEntityManager.createSprite()
    .setImage(creepType.assetName)
    .setZIndex(40)

  val fillSprite = theEntityManager.createSprite()
    .setImage(creepType.fillAssetName)
    .setTint(owner.colorToken)
    .setZIndex(30)

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

