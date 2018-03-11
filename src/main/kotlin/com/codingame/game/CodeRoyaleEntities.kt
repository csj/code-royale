package com.codingame.game

import com.codingame.game.Constants.CREEP_DAMAGE
import com.codingame.game.Constants.GIANT_BUST_RATE
import com.codingame.game.Constants.QUEEN_HP
import com.codingame.game.Constants.QUEEN_MASS
import com.codingame.game.Constants.QUEEN_RADIUS
import com.codingame.game.Constants.OBSTACLE_MINERAL_RANGE
import com.codingame.game.Constants.WORLD_HEIGHT
import com.codingame.game.Constants.WORLD_WIDTH
import com.codingame.game.Constants.OBSTACLE_RADIUS_RANGE
import com.codingame.game.Constants.TOWER_COVERAGE_PER_HP
import com.codingame.game.Constants.TOWER_MELT_RATE
import com.codingame.gameengine.module.entities.Circle
import com.codingame.gameengine.module.entities.Curve
import com.codingame.gameengine.module.entities.Entity
import com.codingame.gameengine.module.entities.GraphicEntityModule
import tooltipModule.TooltipModule

lateinit var theEntityManager: GraphicEntityModule
lateinit var theTooltipModule: TooltipModule

val viewportX = 0..1920
val viewportY = 0..950

var <T : Entity<*>?> Entity<T>.location: Vector2
  get() = Vector2(x - viewportX.first, y - viewportY.first)

  set(value) {
    x = (value.x + viewportX.first).toInt()
    y = (value.y + viewportY.first).toInt()
  }

abstract class MyEntity {
  open var location = Vector2.Zero
  open var radius = 0

  abstract val mass: Int   // 0 := immovable
}

abstract class MyOwnedEntity(val owner: Player) : MyEntity() {
  abstract fun damage(damageAmount: Int)
}

class Queen(owner: Player) : MyOwnedEntity(owner) {
  override val mass = QUEEN_MASS
  override var radius = QUEEN_RADIUS

  private val queenOutline = theEntityManager.createCircle()
    .setRadius(QUEEN_RADIUS)
    .setLineColor(owner.colorToken)
    .setLineWidth(2)!!

  private val queenSprite = theEntityManager.createSprite()
    .setImage("queen.png")
    .setZIndex(40)
    .setAnchor(0.5)!!

  private val queenFillSprite = theEntityManager.createSprite()
    .setImage("queen-fill.png")
    .setTint(owner.colorToken)
    .setAnchor(0.5)
    .setZIndex(30)!!

  fun setHealth(health: Int) {
    when {
      health <= 0 -> queenFillSprite.alpha = 0.0
      else -> queenFillSprite.alpha = 0.8 * health / QUEEN_HP + 0.2
    }
    theTooltipModule.updateExtraTooltipText(queenSprite, "Health: $health")
  }

  init {
    theTooltipModule.registerEntity(queenSprite, mapOf("id" to queenSprite.id, "type" to "Queen"))
  }

  fun moveTowards(target: Vector2) {
    location = location.towards(target, Constants.UNIT_SPEED.toDouble())
  }

  override var location: Vector2
    get() = super.location
    set(value) {
      super.location = value
      queenSprite.location = location
      queenFillSprite.location = location
      queenOutline.location = location
    }

  override fun damage(damageAmount: Int) {
    owner.health -= damageAmount
  }
}

interface Structure {
  val owner: Player
  fun updateEntities()
  fun hideEntities()
  fun extraTooltipLines(): List<String>
}

class Mine(private val obstacle: Obstacle, override val owner: Player, incomeRate: Int) : Structure {

  override fun extraTooltipLines(): List<String> = listOf(
    "MINE (+$incomeRate)",
    "Remaining resources: ${obstacle.minerals}"
  )

  private val text = theEntityManager.createText("+$incomeRate")
    .setFillColor(owner.colorToken)!!
    .setZIndex(401)
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
    .setZIndex(401)

