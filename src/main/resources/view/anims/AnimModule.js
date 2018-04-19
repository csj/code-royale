
import * as utils from '../core/utils.js';
import {WIDTH, HEIGHT} from '../core/constants.js';
import {FRAMES, ANCHORS} from './AnimData.js';
import {api as entityModule} from '../entity-module/GraphicEntityModule.js';

const DURATIONS = {
  death: 1,
  destruction: 1,
  construction: 1
}

const REPEAT = {
  death: 1,
  destruction: 1,
  construction: 1
}

export class AnimModule {
  constructor(assets) {
    this.loadState = 1;
    this.animData = [];
    this.anims = {};
    this.frames = 0;
    this.currentData = {number: 0};
    this.progress = 0;
  }

  static get name() {
    return 'anims';
  }

  updateScene(previousData, currentData, progress) {
    this.currentData = currentData;
    this.progress = progress;

    for (let data of this.animData) {
      let visible = false;
      let start = data.started.frame + data.started.t;
      let end = start + data.duration;
      let now = currentData.number + progress;

      if (start <= now && end > now) {
        visible = true;
      }

      if (!visible && data.sprite) {
        data.sprite.visible = false;
        data.sprite.busyp = false;
        data.sprite = null;
      } else if (visible && !data.sprite) {
        data.sprite = this.getAnimFromPool(data.id);
        data.sprite.visible = true;
      }

      if (visible) {
        if (this.loadState > 0) {
          const repeats = REPEAT[data.id] || 1;

          let image = FRAMES[data.id][0];
          if (repeats > 1) {
            const animationProgress = utils.unlerpUnclamped(start, end, now) * repeats;
            if (animationProgress >= 0) {
              const animationIndex = Math.floor(FRAMES[data.id].length * animationProgress);
              image = FRAMES[data.id][animationIndex % FRAMES[data.id].length];
            }
          } else {
            const animationProgress = utils.unlerp(start, end, now);
            const animationIndex = Math.floor(FRAMES[data.id].length * animationProgress);
            image = (FRAMES[data.id][animationIndex] || FRAMES[data.id][FRAMES[data.id].length - 1]);
          }

          data.sprite.texture = PIXI.Texture.fromFrame(image);
        }
        if (data.params.x && data.params.y) {
          data.sprite.position.x = +data.params.x
          data.sprite.position.y = +data.params.y
        }
      }
    }
  }

  handleFrameData(frameInfo, anims) {
    const number = (frameInfo.number == 0) ? 0 : ++this.frames;

    for (let a of anims) {
      a.started = {frame: number, t: a.t}
      a.duration = DURATIONS[a.id] || 1;
      a.duration *= REPEAT[a.id] || 1;
      this.animData.push(a);
    }
    return {number};
  }

  getAnimFromPool(id) {
    for (let a of this.anims[id]) {
      if (!a.busyp) {
        a.busyp = true;
        return a;
      }
    }

    let a = this.createAnim(id);
    this.anims[id].push(a);
    a.busyp = true;
    return a;
  };

  createAnim(id) {
    const sprite = new PIXI.Sprite(PIXI.Texture.EMPTY);
    if (ANCHORS[id]) {
      sprite.anchor.copy(ANCHORS[id]);
    }
    this.container.addChild(sprite);
    return sprite;
  }

  reinitScene(container) {
    this.container = container;
    for (let a of this.animData) {
      a.sprite = null;
    }
    for (let key in FRAMES) {
      this.anims[key] = [];
    }
  }

  animateScene(delta) {
  }

  handleGlobalData(players, globalData) {
  }

}