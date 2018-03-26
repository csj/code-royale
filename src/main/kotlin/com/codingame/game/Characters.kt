package com.codingame.game

import com.codingame.game.Constants.CREEP_DAMAGE
import com.codingame.game.Constants.GIANT_BUST_RATE
import com.codingame.game.Constants.QUEEN_HP
import com.codingame.game.Constants.QUEEN_MASS
import com.codingame.game.Constants.QUEEN_RADIUS
import com.codingame.game.Constants.QUEEN_SPEED
import com.codingame.game.Constants.TOUCHING_DELTA
import com.codingame.gameengine.core.GameManager
import com.codingame.gameengine.module.entities.Curve
import com.codingame.gameengine.module.entities.Entity
import com.codingame.gameengine.module.entities.GraphicEntityModule
import tooltipModule.TooltipModule

lateinit var theEntityManager: GraphicEntityModule
lateinit var theTooltipModule: TooltipModule
lateinit var theGameManager: GameManager<Player>

val viewportX = 0..1920
val viewportY = 180..1080

var <T : Entity<*>?> Entity<T>.location: Vector2
  get() = Vector2(x - viewportX.first, y - viewportY.first)

  set(value) {
    x = (value.x + viewportX.first).toInt()
    y = (value.y + viewportY.first).toInt()
  }

abstract class MyEntity {
  abstract var location: Vector2
  abstract var radius: Int
  abstract val mass: Int   // 0 := immovable
}

abstract class MyOwnedEntity(val owner: Player) : MyEntity() {
  abstract fun damage(damageAmount: Int)

  protected val tokenCircle = theEntityManager.createCircle()
    .setLineColor(0xffffff)
    .setFillColor(owner.colorToken)
    .setFillAlpha(1.0)
    .setZIndex(40)!!   // TODO: set to some kind of increasing ID

  protected val characterSprite = theEntityManager.createSprite()
    .setZIndex(41)
    .setAnchor(0.5)!!

  override var location: Vector2 = Vector2.Zero
    set(value) {
      if (value == Vector2.Zero) return
      field = value

      tokenCircle.location = value
      characterSprite.location = value
    }

  abstract val maxHealth:Int
  open var health:Int = 0
    set(value) {
      field = value
      if (value < 0) field = 0
      tokenCircle.fillAlpha = when {
        health <= 0 -> 0.0
        else -> 0.8 * health / maxHealth + 0.2
      }
      theTooltipModule.updateExtraTooltipText(tokenCircle, "Health: $health")
    }

  fun commitState(time: Double) {
    theEntityManager.commitEntityState(time, tokenCircle, characterSprite)
  }
}

class Queen(owner: Player) : MyOwnedEntity(owner) {
  override val mass = QUEEN_MASS
  override var radius = QUEEN_RADIUS
  override val maxHealth = QUEEN_HP

  init {
    characterSprite.image = "queen.png"
    theTooltipModule.registerEntity(tokenCircle, mapOf("id" to tokenCircle.id, "type" to "Queen"))
    tokenCircle.radius = radius
    tokenCircle.lineWidth = 2
    characterSprite.baseWidth = radius*2
    characterSprite.baseHeight = radius*2
  }

  fun moveTowards(target: Vector2) {
    location = location.towards(target, QUEEN_SPEED.toDouble())
  }

  override fun damage(damageAmount: Int) {
    if (damageAmount <= 0) return
    owner.health -= damageAmount
  }

  fun commitState(time: Double) {
    theEntityManager.commitEntityState(time, queenSprite, queenFillSprite, queenOutline)
  }
}

abstract class Creep(
  owner: Player,
  val creepType: CreepType
) : MyOwnedEntity(owner) {

  protected val speed: Int = creepType.speed
  val attackRange: Int = creepType.range
  override val mass: Int = creepType.mass
  override val maxHealth = creepType.hp
  final override var radius = creepType.radius

  open fun finalizeFrame() { }

  override fun damage(damageAmount: Int) {
    if (damageAmount <= 0) return   // no accidental healing!

    health -= damageAmount
    theTooltipModule.updateExtraTooltipText(sprite, "Health: $health")
  }

  abstract fun dealDamage()
  abstract fun move(frames: Double)

  final override var health: Int
    get() { return super.health }
    set(value) {
      super.health = value
      if (super.health == 0) {
        characterSprite.alpha = 0.0
        tokenCircle.alpha = 0.0
      }
    }

  init {
    health = creepType.hp

    tokenCircle.radius = radius
    tokenCircle.lineWidth = 1
    characterSprite.image = creepType.assetName
    characterSprite.baseWidth = radius*2
    characterSprite.baseHeight = radius*2

    theTooltipModule.registerEntity(tokenCircle, mapOf("id" to tokenCircle.id, "type" to creepType.toString()))
  }
}

