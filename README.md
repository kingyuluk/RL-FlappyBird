# RL Flappy Bird
![](https://img.shields.io/badge/Framework-DJL-lightgrey)
![](https://img.shields.io/badge/Engine-MXNet-blue)
![](https://img.shields.io/badge/JDK-1.8.0-a7742f.svg)
![](https://img.shields.io/github/license/kingyuluk/RL-FlappyBird)
![](https://img.shields.io/github/repo-size/kingyuluk/RL-FlappyBird?color=ff69b4)


## Overview

This project is a basic application of Reinforcement Learning.

It uses Deep Q-Network to train the agent to play by integrating [Deep Java Library (DJL)](https://github.com/awslabs/djl).

## Build the project and run
This project supports building with Maven, you can use the following command to build: 
```
mvn compile  
```

The following command will start to train without graphics:
```
mvn exec:java -Dexec.mainClass="com.kingyu.rlbird.ai.TrainBird"
```

You can also run with arguments.

| Argument   | Comments                                 |
 | ---------- | ---------------------------------------- |
 | `-m`       | Training mode: <br>0: Training without UI, fastest training mode<br>1: Normal training mode with UI
 | `-b`       | Batch size to use for training. |
 | `-p`       | Use pre-trained weights. |

## Deep Q-Network Algorithm

The pseudo-code for the Deep Q Learning algorithm, as given in [Human-level Control through Deep Reinforcement Learning. Nature](https://www.nature.com/articles/nature14236), can be found below:
```
Initialize replay memory D to size N
Initialize action-value function Q with random weights
for episode = 1, M do
    Initialize state s_1
    for t = 1, T do
        With probability ϵ select random action a_t
        otherwise select a_t=max_a  Q(s_t,a; θ_i)
        Execute action a_t in emulator and observe r_t and s_(t+1)
        Store transition (s_t,a_t,r_t,s_(t+1)) in D
        Sample a minibatch of transitions (s_j,a_j,r_j,s_(j+1)) from D
        Set y_j:=
            r_j for terminal s_(j+1)
            r_j+γ*max_(a^' )  Q(s_(j+1),a'; θ_i) for non-terminal s_(j+1)
        Perform a gradient step on (y_j-Q(s_j,a_j; θ_i))^2 with respect to θ
    end for
end for
```

## Notes

Troubleshooting

* [X11 error](https://github.com/aws-samples/d2l-java/blob/master/documentation/troubleshoot.md#1-x11-error-when-running-object-detection-notebooks-on-ec2-instances)

This work is based on the following repos:

* [uvipen/Flappy-bird-deep-Q-learning-pytorch](https://github.com/uvipen/Flappy-bird-deep-Q-learning-pytorch)

* [yenchenlin/DeepLearningFlappyBird](https://github.com/yenchenlin/DeepLearningFlappyBird)

## License
[MIT](License) © Kingyu Luk