  var incomeRate = incomeRate
    set(value) {
      field = value
      text.text = "+$incomeRate"
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
    mineralBarFill.width = 80 * obstacle.minerals / OBSTACLE_MINERAL_RANGE.last
  }
}

class Tower(private val obstacle: Obstacle, override val owner: Player, var attackRadius: Int, var health: Int) : Structure {
  override fun extraTooltipLines(): List<String> = listOf(
    "TOWER",
    "Range: $attackRadius",
    "Health: $health"
  )

  private val towerRangeCircle = theEntityManager.createCircle()
    .setFillAlpha(0.2)
    .setLineWidth(0)
    .setZIndex(10)
    .also { it.location = obstacle.location }

  private val sprite = theEntityManager.createSprite()
    .setImage("tower.png")
    .setZIndex(40)
    .also { it.location = obstacle.location }
    .setAnchor(0.5)!!

  private val fillSprite = theEntityManager.createSprite()!!
    .setImage("tower-fill.png")
    .setZIndex(30)
    .also { it.location = obstacle.location }
    .setAnchor(0.5)

  private val projectile = theEntityManager.createCircle()!!
      .setZIndex(30)
      .setRadius(8)
      .setFillColor(owner.colorToken)
      .setLineColor(0xffffff)
      .setLineWidth(3)
      .setVisible(false)

  public var attackTarget: MyEntity? = null

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

    val localAttackTarget = attackTarget
    if (localAttackTarget != null) {
      projectile.isVisible = true
      projectile.setX(obstacle.location.x.toInt(), Curve.NONE)
      projectile.setY(obstacle.location.y.toInt(), Curve.NONE)
      theEntityManager.commitEntityState(0.0, projectile)
      projectile.setX(localAttackTarget.location.x.toInt(), Curve.EASE_IN_AND_OUT)
      projectile.setY(localAttackTarget.location.y.toInt(), Curve.EASE_IN_AND_OUT)
      theEntityManager.commitEntityState(1.0, projectile)
      projectile.isVisible = false
    }
  }

    fun distanceScaledDamage(minDamage: Int, maxDamage: Int, target: MyEntity) : Int {
        val damageRange = maxDamage - minDamage + 1
        val distance = target.location.distanceTo(obstacle.location)
        return maxDamage - (Constants.TOWER_TIER_DAMAGE_INCREMENT * Math.floor(distance / (attackRadius / damageRange)).toInt())
    }
}

class Barracks(val obstacle: Obstacle, override val owner: Player, var creepType: CreepType) : Structure {

  override fun extraTooltipLines(): List<String> {
    val retVal = mutableListOf(
      "BARRACKS ($creepType)"
    )
    if (this.isTraining) retVal += "Progress: $progress/$progressMax"
    return retVal
  }

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

var nextObstacleId = 0
class Obstacle(val maxMineralRate: Int, initialAmount: Int): MyEntity() {
  val obstacleId = nextObstacleId++
  override val mass = 0
  var minerals by nonNegative(initialAmount)

  private val outline: Circle = theEntityManager.createCircle()
    .setLineWidth(3)
    .setLineColor(0xbbbbbb)
    .setFillColor(0x222222)
    .setZIndex(20)

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
    val params = hashMapOf("id" to obstacleId, "type" to "Obstacle")
    theTooltipModule.registerEntity(outline, params as Map<String, Any>?)
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
    val struc = structure
    if (struc != null) {
      theTooltipModule.updateExtraTooltipText(outline, *struc.extraTooltipLines().toTypedArray())
    } else {
      theTooltipModule.updateExtraTooltipText(outline, "Remaining resources: $minerals")
    }
  }

  fun destroy() {
    outline.isVisible = false
  }

