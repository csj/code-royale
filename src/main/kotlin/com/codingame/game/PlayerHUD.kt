package com.codingame.game

class PlayerHUD(private val player: Player, isSecondPlayer: Boolean) {
  private val left = 0 //if (isSecondPlayer) 1920/2 else 0
  private val right = 1920 //if (isSecondPlayer) 1920 else 1920/2
  private val top = if (isSecondPlayer) viewportY.first/2 else 0 //0 //viewportY.last
  private val bottom = if (isSecondPlayer) viewportY.first else viewportY.first/2 //1080

  private val healthBarWidth = 400
  private val healthBarPadding = 10
  private val healthBarMargin = 20

  private val background = theEntityManager.createRectangle()!!
    .setX(left).setY(top)
    .setWidth(right-left).setHeight(bottom-top)
    .setFillColor(player.colorToken)
    .setLineAlpha(0.0)
    .setZIndex(4000)

  private val avatar = theEntityManager.createSprite()
    .setImage(player.avatarToken)
    .setX(left + 10).setY(top + 10)
    .setBaseWidth(bottom - top - 10 - 10)
    .setBaseHeight(bottom - top - 10 - 10)
    .setZIndex(4003)!!

  private val avatarFrame = theEntityManager.createRectangle()!!
    .setX(avatar.x).setY(avatar.y)
    .setWidth(avatar.baseWidth).setHeight(avatar.baseHeight)
    .setFillColor(0)
    .setFillAlpha(0.5)
    .setZIndex(4001)

  private val healthBarBackground = theEntityManager.createRectangle()!!
    .setX(left + 120 - healthBarPadding).setY(top + healthBarMargin - healthBarPadding)
    .setWidth(healthBarWidth + 2*healthBarPadding).setHeight(bottom - top - 2*healthBarMargin + 2*healthBarPadding)
    .setLineAlpha(0.0)
    .setFillColor(0).setFillAlpha(0.4)
    .setZIndex(4001)

  private val healthBarFill = theEntityManager.createRectangle()!!
    .setLineAlpha(0.0)
    .setX(left + 120).setY(top + healthBarMargin)
    .setWidth(healthBarWidth).setHeight(bottom - top - 2*healthBarMargin)
    .setFillColor(0x55ff55)
    .setLineAlpha(0.0)
    .setZIndex(4002)

  private val playerName = theEntityManager.createText(player.nicknameToken)!!
    .setX(left + 120 + 5).setY(top + healthBarMargin - 2)
    .setFillColor(0)
    .setScale(1.8)
    .setZIndex(4003)

  private val heartSprite = theEntityManager.createSprite()
    .setX(healthBarBackground.x + healthBarBackground.width + 40).setY((top + bottom)/2)
    .setScale(2.0)
    .setImage("heart.png")
    .setAnchor(0.5)
    .setZIndex(4002)!!

  private val healthText = theEntityManager.createText(player.health.toString())!!
    .setX(heartSprite.x + 30).setY(top + 20)
    .setScale(1.8)
    .setFillColor(0)
    .setZIndex(4003)

  private val moneySprite = theEntityManager.createSprite()
    .setX(heartSprite.x + 180).setY((top + bottom)/2)
    .setImage("money.png")
    .setScale(2.0)
    .setAnchor(0.5)
    .setZIndex(4002)!!

  private val moneyText = theEntityManager.createText("0")
    .setX(moneySprite.x + 30).setY(top + 20)
    .setScale(1.8)
    .setZIndex(4002)!!

  private val moneyIncText = theEntityManager.createText("")
    .setX(moneySprite.x + 120).setY(top + 20)
    .setScale(1.8)
    .setZIndex(4002)!!

  private val meleeSprite = theEntityManager.createSprite()
    .setImage("bug.png")
    .setScale(1.3)
    .setTint(0)
    .setX(moneySprite.x + 500).setY((top + bottom)/2).setAnchor(0.5)
    .setZIndex(4002)

  private val meleeCountText = theEntityManager.createText("x0")
    .setScale(1.8)
    .setX(meleeSprite.x + 25).setY(top + 20)
    .setZIndex(4002)

  private val rangedSprite = theEntityManager.createSprite()
    .setImage("archer.png")
    .setScale(1.3)
    .setTint(0)
    .setX(meleeSprite.x + 160).setY((top + bottom)/2).setAnchor(0.5)
    .setZIndex(4002)

  private val rangedCountText = theEntityManager.createText("x0")
    .setScale(1.8)
    .setX(rangedSprite.x + 25).setY(top + 20)
    .setZIndex(4002)

  private val giantSprite = theEntityManager.createSprite()
    .setImage("bulldozer.png")
    .setScale(1.3)
    .setTint(0)
    .setX(rangedSprite.x + 160).setY((top + bottom)/2).setAnchor(0.5)
    .setZIndex(4002)

  private val giantCountText = theEntityManager.createText("x0")
    .setScale(1.8)
    .setX(giantSprite.x + 25).setY(top + 20)
    .setZIndex(4002)

  private val towerSprite = theEntityManager.createSprite()
    .setImage("tower.png")
    .setScale(1.0)
    .setTint(0)
    .setX(giantSprite.x + 160).setY((top + bottom)/2).setAnchor(0.5)
    .setZIndex(4002)

  private val towerCountText = theEntityManager.createText("x0")
    .setScale(1.8)
    .setX(towerSprite.x + 25).setY(top + 20)
    .setZIndex(4002)

  fun update() {
    healthBarFill.width = healthBarWidth * player.health / Constants.QUEEN_HP
    healthText.text = player.health.toString()
    moneyText.text = player.resources.toString()
    moneyIncText.text = when (player.resourcesPerTurn) {
      0 -> ""
      else -> "(+${player.resourcesPerTurn})"
    }
    meleeCountText.text = "x" + player.activeCreeps.count { it.creepType == CreepType.MELEE }.toString()
    rangedCountText.text = "x" + player.activeCreeps.count { it.creepType == CreepType.RANGED }.toString()
    giantCountText.text = "x" + player.activeCreeps.count { it.creepType == CreepType.GIANT }.toString()
    towerCountText.text = "x" + obstacles.count { it.structure is Tower && it.structure!!.owner == player }
    theEntityManager.commitEntityState(0.0, moneyText, healthText, moneyIncText, meleeCountText,
      rangedCountText, giantCountText, towerCountText)
  }

  companion object {
    lateinit var obstacles: List<Obstacle>
  }
}