package com.codingame.game

import com.codingame.gameengine.module.entities.Circle
import com.codingame.gameengine.module.entities.GraphicEntityModule
import com.codingame.gameengine.module.entities.Rectangle

//abstract class MyEntity {
//  var location = Vector2.Zero
//  abstract val mass: Int   // 0 := immovable
//  abstract val entity: Circle
//
//  open fun updateEntity() {
//    entity.x = location.x.toInt()
//    entity.y = location.y.toInt()
//    entity.zIndex = 50
//  }
//}
//
//abstract class MyOwnedEntity(val owner: Player) : MyEntity()
//
//class Obstacle(entityManager: GraphicEntityModule): MyEntity() {
//  override val mass = 0
//
//  val radius = (Math.random() * (OBSTACLE_MAX_RADIUS - OBSTACLE_MIN_RADIUS) + OBSTACLE_MIN_RADIUS).toInt()
//  private val area = Math.PI * radius * radius
//
//  var incomeOwner: Player? = null
//  var incomeTimer: Int = 0
//
//  var towerOwner: Player? = null
//  var towerAttackRadius: Int = 0
//  var towerHealth: Int = 0
//
//  override val entity: Circle = entityManager.createCircle()
//          .setRadius(radius)
//          .setFillColor(0)
//          .setFillAlpha(0.5)
//          .setZIndex(100)
//
//  private val towerRangeCircle = entityManager.createCircle()
//          .setFillAlpha(0.15)
//          .setFillColor(0)
//          .setLineColor(0)
//          .setZIndex(10)
//
//  val sprite = entityManager.createSprite()
//          .setImage("tower.png")
//          .setZIndex(40)
//
//  val fillSprite = entityManager.createSprite()
//          .setImage("towerfill.png")
//          .setZIndex(30)
//
//  fun updateEntities() {
//    if (towerOwner != null) {
//      towerRangeCircle.isVisible = true
//      towerRangeCircle.fillColor = towerOwner!!.colorToken
//      towerRangeCircle.radius = towerAttackRadius
//      sprite.isVisible = true
//      fillSprite.isVisible = true
//      fillSprite.tint = towerOwner!!.colorToken
//    } else {
//      entity.fillColor = 0
//      towerRangeCircle.isVisible = false
//      sprite.isVisible = false
//      fillSprite.isVisible = false
//    }
//    if (incomeOwner == null) {
//      entity.fillColor = 0
//    } else {
//      entity.fillColor = incomeOwner!!.colorToken
//      entity.fillAlpha = 0.2 + 0.8 * (incomeTimer / INCOME_TIMER)
//    }
//
//    sprite.x = location.x.toInt()
//    sprite.y = location.y.toInt()
//    fillSprite.x = location.x.toInt()
//    fillSprite.y = location.y.toInt()
//    towerRangeCircle.x = location.x.toInt()
//    towerRangeCircle.y = location.y.toInt()
//  }
//
//  fun act() {
//    if (towerOwner != null) {
//      val damage = 6 + (Math.random() * 3).toInt()
//      val closestEnemy = towerOwner!!.enemyPlayer.activeCreeps.minBy { it.location.distanceTo(location) }
//      if (closestEnemy != null && closestEnemy.location.distanceTo(location) < towerAttackRadius) {
//        closestEnemy.damage(damage)
//      } else if (towerOwner!!.enemyPlayer.kingUnit.location.distanceTo(location) < towerAttackRadius) {
//        towerOwner!!.enemyPlayer.health -= 1
//      }
//
//      val enemyGeneral = towerOwner!!.enemyPlayer.generalUnit
//      if (enemyGeneral.location.distanceTo(location) < towerAttackRadius) {
//        enemyGeneral.location += (enemyGeneral.location - location).resizedTo(20.0)
//      }
//
//      towerHealth -= TOWER_MELT_RATE
//      towerAttackRadius = Math.sqrt((towerHealth * TOWER_COVERAGE_PER_HP + area) / Math.PI).toInt()
//
//      if (towerHealth < 0) {
//        towerHealth = -1
//        towerOwner = null
//      }
//    }
//
//    if (incomeOwner != null) {
//      incomeTimer -= 1
//      if (incomeTimer <= 0) incomeOwner = null
//    }
//    if (incomeOwner != null) incomeOwner!!.resources += 1
//
//    updateEntities()
//  }
//
//  init {
//    this.location = Vector2.random(1920, 1080)
//  }
//
//  fun setTower(owner: Player, health: Int) {
//    towerOwner = owner
//    this.towerHealth = health
//  }
//}
//
//class TowerBustingCreep(
//        entityManager: GraphicEntityModule,
//        owner: Player,
//        location: Vector2,
//        creepType: CreepType,
//        private val obstacles: List<Obstacle>
//) : Creep(entityManager, owner, location, creepType
//) {
//  override fun move() {
//    obstacles
//            .filter { it.towerOwner == owner.enemyPlayer }
//            .minBy { it.location.distanceTo(location) }
//            ?.let {
//              location = location.towards(it.location, speed.toDouble())
//            }
//  }
//
//  override fun dealDamage() {
//    obstacles
//            .firstOrNull {
//              it.towerOwner == owner.enemyPlayer &&
//                      it.location.distanceTo(location) - entity.radius - it.radius < 10
//            }?.let {
//      it.towerHealth -= 5
//    }
//
//    val enemyKing = owner.enemyPlayer.kingUnit
//    if (location.distanceTo(enemyKing.location) < entity.radius + enemyKing.entity.radius + attackRange + 10) {
//      owner.enemyPlayer.health -= 1
//    }
//  }
//}
//
//class KingChasingCreep(
//        entityManager: GraphicEntityModule,
//        owner: Player,
//        location: Vector2,
//        creepType: CreepType) : Creep(entityManager, owner, location, creepType
//) {
//  override fun move() {
//    val enemyKing = owner.enemyPlayer.kingUnit
//    // move toward enemy king, if not yet in range
//    if (location.distanceTo(enemyKing.location) - entity.radius - enemyKing.entity.radius > attackRange)
//      location = location.towards((enemyKing.location + (location - enemyKing.location).resizedTo(3.0)), speed.toDouble())
//  }
//
//  override fun dealDamage() {
//    val enemyKing = owner.enemyPlayer.kingUnit
//    if (location.distanceTo(enemyKing.location) < entity.radius + enemyKing.entity.radius + attackRange + 10) {
//      owner.enemyPlayer.health -= 1
//    }
//  }
//}
//
//abstract class Creep(
//        entityManager: GraphicEntityModule,
//        owner: Player,
//        location: Vector2,
//        val creepType: CreepType
//) : MyOwnedEntity(owner) {
//
//  protected val speed: Int = creepType.speed
//  val attackRange: Int = creepType.range
//  private val radius: Int = creepType.radius
//  override val mass: Int = creepType.mass
//  var health: Int = creepType.hp
//  private val maxHealth = health
//
//  override val entity = entityManager.createCircle()
//          .setRadius(radius)
//          .setFillColor(owner.colorToken)
//          .setVisible(false)!!
//
//  val sprite = entityManager.createSprite()
//          .setImage(creepType.assetName)
//          .setZIndex(40)
//
//  val fillSprite = entityManager.createSprite()
//          .setImage(creepType.fillAssetName)
//          .setTint(owner.colorToken)
//          .setZIndex(30)
//
//  init {
//    this.location = location
//    @Suppress("LeakingThis") updateEntity()
//  }
//
//  override fun updateEntity() {
//    super.updateEntity()
//    fillSprite.alpha = health.toDouble() / maxHealth * 0.8 + 0.2
//    fillSprite.x = location.x.toInt()
//    fillSprite.y = location.y.toInt()
//    sprite.x = location.x.toInt()
//    sprite.y = location.y.toInt()
//  }
//
//  fun damage(hp: Int) {
//    health -= hp
//    if (health <= 0) {
//      entity.isVisible = false
//      sprite.alpha = 0.0
//      fillSprite.alpha = 0.0
//      owner.activeCreeps.remove(this)
//    }
//  }
//
//  abstract fun dealDamage()
//  abstract fun move()
//}


