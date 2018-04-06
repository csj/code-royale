// import { EntityManager } from './EntityManager.js';
//
// // You can define resources used by your games (like images, fonts, ...)
// export const assets = {
//   images: {
//     // ball: '/assets/ball.png',
//     // background: '/assets/background.jpg'
//   }
// };
//
// export const players = [{
//     name: 'Player 1',
//     avatar: 'https://static.codingame.com/servlet/fileservlet?id=1719285195844&format=viewer_avatar'
//   },
//   {
//     name: 'Player 2',
//     avatar: 'https://static.codingame.com/servlet/fileservlet?id=1717001354716&format=viewer_avatar'
//   }
// ];
//
// // List of viewer modules that you want to use in your game
// export const modules = [
//   {name: 'entitymanager', class: EntityManager}
// ];


import { GraphicEntityModule } from './entity-module/GraphicEntityModule.js';
import {TooltipModule} from './tooltips/TooltipModule.js';
import {AnimationModule} from './animationModule/AnimationModule.js';

// List of viewer modules that you want to use in your game
export const modules = [
    GraphicEntityModule,
    TooltipModule,
    AnimationModule
];
