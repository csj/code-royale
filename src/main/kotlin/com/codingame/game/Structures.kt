package com.codingame.game

import com.codingame.game.Constants.OBSTACLE_MINERAL_INCREASE
import com.codingame.game.Constants.OBSTACLE_MINERAL_RANGE
import com.codingame.game.Constants.TOWER_COVERAGE_PER_HP
import com.codingame.game.Constants.TOWER_CREEP_DAMAGE_DROP_DISTANCE
import com.codingame.game.Constants.TOWER_CREEP_DAMAGE_MAX
import com.codingame.game.Constants.TOWER_HP_PER_QUEEN_DAMAGE
import com.codingame.game.Constants.TOWER_MELT_RATE
import com.codingame.gameengine.module.entities.Curve
import kotlin.math.min

var nextObstacleId = 0
class Obstacle(var maxMineralRate: Int, initialAmount: Int, initialRadius: Int, initialLocation: Vector2): MyEntity() {
  val obstacleId = nextObstacleId++
  override val mass = 0
  var minerals by nonNegative(initialAmount)

  private val outline = theEntityManager.createSprite()
    .setImage("LC_1.png")
    .setZIndex(20)
    .setAnchor(0.5)

  override var radius: Int = 0
    set(value) {
      field = value
      outline.setScale(value * 2 / 220.0)
    }

  override var location: Vector2 = initialLocation
    set(value) {
      field = value
      outline.location = location
    }

  init {
    radius = initialRadius
    location = initialLocation
    val params = hashMapOf("id" to obstacleId, "type" to "Obstacle")
    theTooltipModule.registerEntity(outline, params as Map<String, Any>?)
  }

  val area = Math.PI * radius * radius

  var structure: Structure? = null
    set(value) {
      structure?.hideEntities()
      field = value
      value?.updateEntities()
      outline.alpha = if (value == null) 1.0 else 0.0
      theEntityManager.commitEntityState(0.0, outline)
    }

  fun updateEntities() {
    structure?.updateEntities()
    val struc = structure
    val lines = listOf(
      "Radius: $radius",
      "Remaining resources: $minerals"
    ) + (struc?.extraTooltipLines() ?: listOf())
    theTooltipModule.updateExtraTooltipText(outline, *lines.toTypedArray())
  }

  fun destroy() {
    outline.isVisible = false
  }