  fun act() {
    structure?.also {
      when (it) {
        is Tower -> {
          val closestEnemy = it.owner.enemyPlayer.activeCreeps.minBy { it.location.distanceTo(location) }
          if (closestEnemy != null && closestEnemy.location.distanceTo(location) < it.attackRadius) {
            closestEnemy.damage(it.distanceScaledDamage(Constants.TOWER_CREEP_DAMAGE_MIN, Constants.TOWER_CREEP_DAMAGE_MAX, closestEnemy))
            it.attackTarget = closestEnemy
          } else if (it.owner.enemyPlayer.queenUnit.location.distanceTo(location) < it.attackRadius) {
            it.owner.enemyPlayer.health -= it.distanceScaledDamage(Constants.TOWER_CREEP_DAMAGE_MIN, Constants.TOWER_QUEEN_DAMAGE_MAX, it.owner.enemyPlayer.queenUnit)
            it.attackTarget = it.owner.enemyPlayer.queenUnit
          } else {
            it.attackTarget = null
          }

          it.health -= TOWER_MELT_RATE
          it.attackRadius = Math.sqrt((it.health * TOWER_COVERAGE_PER_HP + area) / Math.PI).toInt()

          if (it.health <= 0) {
            it.hideEntities()
            structure = null
          }
        }
        is Mine -> {
          it.owner.resourcesPerTurn += it.incomeRate
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
    structure = Mine(this, owner, 1)
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
) : Creep(owner, creepType) {
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
        val struc = it.structure
        struc is Tower
          && struc.owner == owner.enemyPlayer
          && it.location.distanceTo(location) - radius - it.radius < 10
      }?.let { (it.structure as Tower).health -= GIANT_BUST_RATE }

    val enemyQueen = owner.enemyPlayer.queenUnit
    if (location.distanceTo(enemyQueen.location) < radius + enemyQueen.radius + attackRange + 10) {
      owner.enemyPlayer.health -= 1
    }
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
      sprite.rotation = Math.atan2(movementVector.y, movementVector.x)
      fillSprite.rotation = Math.atan2(movementVector.y, movementVector.x)
    }

    lastLocation = location
  }

  override fun move() {
    val enemyQueen = owner.enemyPlayer.queenUnit
    // move toward enemy queen, if not yet in range
    if (location.distanceTo(enemyQueen.location) - radius - enemyQueen.radius > attackRange)
      location = location.towards((enemyQueen.location + (location - enemyQueen.location).resizedTo(3.0)), speed.toDouble())
  }

  override fun dealDamage() {
    val enemyQueen = owner.enemyPlayer.queenUnit
    if (location.distanceTo(enemyQueen.location) < radius + enemyQueen.radius + attackRange + 10) {
      owner.enemyPlayer.health -= 1
    }
  }
}

//targets the closest enemy creep
class AutoAttackCreep(owner: Player, creepType: CreepType)
  : Creep(owner, creepType){

  private var lastLocation: Vector2? = null

  public var attackTarget: Creep? = null

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
      sprite.rotation = Math.atan2(movementVector.y, movementVector.x)
      fillSprite.rotation = Math.atan2(movementVector.y, movementVector.x)
    }

    lastLocation = location