// MY STUFF

abstract class MyUnit(var x: Int, var y: Int, val id: Int, entityManager: GraphicEntityModule) {
  abstract fun draw()
  abstract val entity: Rectangle
  abstract fun sendInput(player: Player) : String
  var savedX : Int = 0
  var savedY : Int = 0
  open val moveDist = 1

  fun save() {
    savedX = x
    savedY = y
  }

  fun reset() {
    x = savedX
    y = savedY
  }

  override fun equals(other: Any?): Boolean {
    if (other is MyUnit)
      return other.id == id
    else
      return false
  }

  override fun hashCode(): Int {
    return id
  }
}

class Resource(x: Int, y: Int, id: Int, entityManager: GraphicEntityModule, var resourceAmount: Int) :
        MyUnit(x, y, id, entityManager) {

  override val entity = entityManager.createRectangle()
          .setWidth(gridSizeX)
          .setHeight(gridSizeY)
          .setX(x * gridSizeX + MIN_X)
          .setY(y * gridSizeY + MIN_Y)
          .setFillColor(0x00ff00)!!


  override fun draw() {
    entity.x = x * gridSizeX + MIN_X
    entity.y = y * gridSizeY + MIN_Y
  }

  override fun sendInput(player: Player) : String {
    return "$id -1 $RESOURCE_TYPE $x $y 0"
  }

}

