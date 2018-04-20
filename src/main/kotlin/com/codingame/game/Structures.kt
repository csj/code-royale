package com.codingame.game

import com.codingame.game.Constants.OBSTACLE_GOLD_INCREASE
import com.codingame.game.Constants.OBSTACLE_GOLD_RANGE
import com.codingame.game.Constants.TOWER_COVERAGE_PER_HP
import com.codingame.game.Constants.TOWER_CREEP_DAMAGE_CLIMB_DISTANCE
import com.codingame.game.Constants.TOWER_CREEP_DAMAGE_MIN
import com.codingame.game.Constants.TOWER_MELT_RATE
import com.codingame.game.Constants.TOWER_QUEEN_DAMAGE_CLIMB_DISTANCE
import com.codingame.game.Constants.TOWER_QUEEN_DAMAGE_MIN
import com.codingame.gameengine.module.entities.Curve
import java.util.Random
import kotlin.Unit
import kotlin.math.min
import kotlin.math.sqrt

var nextObstacleId = 0
val rando = Random()

class Obstacle(var maxMineSize: Int, initialGold: Int, initialRadius: Int, initialLocation: Vector2): FieldObject() {
  val obstacleId = nextObstacleId++
  override val mass = 0
  var gold by nonNegative(initialGold)

  private val obstacleImage = theEntityManager.createSprite()
    .setImage("LC_${rando.nextInt(10) + 1}")
    .setZIndex(20)
    .setAnchor(0.5)

  override var radius: Int = 0
    set(value) {
      field = value
      obstacleImage.setScale(value * 2 / 220.0)
    }

  override var location: Vector2 = initialLocation
    set(value) {
      field = value
      obstacleImage.location = location
    }

  init {
    radius = initialRadius
    location = initialLocation
    val params = hashMapOf("id" to obstacleId, "type" to "Site")
    theTooltipModule.registerEntity(obstacleImage, params as Map<String, Any>?)
  }

  val area = Math.PI * radius * radius

  var structure: Structure? = null
    set(value) {
      if (value != null && structure != value)
        theAnimModule.createAnimationEvent("construction", 0.0).location = location   // includes replacing
      if (value == null && structure != null)
        theAnimModule.createAnimationEvent("destruction", 0.0).location = location

      structure?.hideEntities()
      field = value
      value?.updateEntities()
      if (value == null) {
        obstacleImage.alpha = 1.0
      } else {
        obstacleImage.alpha = 0.0
        obstacleImage.image = "LieuDetruit"
      }
      obstacleImage.alpha = if (value == null) 1.0 else 0.0
      theEntityManager.commitEntityState(0.0, obstacleImage)
    }

  fun updateEntities() {
    structure?.updateEntities()
    val struc = structure
    val lines = listOf(
      "Radius: $radius",
      "Remaining gold: $gold"
    ) + (struc?.extraTooltipLines() ?: listOf())
    theTooltipModule.updateExtraTooltipText(obstacleImage, *lines.toTypedArray())
  }

  fun destroy() {
    obstacleImage.isVisible = false
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
    .setImage("Mine")
    .setZIndex(40)
    .setAnchor(0.5)
    .also { it.location = obstacle.location }
    .setScale(obstacle.radius * 2 / 220.0)

  private val pickaxeSprite = theEntityManager.createSprite()
    .setImage(if (owner.isSecondPlayer) "Mine_Bleu" else "Mine_Rouge")
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
    theEntityManager.commitEntityState(0.51, text, pickaxeSprite, mineImage, mineralBarOutline, mineralBarFill)
  }

  override fun updateEntities() {
    text.isVisible = true
    pickaxeSprite.isVisible = true
    mineImage.isVisible = true
    mineralBarOutline.isVisible = true
    mineralBarFill.isVisible = true
    mineralBarFill.width = 80 * obstacle.gold / (OBSTACLE_GOLD_RANGE.last + 2 * OBSTACLE_GOLD_INCREASE)
    theEntityManager.commitEntityState(0.5, text, pickaxeSprite, mineImage, mineralBarOutline, mineralBarFill)
  }

