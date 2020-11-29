package com.kingyu.rlbird.game.component;

import com.kingyu.rlbird.game.FlappyBird;

/**
 * 记分类, 单例类
 *
 * @author Kingyu
 */
public class ScoreCounter {
    private static final ScoreCounter scoreCounter = new ScoreCounter();

    private long score = 0;

    private ScoreCounter() {
    }

    public static ScoreCounter getInstance() {
        return scoreCounter;
    }

    public void score(Bird bird) {
        if (!bird.isDead()) {
            FlappyBird.setCurrentReward(1f);
            score += 1;
        }
    }

    public long getCurrentScore() {
        return score;
    }

    public void reset() {
        score = 0;
    }

}
