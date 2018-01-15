package com.codingame.game

import com.codingame.game.Constants.KING_HP
import com.codingame.gameengine.core.AbstractPlayer

class Player : AbstractPlayer() {
  override fun getExpectedOutputLines(): Int = 3
  lateinit var kingUnit: MyOwnedEntity
  lateinit var engineerUnit: MyOwnedEntity
  lateinit var generalUnit: MyOwnedEntity
  lateinit var enemyPlayer: Player
  var inverted: Boolean = false

  private fun fixOwner(player: Player?) = when (player) { null -> -1; this -> 0; else -> 1 }

  fun printLocation(location: Vector2) {
    val (x,y) = location
    sendInputLine("${x.toInt()} ${y.toInt()}")
  }

  fun printObstacle(obstacle: Obstacle) {
    val (x,y) = obstacle.location
    val toks = listOf(
      x.toInt(),y.toInt(),
      obstacle.radius,
      fixOwner(obstacle.incomeOwner),
      obstacle.incomeTimer,
      fixOwner(obstacle.towerOwner),
      obstacle.towerHealth,
      obstacle.towerAttackRadius
    )
    sendInputLine(toks.joinToString(" "))
  }


  val units by lazy { listOf(kingUnit, engineerUnit, generalUnit) }
  val activeCreeps = mutableListOf<Creep>()

  fun allUnits() = units + activeCreeps

  var health = KING_HP

  fun checkKingHealth(): Boolean {
    kingUnit.entity.fillAlpha = 0.8 * health / KING_HP + 0.2
    if (health <= 0) kingUnit.entity.fillAlpha = 0.0
    return health > 0
  }

  var resources = 0
}