  override fun act(): Boolean {
    val cash = min(incomeRate, obstacle.gold)

    owner.goldPerTurn += cash
    owner.gold += cash
    obstacle.gold -= cash
    if (obstacle.gold <= 0) {
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
      val color = if (owner.isSecondPlayer) "B" else "R"
      (1..15).map {
        "T$color${it.toString().padStart(2, '0')}"
      }
    }().toTypedArray())
    .setZIndex(40)
    .also { it.location = obstacle.location }
    .setAnchorX(0.5).setAnchorY(1 - (220.0 / 238.0 * 0.5))
    .setStarted(true)
    .setLoop(true)
    .setScale(obstacle.radius * 2 / 220.0)

  private val projectile = theEntityManager.createSprite()!!
    .setImage(if (owner.isSecondPlayer) "Eclair_Bleu" else "Eclair_Rouge")
    .setZIndex(50)
    .setVisible(false)
    .setAnchorX(0.5)
    .setAnchorY(0.5)

  var attackTarget: FieldObject? = null

  override fun hideEntities() {
    towerRangeCircle.radius = obstacle.radius
    towerRangeCircle.isVisible = false
    sprite.isVisible = false
    theEntityManager.commitEntityState(0.51, towerRangeCircle, sprite)
  }

  override fun updateEntities()
  {
    towerRangeCircle.isVisible = true
    towerRangeCircle.lineColor = if (owner.isSecondPlayer) 0x8844ff else 0xff4444
    sprite.isVisible = true
    theEntityManager.commitEntityState(0.5, towerRangeCircle, sprite)
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

  private fun damageCreep(target: Creep) {
    val shotDistance = target.location.distanceTo(obstacle.location).toDouble - obstacle.radius
    val differenceFromMax = attackRadius - shotDistance
    val damage = TOWER_CREEP_DAMAGE_MIN + (differenceFromMax / TOWER_CREEP_DAMAGE_CLIMB_DISTANCE).toInt()
    target.damage(damage)
  }

  private fun damageQueen(target: Queen) {
    val shotDistance = target.location.distanceTo(obstacle.location).toDouble - obstacle.radius
    val differenceFromMax = attackRadius - shotDistance
    val damage = TOWER_QUEEN_DAMAGE_MIN + (differenceFromMax / TOWER_QUEEN_DAMAGE_CLIMB_DISTANCE).toInt()
    target.damage(damage)
  }

  override fun act(): Boolean {
    val closestEnemy = owner.enemyPlayer.activeCreeps.minBy { it.location.distanceTo(obstacle.location) }
    val enemyQueen = owner.enemyPlayer.queenUnit

    attackTarget = when {
      closestEnemy != null && closestEnemy.location.distanceTo(obstacle.location) < attackRadius ->
        closestEnemy.also { damageCreep(it) }
      enemyQueen.location.distanceTo(obstacle.location) < attackRadius ->
        enemyQueen.also { damageQueen(it) }
      else -> null
    }

    health -= TOWER_MELT_RATE
    attackRadius = sqrt((health * TOWER_COVERAGE_PER_HP + obstacle.area) / Math.PI).toInt()

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

  var progressMax = creepType.buildTime
  var progress = 0
  var isTraining = false

  var onComplete: () -> Unit = { }

  private val barracksImage = theEntityManager.createSprite()
    .setAnchor(0.5)
    .setImage(if (owner.isSecondPlayer) "Caserne_Bleu" else "Caserne_Rouge")
    .setZIndex(40)
    .also { it.location = obstacle.location }
    .setBaseHeight(obstacle.radius * 2).setBaseWidth(obstacle.radius * 2)

  private val progressFill = theEntityManager.createSprite()
    .setAnchor(0.5)
    .setImage(if (owner.isSecondPlayer) "Caserne_Bleu_Jauge" else "Caserne_Rouge_Jauge")
    .setZIndex(400)
    .also { it.location = obstacle.location.plus(Vector2(0, obstacle.radius * 53/68)) }
          .setBaseWidth(obstacle.radius * 2 * 70 / 138)
          .setBaseHeight((obstacle.radius * 2 * 70 / 138 * 10.0/110.0).toInt())
    .setMask(progressFillMask)

  private val creepToken = theEntityManager.createSprite()
    .setAnchor(0.5)
    .setImage(if (owner.isSecondPlayer) "Unite_Base_Bleu" else "Unite_Base_Rouge")
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
    theEntityManager.commitEntityState(0.5, barracksImage, creepToken, creepSprite)

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
    theEntityManager.commitEntityState(0.51, barracksImage, creepToken, creepSprite, progressFill, progressFillMask)
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