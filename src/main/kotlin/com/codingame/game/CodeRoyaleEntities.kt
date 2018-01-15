package com.codingame.game

import com.codingame.gameengine.core.GameManager
import com.codingame.gameengine.module.entities.GraphicEntityModule
import com.codingame.gameengine.module.entities.Rectangle

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

open class KillableUnit(x: Int, y: Int, id: Int, entityManager: GraphicEntityModule, owner: Player, unitType: Int):
        OwnedUnit(x, y, id, entityManager, owner, unitType) {

  var health = MAX_HEALTH

  open fun damage(gameManager: GameManager<Player>) {

  }

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
        KillableUnit(x, y, id, entityManager, owner, unitType) {

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
        KillableUnit(x, y, id, entityManager, owner, unitType) {

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