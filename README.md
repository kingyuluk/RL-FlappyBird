# RL Flappy Bird
![](https://img.shields.io/badge/JDK-1.8.0-a7742f.svg)
![](https://img.shields.io/badge/platform-MacOS%20%7C%20Linux%20%7C%20Windows-yellow.svg)
![](https://img.shields.io/github/license/kingyuluk/FlappyBird)
![](https://img.shields.io/github/v/release/kingyuluk/FlappyBird)
![](https://img.shields.io/github/repo-size/kingyuluk/FlappyBird?color=ff69b4)

Training Flappy Bird Using Reinforcement Learning.

## Overview
DQN implemented based on DJL.

## Build the project and run
This project integrates DJL by using Maven. 
```
mvn compile  
mvn exec:java -Dexec.mainClass="com.kingyu.rlbird.ai.TrainBird"
```
You can also run with arguments.

| Argument   | Comments                                 |
 | ---------- | ---------------------------------------- |
 | `-m`       | Training mode: <br>0: Training without UI, fastest training mode<br>1: Normal training mode with UI
 | `-b`       | Batch size to use for training. |
 | `-p`       | Use pre-trained weights. |


## Notes

The default mode is without graphics.

## Package Contents
* org.kingyu.rlbird.ai    

* org.kingyu.rlbird.game

* org.kingyu.rlbird.rl  

* org.kingyu.rlbird.util   

## License
[MIT](License) © Kingyu Luk
