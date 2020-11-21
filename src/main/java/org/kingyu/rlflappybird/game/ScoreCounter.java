package org.kingyu.rlflappybird.game;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.kingyu.rlflappybird.util.Constant;
//import org.bird.util.MusicUtil;

/**
 * 记分类, 单例类
 *
 * @author Kingyu
 */
public class ScoreCounter {
    private static final ScoreCounter scoreCounter = new ScoreCounter();

    private long score = 0; // 分数
    private long bestScore; // 最高分数

    private ScoreCounter() {
        bestScore = -1;
        try {
            loadBestScore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ScoreCounter getInstance() {
        return scoreCounter;
    }

    // 装载最高纪录
    private void loadBestScore() throws Exception {
        File file = new File(Constant.SCORE_FILE_PATH);
        if (file.exists()) {
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            bestScore = dis.readLong();
            dis.close();
        }
    }

    // 保存最高纪录
    public void saveBestScore(long score) throws Exception {
        File file = new File(Constant.SCORE_FILE_PATH);
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
        dos.writeLong(score);
        dos.close();
    }

    public long getBestScore() {
        return bestScore;
    }

    public long getCurrentScore() {
        return score;
    }

    public void score(Bird bird) {
        if (!bird.isDead()) {
//			MusicUtil.playScore();
            Game.setCurrentReward(1f);
            score += 1;
        }
    }

    // 判断是否为最高纪录
    public void isSaveScore() {
        long score = getCurrentScore();
        if (bestScore < score)
            bestScore = score;
        try {
            saveBestScore(bestScore);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //重置计分器
    public void reset() {
        score = 0;
    }

}
