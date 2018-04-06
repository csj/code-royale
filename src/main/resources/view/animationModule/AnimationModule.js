
import * as utils from '../core/utils.js';
import { WIDTH, HEIGHT } from '../core/constants.js';
import {SpriteAnimation} from '../entity-module/SpriteAnimation.js';
import {api} from '../entity-module/GraphicEntityModule.js';

export class AnimationModule {
  constructor() {
    this.globalData = {};
    this.lastProgress = 1;
    this.lastFrame = 0;

    this.eventAnimators = {
      Death: DeathAnimator
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


class DeathAnimator {
  constructor(event, layer, globalData) {
    // this.time = 0;
    this.anim = new SpriteAnimation();
    this.anim.defaultState.images = [
        'Anim_Death/Mort0001.png',
        'Anim_Death/Mort0006.png',
        'Anim_Death/Mort0011.png',
        'Anim_Death/Mort0016.png',
        'Anim_Death/Mort0021.png',
        'Anim_Death/Mort0026.png',
        'Anim_Death/Mort0031.png',
        'Anim_Death/Mort0036.png',
        'Anim_Death/Mort0041.png',
        'Anim_Death/Mort0046.png'
    ];
    this.anim.defaultState.started = true;

    const newId = ++globalData.instanceCount;
    this.anim.id = newId;
    api.entities[newId] = this.anim;
  }

  // animate(delta) {
  //   this.time += delta;
  //   this.anim.
  //   this.graphics.scale.set(utils.lerp(0, 4, utils.unlerp(0, this.duration, this.time)));
  // }
  //
  // isActive() {
  //   return this.time <= this.duration;
//  }
}