  fun act() {
    structure?.also { if (it.act()) structure = null }
    updateEntities()
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

interface Structure {
  val owner: Player
  val obstacle: Obstacle
  fun updateEntities()
  fun hideEntities()
  fun extraTooltipLines(): List<String>
  fun act(): Boolean  // return true if the Structure should be destroyed
}

class Mine(override val obstacle: Obstacle, override val owner: Player, incomeRate: Int) : Structure {

  override fun extraTooltipLines(): List<String> = listOf(
    "MINE (+$incomeRate)"
  )

  private val mineImage = theEntityManager.createSprite()
    .setImage("Mine.png")
    .setZIndex(40)
    .setAnchor(0.5)
    .also { it.location = obstacle.location }
    .setScale(obstacle.radius * 2 / 220.0)

  private val pickaxeSprite = theEntityManager.createSprite()
    .setImage(if (owner.isSecondPlayer) "Mine_Bleu.png" else "Mine_Rouge.png")
    .setZIndex(41)
    .also { it.location = obstacle.location + Vector2(0, -40) }
    .setAnchor(0.5)!!

  private val text = theEntityManager.createText("+$incomeRate")
    .setFillColor(0xffffff)!!
    .setZIndex(42)
    .setFontFamily("Arial Black")
    .setAnchorY(0.5)
    .also { it.location = obstacle.location + Vector2(0,-40) }

  private val mineralBarOutline = theEntityManager.createRectangle()
    .also { it.location = obstacle.location + Vector2(-40, -90) }
    .setHeight(15)
    .setWidth(80)
    .setLineColor(0)
    .setLineWidth(1)
    .setFillAlpha(0.0)
    .setZIndex(401)!!

  private val mineralBarFill = theEntityManager.createRectangle()
    .also { it.location = obstacle.location + Vector2(-40, -90) }
    .setHeight(15)
    .setWidth(80)
    .setFillColor(0xffbf00)
    .setLineAlpha(0.0)!!
    .setZIndex(400)

  var incomeRate = incomeRate
    set(value) {
      field = value
      text.text = "+$incomeRate"
    }

  override fun hideEntities() {
    text.isVisible = false
    pickaxeSprite.isVisible = false
    mineImage.isVisible = false
    mineralBarOutline.isVisible = false
    mineralBarFill.isVisible = false
    theEntityManager.commitEntityState(0.0, text, pickaxeSprite, mineImage, mineralBarOutline, mineralBarFill)
  }

  override fun updateEntities() {
    text.isVisible = true
    pickaxeSprite.isVisible = true
    mineImage.isVisible = true
    mineralBarOutline.isVisible = true
    mineralBarFill.isVisible = true
    mineralBarFill.width = 80 * obstacle.minerals / (OBSTACLE_MINERAL_RANGE.last + 2 * OBSTACLE_MINERAL_INCREASE)
    theEntityManager.commitEntityState(0.0, text, pickaxeSprite, mineImage, mineralBarOutline, mineralBarFill)
  }

  override fun act(): Boolean {
    val cash = min(incomeRate, obstacle.minerals)

    owner.resourcesPerTurn += cash
    owner.resources += cash
    obstacle.minerals -= cash
    if (obstacle.minerals <= 0) {
      hideEntities()
      return true
    }

    return false
  }
}

class Tower(override val obstacle: Obstacle, override val owner: Player, var attackRadius: Int, var health: Int) : Structure {
  override fun extraTooltipLines(): List<String> = listOf(
    "TOWER",
    "Range: $attackRadius",
    "Health: $health"
  )

  private val towerRangeCircle = theEntityManager.createCircle()
    .setFillAlpha(0.0)
    .setAlpha(0.2)
    .setLineWidth(10)
    .setZIndex(10)
    .also { it.location = obstacle.location }
    .setRadius(obstacle.radius)
    .also { theEntityManager.commitEntityState(0.0, it) }

  private val sprite = theEntityManager.createSpriteAnimation()
    .setImages(*{
      val color = if (owner.isSecondPlayer) "Bleu" else "Rouge"
      (1..15).map {
        "Tour $color/Tour_$color${it.toString().padStart(4, '0')}.png"
      }
    }().toTypedArray())
    .setZIndex(40)
    .also { it.location = obstacle.location }
    .setAnchorX(0.5).setAnchorY(1 - (220.0 / 238.0 * 0.5))
    .setStarted(true)
    .setLoop(true)
    .setScale(obstacle.radius * 2 / 220.0)

  private val projectile = theEntityManager.createSprite()!!
    .setImage(if (owner.isSecondPlayer) "Eclair_Bleu.png" else "Eclair_Rouge.png")
    .setZIndex(50)
    .setVisible(false)
    .setAnchorX(0.5)
    .setAnchorY(0.5)

  var attackTarget: MyEntity? = null

  override fun hideEntities() {
    towerRangeCircle.radius = obstacle.radius
    towerRangeCircle.isVisible = false
    sprite.isVisible = false
    theEntityManager.commitEntityState(0.0, towerRangeCircle, sprite)
  }

  override fun updateEntities()
  {
    towerRangeCircle.isVisible = true
    towerRangeCircle.lineColor = if (owner.isSecondPlayer) 0x8844ff else 0xff4444
    sprite.isVisible = true
    theEntityManager.commitEntityState(0.0, towerRangeCircle, sprite)
    towerRangeCircle.radius = attackRadius
    theEntityManager.commitEntityState(1.0, towerRangeCircle)

    val localAttackTarget = attackTarget
    if (localAttackTarget != null) {
      projectile.isVisible = true
      val projectileSource = obstacle.location - Vector2(0.0, obstacle.radius * 0.6)
      val obsToTarget = localAttackTarget.location - projectileSource
      projectile.location = (projectileSource + localAttackTarget.location) / 2.0
      projectile.scaleX = obsToTarget.length.toDouble / 200.0
      projectile.scaleY = 1.0
      projectile.setRotation (obsToTarget.angle, Curve.IMMEDIATE)
      theEntityManager.commitEntityState(0.0, projectile)
      projectile.setRotation ((-obsToTarget).angle, Curve.IMMEDIATE)
      projectile.scaleY = 2.0
      theEntityManager.commitEntityState(0.2, projectile)
      projectile.setRotation (obsToTarget.angle, Curve.IMMEDIATE)
      theEntityManager.commitEntityState(0.4, projectile)
      projectile.setRotation ((-obsToTarget).angle, Curve.IMMEDIATE)
      projectile.scaleY = 1.0
      theEntityManager.commitEntityState(0.6, projectile)
      projectile.setRotation (obsToTarget.angle, Curve.IMMEDIATE)
      theEntityManager.commitEntityState(0.8, projectile)
      projectile.setRotation ((-obsToTarget).angle, Curve.IMMEDIATE)
      theEntityManager.commitEntityState(0.99, projectile)
      projectile.isVisible = false
      theEntityManager.commitEntityState(1.0, projectile)

    }
  }

  override fun act(): Boolean {
    val closestEnemy = owner.enemyPlayer.activeCreeps.minBy { it.location.distanceTo(obstacle.location) }
    val enemyQueen = owner.enemyPlayer.queenUnit

    attackTarget = if (closestEnemy != null && closestEnemy.location.distanceTo(obstacle.location) < attackRadius) {
      val shotDistance = closestEnemy.location.distanceTo(obstacle.location).toDouble - obstacle.radius  // should be maximum right at the foot
      closestEnemy.also { it.damage(TOWER_CREEP_DAMAGE_MAX - (shotDistance / TOWER_CREEP_DAMAGE_DROP_DISTANCE).toInt()) }
    } else if (enemyQueen.location.distanceTo(obstacle.location) < attackRadius) {
      enemyQueen.also { it.damage(health / TOWER_HP_PER_QUEEN_DAMAGE + 1 ) }
    } else {
      null
    }

    health -= TOWER_MELT_RATE
    attackRadius = Math.sqrt((health * TOWER_COVERAGE_PER_HP + obstacle.area) / Math.PI).toInt()

    if (health <= 0) {
      hideEntities()
      return true
    }

    return false
  }

}

class Barracks(override val obstacle: Obstacle, override val owner: Player, var creepType: CreepType) : Structure {

  override fun extraTooltipLines(): List<String> {
    val retVal = mutableListOf(
      "BARRACKS ($creepType)"
    )
    if (this.isTraining) retVal += "Progress: $progress/$progressMax"
    return retVal
  }

  private val progressFillMaxWidth =(obstacle.radius * 1.05).toInt()

  private val progressFillMask = theEntityManager.createRectangle()
    .also { it.location = obstacle.location + Vector2(-0.51, 0.72) * obstacle.radius.toDouble() }
    .setHeight(8)
    .setWidth(progressFillMaxWidth)
//    .setLineAlpha(0.0)
//    .setFillColor(owner.colorToken)
//    .setZIndex(400)

  private val progressFill = theEntityManager.createSprite()
    .setImage(if (owner.isSecondPlayer) "Life-Bleu.png" else "Life-Rouge.png")
    .setBaseHeight(8).setBaseWidth(progressFillMaxWidth)
    .also { it.location = obstacle.location + Vector2(-0.51, 0.72) * obstacle.radius.toDouble() }
    .setZIndex(400)
    .setMask(progressFillMask)

  var progressMax = creepType.buildTime
  var progress = 0
  var isTraining = false

  var onComplete: () -> Unit = { }

  private val barracksImage = theEntityManager.createSprite()
    .setAnchor(0.5)
    .setImage(if (owner.isSecondPlayer) "Caserne_Bleu.png" else "Caserne_Rouge.png")
    .setZIndex(40)
    .also { it.location = obstacle.location }
    .setBaseHeight(obstacle.radius * 2).setBaseWidth(obstacle.radius * 2)

  private val creepToken = theEntityManager.createSprite()
    .setAnchor(0.5)
    .setImage(if (owner.isSecondPlayer) "Unite_Base_Bleu.png" else "Unite_Base_Rouge.png")
    .setZIndex(41)
    .also { it.location = obstacle.location + Vector2(-0.1, -0.45) * obstacle.radius.toDouble() }
    .setBaseHeight((barracksImage.baseHeight * 0.32).toInt()).setBaseWidth((barracksImage.baseWidth * 0.32).toInt())

  private val creepSprite = theEntityManager.createSprite()
    .setAnchor(0.5)
    .setZIndex(42)
    .also { it.location = creepToken.location }
    .setBaseHeight(creepToken.baseHeight).setBaseWidth(creepToken.baseWidth)

  override fun updateEntities() {
    barracksImage.isVisible = true
    creepToken.isVisible = true
    creepSprite.isVisible = true
    creepSprite.image = creepType.assetName
    theEntityManager.commitEntityState(0.0, barracksImage, creepToken, creepSprite)

    progressFill.isVisible = isTraining
    theEntityManager.commitEntityState(0.0, progressFill, progressFillMask)
    progressFillMask.width = (progressFillMaxWidth * progress / (progressMax-1))
    theEntityManager.commitEntityState(1.0, progressFillMask)
  }

  override fun hideEntities() {
    barracksImage.isVisible = false
    creepToken.isVisible = false
    creepSprite.isVisible = false
    progressFill.isVisible = false
    theEntityManager.commitEntityState(0.0, barracksImage, creepToken, creepSprite, progressFill, progressFillMask)
  }

  override fun act(): Boolean {
    if (isTraining) {
      progress++
      if (progress == progressMax) {
        progress = 0
        isTraining = false
        onComplete()
      }
    }
    updateEntities()
    return false
  }
}