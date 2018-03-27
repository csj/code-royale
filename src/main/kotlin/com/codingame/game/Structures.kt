package com.codingame.game

import com.codingame.game.Constants.OBSTACLE_MINERAL_INCREASE
import com.codingame.game.Constants.OBSTACLE_MINERAL_RANGE
import com.codingame.game.Constants.TOWER_COVERAGE_PER_HP
import com.codingame.game.Constants.TOWER_CREEP_DAMAGE_DROP_DISTANCE
import com.codingame.game.Constants.TOWER_CREEP_DAMAGE_MAX
import com.codingame.game.Constants.TOWER_MELT_RATE
import com.codingame.game.Constants.TOWER_QUEEN_DAMAGE
import com.codingame.gameengine.module.entities.Circle
import com.codingame.gameengine.module.entities.Curve
import kotlin.math.min

var nextObstacleId = 0
class Obstacle(var maxMineralRate: Int, initialAmount: Int, initialRadius: Int, initialLocation: Vector2): MyEntity() {
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
    }

  fun updateEntities() {
    structure?.updateEntities()
    val struc = structure
    if (struc != null) {
      theTooltipModule.updateExtraTooltipText(outline, *struc.extraTooltipLines().toTypedArray())
    } else {
      theTooltipModule.updateExtraTooltipText(outline, "Radius: $radius", "Remaining resources: $minerals")
    }
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
    .setFillAlpha(0.0)
    .setZIndex(401)!!

  private val mineralBarFill = theEntityManager.createRectangle()
    .also { it.location = obstacle.location + Vector2(-40, -30) }
    .setHeight(25)
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
    mineralBarOutline.isVisible = false
    mineralBarFill.isVisible = false
    theEntityManager.commitEntityState(0.0, text, pickaxeSprite, mineralBarOutline, mineralBarFill)
  }

  override fun updateEntities() {
    text.isVisible = true
    pickaxeSprite.isVisible = true
    mineralBarOutline.isVisible = true
    mineralBarFill.isVisible = true
    mineralBarFill.width = 80 * obstacle.minerals / (OBSTACLE_MINERAL_RANGE.last + 2 * OBSTACLE_MINERAL_INCREASE)
    theEntityManager.commitEntityState(0.0, text, pickaxeSprite, mineralBarOutline, mineralBarFill)
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
    .setLineWidth(10)
    .setZIndex(10)
    .also { it.location = obstacle.location }
    .setRadius(obstacle.radius)
    .also { theEntityManager.commitEntityState(0.0, it) }

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

  var attackTarget: MyEntity? = null

  override fun hideEntities() {
    towerRangeCircle.radius = obstacle.radius
    towerRangeCircle.isVisible = false
    sprite.isVisible = false
    fillSprite.isVisible = false
  }

  override fun updateEntities()
  {
    towerRangeCircle.isVisible = true
    towerRangeCircle.lineColor = owner.colorToken
    sprite.isVisible = true
    fillSprite.isVisible = true
    fillSprite.tint = owner.colorToken
    theEntityManager.commitEntityState(0.0, towerRangeCircle, sprite, fillSprite)
    towerRangeCircle.radius = attackRadius
    theEntityManager.commitEntityState(1.0, towerRangeCircle)

    val localAttackTarget = attackTarget
    if (localAttackTarget != null) {
      projectile.isVisible = true
      projectile.setX(obstacle.location.x.toInt() + viewportX.first, Curve.NONE)
      projectile.setY(obstacle.location.y.toInt() + viewportY.first, Curve.NONE)
      theEntityManager.commitEntityState(0.0, projectile)
      projectile.setX(localAttackTarget.location.x.toInt() + viewportX.first, Curve.EASE_IN_AND_OUT)
      projectile.setY(localAttackTarget.location.y.toInt() + viewportY.first, Curve.EASE_IN_AND_OUT)
      theEntityManager.commitEntityState(0.99, projectile)
      projectile.isVisible = false
      theEntityManager.commitEntityState(1.0, projectile)
    }
  }

  override fun act(): Boolean {
    val closestEnemy = owner.enemyPlayer.activeCreeps.minBy { it.location.distanceTo(obstacle.location) }
    val enemyQueen = owner.enemyPlayer.queenUnit

    attackTarget = if (closestEnemy != null && closestEnemy.location.distanceTo(obstacle.location) < attackRadius) {
      val shotDistance = closestEnemy.location.distanceTo(obstacle.location) - obstacle.radius  // should be maximum right at the foot
      closestEnemy.also { it.damage(TOWER_CREEP_DAMAGE_MAX - (shotDistance / TOWER_CREEP_DAMAGE_DROP_DISTANCE).toInt()) }
    } else if (enemyQueen.location.distanceTo(obstacle.location) < attackRadius) {
      enemyQueen.also { it.damage(TOWER_QUEEN_DAMAGE) }
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

  private val progressOutline = theEntityManager.createRectangle()
    .also { it.location = obstacle.location + Vector2(-40,20) }
    .setHeight(25)
    .setWidth(80)
    .setLineColor(0xFFFFFF)
    .setLineWidth(1)
    .setFillAlpha(0.0)
    .setZIndex(401)

  private val progressFill = theEntityManager.createRectangle()
    .also { it.location = obstacle.location + Vector2(-40,20) }
    .setHeight(25)
    .setWidth(80)
    .setLineAlpha(0.0)
    .setFillColor(owner.colorToken)
    .setZIndex(400)

  var progressMax = creepType.buildTime
  var progress = 0
  var isTraining = false

  var onComplete: () -> Unit = { }

  private val creepSprite = theEntityManager.createSprite()
    .setAnchor(0.5)
    .setZIndex(40)
    .also { it.location = obstacle.location }
    .setScale(2.0)!!
//
//  private val creepFillSprite = theEntityManager.createSprite()
//    .setTint(owner.colorToken)
//    .setAnchor(0.5)
//    .setZIndex(30)
//    .also { it.location = obstacle.location }
//    .setTint(owner.colorToken)
//    .setScale(2.0)!!

  override fun updateEntities() {
    creepSprite.isVisible = true
    creepSprite.image = creepType.assetName
//    creepFillSprite.isVisible = true
//    creepFillSprite.image = creepType.fillAssetName
//    theEntityManager.commitEntityState(0.0, creepSprite, creepFillSprite)

//    creepFillSprite.location = obstacle.location + Vector2(0, if (isTraining) -20 else 0)
    creepSprite.location = obstacle.location + Vector2(0, if (isTraining) -20 else 0)
//    theEntityManager.commitEntityState(0.3, creepSprite, creepFillSprite)

    progressOutline.isVisible = isTraining
    progressFill.isVisible = isTraining
    theEntityManager.commitEntityState(0.0, progressFill, progressOutline)
    progressFill.width = (80 * progress / (progressMax-1))
    theEntityManager.commitEntityState(1.0, progressFill, progressOutline)
  }

  override fun hideEntities() {
    //creepFillSprite.isVisible = false
    creepSprite.isVisible = false
    progressOutline.isVisible = false
    progressFill.isVisible = false
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