    val localAttackTarget = attackTarget
    if (localAttackTarget != null) {
      projectile.isVisible = true
      projectile.setX(location.x.toInt(), Curve.NONE)
      projectile.setY(location.y.toInt(), Curve.NONE)
      theEntityManager.commitEntityState(0.0, projectile)
      projectile.setX(localAttackTarget.location.x.toInt(), Curve.EASE_IN_AND_OUT)
      projectile.setY(localAttackTarget.location.y.toInt(), Curve.EASE_IN_AND_OUT)
      theEntityManager.commitEntityState(0.9, projectile)
      projectile.isVisible = false
      theEntityManager.commitEntityState(1.0, projectile)
    }
  }

  override fun move() {
    val target = findTarget() ?: return
    // move toward target, if not yet in range
    if (location.distanceTo(target.location) - radius - target.radius > attackRange)
      location = location.towards((target.location + (location - target.location).resizedTo(3.0)), speed.toDouble())
  }

  override fun dealDamage() {
    attackTarget = null
    val target = findTarget() ?: return
    if (location.distanceTo(target.location) < radius + target.radius + attackRange + 10) {
      target.damage(CREEP_DAMAGE)
      attackTarget = target
    }
  }

  private fun findTarget(): Creep? {
    return owner.enemyPlayer.activeCreeps
      .minBy { it.location.distanceTo(location) }
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

  protected val sprite = theEntityManager.createSprite()
    .setImage(creepType.assetName)
    .setAnchor(0.5)
    .setZIndex(40)!!

  protected val fillSprite = theEntityManager.createSprite()
    .setImage(creepType.fillAssetName)
    .setTint(owner.colorToken)
    .setAnchor(0.5)
    .setZIndex(30)!!

  override var location: Vector2 = Vector2.Zero
    set(value) {
      field = value
      if (value == Vector2.Zero) return

      sprite.location = value
      fillSprite.location = value
    }

  open fun finalizeFrame() { }

  override var radius = creepType.radius

  override fun damage(hp: Int) {
    health -= hp
    if (health <= 0) {
      owner.activeCreeps.remove(this)
    }
    theTooltipModule.updateExtraTooltipText(sprite, "Health: $health")
  }

  abstract fun dealDamage()
  abstract fun move()

  init {
    theTooltipModule.registerEntity(sprite, mapOf("id" to sprite.id, "type" to creepType.toString()))
  }
}

class PlayerHUD(private val player: Player, isSecondPlayer: Boolean) {
  private val left = if (isSecondPlayer) 1920/2 else 0
  private val right = if (isSecondPlayer) 1920 else 1920/2
  private val top = viewportY.last + 20
  private val bottom = 1080

  private val healthBarWidth = 400
  private val healthBarPadding = 15

  private val background = theEntityManager.createRectangle()!!
    .setX(left).setY(top)
    .setWidth(right-left).setHeight(bottom-top)
    .setFillColor(player.colorToken)
    .setLineWidth(0)
    .setZIndex(4000)

  private val avatar = theEntityManager.createSprite()
    .setImage(player.avatarToken)
    .setX(left + 10).setY(top + 10)
    .setBaseWidth(bottom - top - 10 - 10)
    .setBaseHeight(bottom - top - 10 - 10)
    .setZIndex(4003)!!

  private val heartSprite = theEntityManager.createSprite()
    .setX(left + 155).setY((top + bottom)/2)
    .setScale(2.0)
    .setImage("heart.png")
    .setAnchor(0.5)
    .setZIndex(4002)!!

  private val healthBarBackground = theEntityManager.createRectangle()!!
    .setX(left + 200 - healthBarPadding).setY(top + 25 - healthBarPadding)
    .setWidth(healthBarWidth + 2*healthBarPadding).setHeight(bottom-top-25-25+2*healthBarPadding)
    .setLineWidth(0)
    .setFillColor(0).setFillAlpha(0.4)
    .setZIndex(4001)

  private val healthBarFill = theEntityManager.createRectangle()!!
    .setX(left + 200).setY(top + 25)
    .setWidth(healthBarWidth).setHeight(bottom-top-25-25)
    .setFillColor(0x55ff55)
    .setLineWidth(0)
    .setZIndex(4002)

  private val playerName = theEntityManager.createText(player.nicknameToken)!!
    .setX(left + 200 + 5).setY(top + 25)
    .setFillColor(0)
    .setScale(2.0)
    .setZIndex(4003)

  private val moneySprite = theEntityManager.createSprite()
    .setX(healthBarBackground.x + healthBarBackground.width + 50).setY((top + bottom)/2)
    .setImage("money.png")
    .setScale(2.0)
    .setAnchor(0.5)
    .setZIndex(4002)!!

  private val moneyText = theEntityManager.createText("0")
    .setX(moneySprite.x + 50).setY(top + 20)
    .setScale(2.0)
    .setZIndex(4002)!!

  fun update() {
    healthBarFill.width = healthBarWidth * player.health / QUEEN_HP
    moneyText.text = when (player.resourcesPerTurn) {
      0 -> "${player.resources}"
      else -> "${player.resources} (+${player.resourcesPerTurn})"
    }
  }
}