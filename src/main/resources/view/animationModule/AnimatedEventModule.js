
import * as utils from '../core/utils.js';
import { WIDTH, HEIGHT } from '../core/constants.js';

export class AnimatedEventModule {
  constructor(assets) {
    this.globalData = {};
    this.lastProgress = 1;
    this.lastFrame = 0;

    this.eventAnimators = {
      Ping: PingAnimator
    };
    this.activeAnimators = [];
  }

  static get name() {
    return 'animations';
  }

  launchAnimation(event) {
    if (this.eventAnimators.hasOwnProperty(event.id)) {
      const layer = new PIXI.Container();
      const animator = new this.eventAnimators[event.id](event, layer, this.globalData);
      this.activeAnimators.push({ animator, layer });
      this.container.addChild(layer);
    }
  }

  updateScene(previousData, currentData, progress) {
    const frame = currentData.number;

    for (const event of currentData.events) {
      if (frame === this.lastFrame && event.t > this.lastProgress && event.t <= progress) {
        this.launchAnimation(event);
      } else if (this.lastFrame === previousData.number && event.t < progress) {
        this.launchAnimation(event);
      }
    }

    this.lastProgress = progress;
    this.lastFrame = frame;
  }

  handleFrameData(frameInfo, frameData) {
    return { events: frameData, number: frameInfo.number};
  }

  reinitScene(container, canvasData) {
    this.container = container;
    this.activeAnimators = [];
  }

  animateScene(delta) {
    for (const animatorData of this.activeAnimators) {
      animatorData.animator.animate(delta);
    }
    this.activeAnimators = this.activeAnimators.reduce((stillActive, animatorData) => {
      const animator = animatorData.animator;
      if (animator.isActive()) {
        return [...stillActive, animatorData];
      } else {
        animatorData.layer.parent.removeChild(animatorData.layer);
        return stillActive;
      }
    }, []);
  }

  handleGlobalData(players, globalData) {
    this.globalData.players = players;
    const width = globalData.width;
    const height = globalData.height;
    this.globalData.coeff = utils.fitAspectRatio(width, height, WIDTH, HEIGHT);
  }

}


class PingAnimator {
  constructor(event, layer, globalData) {
    this.time = 0;
    const g = new PIXI.Graphics();
    g.lineStyle(3, globalData.players[event.params.player].color, 0.8);
    g.drawCircle(0, 0, 20);
    g.position.set(event.params.x * globalData.coeff, event.params.y * globalData.coeff);
    layer.addChild(g);
    this.graphics = g;
    this.duration = 600; //ms
  }

  animate(delta) {
    this.time += delta;
    this.graphics.scale.set(utils.lerp(0, 4, utils.unlerp(0, this.duration, this.time)));
  }

  isActive() {
    return this.time <= this.duration;
  }
}