class TowerBustingCreep(
  owner: Player,
  creepType: CreepType,
  private val obstacles: List<Obstacle>
) : Creep(owner, creepType) {
  override fun move(frames: Double)  {
    obstacles
      .filter { it.structure != null && it.structure is Tower && (it.structure as Tower).owner == owner.enemyPlayer }
      .minBy { it.location.distanceTo(location) }
      ?.let {
        location = location.towards(it.location, speed.toDouble() * frames)
      }
  }

  override fun dealDamage() {
    obstacles
      .firstOrNull {
        val struc = it.structure
        struc is Tower
          && struc.owner == owner.enemyPlayer
          && it.location.distanceTo(location) - radius - it.radius < 5
      }?.let { (it.structure as Tower).health -= GIANT_BUST_RATE }
  }
}

class QueenChasingCreep(owner: Player, creepType: CreepType)
  : Creep(owner, creepType) {

  private var lastLocation: Vector2? = null
  override fun finalizeFrame() {
    val last = lastLocation

    if (last != null) {
      val movementVector = when {
        last.distanceTo(location) > 30 -> location - last
        else -> owner.enemyPlayer.queenUnit.location - location
      }
      characterSprite.rotation = Math.atan2(movementVector.y, movementVector.x)
    }

    lastLocation = location
  }

  override fun move(frames: Double)  {
    val enemyQueen = owner.enemyPlayer.queenUnit
    // move toward enemy queen, if not yet in range
    if (location.distanceTo(enemyQueen.location) - radius - enemyQueen.radius > attackRange)
      location = location.towards((enemyQueen.location + (location - enemyQueen.location).resizedTo(3.0)), speed.toDouble() * frames)
  }

  override fun dealDamage() {
    val enemyQueen = owner.enemyPlayer.queenUnit
    if (location.distanceTo(enemyQueen.location) < radius + enemyQueen.radius + attackRange + 5) {
      owner.enemyPlayer.health -= 1
    }
  }
}

//targets the closest enemy creep
class AutoAttackCreep(owner: Player, creepType: CreepType)
  : Creep(owner, creepType){

  private var lastLocation: Vector2? = null

  var attackTarget: Creep? = null

  private val projectile = theEntityManager.createCircle()!!
      .setZIndex(30)
      .setRadius(4)
      .setFillColor(owner.colorToken)
      .setLineColor(0xffffff)
      .setLineWidth(2)
      .setVisible(false)

  override fun finalizeFrame() {
    val target = findTarget() ?: owner.enemyPlayer.queenUnit

    val last = lastLocation

    if (last != null) {
      val movementVector = when {
        last.distanceTo(location) > 30 -> location - last
        else -> target.location - location
      }
      characterSprite.rotation = Math.atan2(movementVector.y, movementVector.x)
    }

    lastLocation = location

    val localAttackTarget = attackTarget
    if (localAttackTarget != null) {
      projectile.isVisible = true
      projectile.setX(location.x.toInt() + viewportX.first, Curve.NONE)
      projectile.setY(location.y.toInt() + viewportY.first, Curve.NONE)
      theEntityManager.commitEntityState(0.0, projectile)
      projectile.setX(localAttackTarget.location.x.toInt() + viewportX.first, Curve.EASE_IN_AND_OUT)
      projectile.setY(localAttackTarget.location.y.toInt() + viewportY.first, Curve.EASE_IN_AND_OUT)
      theEntityManager.commitEntityState(0.99, projectile)
      projectile.isVisible = false
      theEntityManager.commitEntityState(1.0, projectile)
    }
  }

  override fun move(frames: Double) {
    val target = findTarget() ?: owner.queenUnit
    // move toward target, if not yet in range
    if (location.distanceTo(target.location) - radius - target.radius > attackRange)
      location = location.towards((target.location + (location - target.location).resizedTo(3.0)), speed.toDouble() * frames)
  }

  override fun dealDamage() {
    attackTarget = null
    val target = findTarget() ?: return
    if (location.distanceTo(target.location) < radius + target.radius + attackRange + TOUCHING_DELTA) {
      target.damage(CREEP_DAMAGE)
      attackTarget = target
    }
  }

  private fun findTarget(): Creep? {
    return owner.enemyPlayer.activeCreeps
      .minBy { it.location.distanceTo(location) }
  }
}