open class OwnedUnit(x: Int, y: Int, id: Int, entityManager: GraphicEntityModule, val owner: Player, val unitType: Int):
        MyUnit(x, y, id, entityManager) {

  override val entity = entityManager.createRectangle()
          .setWidth(gridSizeX)
          .setHeight(gridSizeY)
          .setX(x * gridSizeX + MIN_X)
          .setY(y * gridSizeY + MIN_Y)
          .setFillColor(owner.colorToken)!!

  override fun draw() {
    entity.x = x * gridSizeX + MIN_X
    entity.y = y * gridSizeY + MIN_Y
  }

  override fun sendInput(player: Player): String {
    return "$id ${if(player == owner) 0 else 1} $unitType $x $y 0"
  }
}

class Minion(x: Int, y: Int, id: Int, entityManager: GraphicEntityModule, owner: Player, unitType: Int) :
        OwnedUnit(x, y, id, entityManager, owner, unitType) {

  val health = MAX_HEALTH

  override val entity = entityManager.createRectangle()
          .setWidth(gridSizeX)
          .setHeight(gridSizeY)
          .setX(x * gridSizeX + MIN_X)
          .setY(y * gridSizeY + MIN_Y)
          .setFillColor(owner.colorToken)!!


  override fun draw() {
    entity.x = x * gridSizeX + MIN_X
    entity.y = y * gridSizeY + MIN_Y
  }

  override fun sendInput(player: Player): String {
    return "$id ${if(player == owner) 0 else 1} $unitType $x $y 0"
  }

}

class Tower(x: Int, y: Int, id: Int, entityManager: GraphicEntityModule, owner: Player, unitType: Int) :
        OwnedUnit(x, y, id, entityManager, owner, unitType) {

  val health = MAX_HEALTH

  override val entity = entityManager.createRectangle()
          .setWidth(gridSizeX)
          .setHeight(gridSizeY)
          .setX(x * gridSizeX + MIN_X)
          .setY(y * gridSizeY + MIN_Y)
          .setFillColor(owner.colorToken)!!


  override fun draw() {
    entity.x = x * gridSizeX + MIN_X
    entity.y = y * gridSizeY + MIN_Y
  }

  override fun sendInput(player: Player): String {
    return "$id ${if(player == owner) 0 else 1} $unitType $x $y 0"
  }

}