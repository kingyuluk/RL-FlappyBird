# RL Flappy Bird
![](https://img.shields.io/badge/JDK-1.8.0-a7742f.svg)
![](https://img.shields.io/badge/platform-MacOS%20%7C%20Linux%20%7C%20Windows-yellow.svg)
![](https://img.shields.io/github/license/kingyuluk/FlappyBird)
![](https://img.shields.io/github/v/release/kingyuluk/FlappyBird)
![](https://img.shields.io/github/repo-size/kingyuluk/FlappyBird?color=ff69b4)

Training Flappy Bird Using Reinforcement Learning.

## Overview
DQN implemented based on DJL.

## How to run
This project integrates DJL by using Maven. 

```
mvn compile  
mvn exec:java -Dexec.mainClass="com.kingyu.rlbird.ai.TrainBird"
```

## Notes

The default mode is without graphics.

The graphical interface can be opened in ai/TrainBird.java on Line 54.

## Package Contents
* org.kingyu.rlbird.ai    

* org.kingyu.rlbird.game

* org.kingyu.rlbird.rl  

* org.kingyu.rlbird.util   

## License
[MIT](License) © Kingyu Luk
