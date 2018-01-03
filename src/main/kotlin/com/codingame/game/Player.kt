package com.codingame.game

import com.codingame.gameengine.core.AbstractPlayer

class Player : AbstractPlayer() {
  override fun getExpectedOutputLines(): Int = 3
  lateinit var kingUnit: MyOwnedEntity
  lateinit var engineerUnit: MyOwnedEntity
  lateinit var generalUnit: MyOwnedEntity
  lateinit var enemyPlayer: Player
  var inverted: Boolean = false

  fun fixLocation(location: Vector2) = if (inverted) Vector2(1920, 1080) - location else location
  private fun fixOwner(player: Player?) = when (player) { null -> -1; this -> 0; else -> 1 }

  fun printLocation(location: Vector2) {
    val (x,y) = fixLocation(location)
    sendInputLine("${x.toInt()} ${y.toInt()}")
  }

  fun printObstacle(obstacle: Obstacle) {
    val (x,y) = fixLocation(obstacle.location)
    val toks = listOf(
      x.toInt(),y.toInt(),
      obstacle.radius,
      fixOwner(obstacle.incomeOwner),
      obstacle.incomeTimer ?: -1,
      fixOwner(obstacle.towerOwner),
      obstacle.towerHealth,
      obstacle.towerAttackRadius
    )
    sendInputLine(toks.joinToString(" "))
  }


  val units by lazy { listOf(kingUnit, engineerUnit, generalUnit) }
  val activeCreeps = mutableListOf<Creep>()

  fun allUnits() = units + activeCreeps

  var health = 200
  private val maxHealth = 200

  fun checkKingHealth(): Boolean {
    kingUnit.entity.fillAlpha = 0.8 * health / maxHealth + 0.2
    if (health <= 0) kingUnit.entity.fillAlpha = 0.0
    return health > 0
  }

  var resources = 0
}