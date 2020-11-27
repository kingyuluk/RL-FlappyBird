package com.kingyu.rlbird.game.content;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

import com.kingyu.rlbird.game.FlappyBird;
import com.kingyu.rlbird.util.Constant;
//import org.bird.util.MusicUtil;

/**
 * 记分类, 单例类
 *
 * @author Kingyu
 */
public class ScoreCounter {
    private static final ScoreCounter scoreCounter = new ScoreCounter();

    private long score = 0;
    private long bestScore = -1;

    private ScoreCounter() {
        try {
            loadBestScore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ScoreCounter getInstance() {
        return scoreCounter;
    }

    private void loadBestScore() throws Exception {
        File file = new File(Constant.SCORE_FILE_PATH);
        if (file.exists()) {
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            bestScore = dis.readLong();
            dis.close();
        }
    }

    public void score(Bird bird) {
        if (!bird.isDead()) {
//			MusicUtil.playScore();
            FlappyBird.setCurrentReward(1f);
            score += 1;
        }
    }

    public long getBestScore() {
        return bestScore;
    }

    public long getCurrentScore() {
        return score;
    }

    public void reset() {
        score = 0;
    }

}
