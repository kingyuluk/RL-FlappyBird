package com.kingyu.rlflappybird.game;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import com.kingyu.rlflappybird.util.Constant;
import com.kingyu.rlflappybird.util.GameUtil;

/**
 * 游戏元素层
 *
 * @author Kingyu
 */

public class GameElementLayer {
    private final List<Pipe> pipes; // 水管的容器

    public GameElementLayer() {
        pipes = new ArrayList<>();
    }

    public void draw(Graphics g, Bird bird) {
        // 遍历水管容器，如果可见则绘制，不可见则归还
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            if (pipe.isVisible()) {
                pipe.draw(g, bird);
            } else {
                Pipe remove = pipes.remove(i);
                PipePool.giveBack(remove);
                i--;
            }
        }
        bird.drawBirdImg(g);
        isCollideBird(bird);
        generatePipe(bird);
    }

    public static final int VERTICAL_INTERVAL = Constant.FRAME_HEIGHT / 3;
    public static final int HORIZONTAL_INTERVAL = Constant.FRAME_HEIGHT >> 2;
    public static final int MIN_HEIGHT = Constant.FRAME_HEIGHT / 5;
    public static final int MAX_HEIGHT = Constant.FRAME_HEIGHT / 4;

    /**
     * 当容器中添加的最后一个水管完全显示到屏幕后，添加下一对；
     */
    private void generatePipe(Bird bird) {
        if (bird.isDead()) {
            return;
        }
        if (pipes.size() == 0) {
            // 若容器为空，则添加一对水管
            int topHeight = GameUtil.getRandomNumber(MIN_HEIGHT, MAX_HEIGHT + 1); // 随机生成水管高度

            Pipe top = PipePool.get("Pipe");
            top.setAttribute(Constant.FRAME_WIDTH, -Pipe.TOP_PIPE_LENGTHENING,
                    topHeight + Pipe.TOP_PIPE_LENGTHENING, Pipe.TYPE_TOP_NORMAL, true);

            Pipe bottom = PipePool.get("Pipe");
            bottom.setAttribute(Constant.FRAME_WIDTH, topHeight + VERTICAL_INTERVAL,
                    Constant.FRAME_HEIGHT - topHeight - VERTICAL_INTERVAL, Pipe.TYPE_BOTTOM_NORMAL, true);

            pipes.add(top);
            pipes.add(bottom);
        } else {
            // 判断最后一对水管是否完全进入游戏窗口，若进入则添加水管
            Pipe lastPipe = pipes.get(pipes.size() - 1); // 获得容器中最后一个水管
            int currentDistance = lastPipe.getX() - bird.getBirdX() + Bird.BIRD_WIDTH / 2; // 小鸟和最后一根水管的距离
            final int SCORE_DISTANCE = Pipe.PIPE_WIDTH * 2 + HORIZONTAL_INTERVAL; // 小于得分距离则得分
            if (pipes.size() >= PipePool.FULL_PIPE
                    && currentDistance <= 275
                    && currentDistance > 270) {
                Game.setCurrentReward(0.5f);
            }
            if (pipes.size() >= PipePool.FULL_PIPE
                    && currentDistance <= SCORE_DISTANCE
                    && currentDistance > SCORE_DISTANCE - Constant.GAME_SPEED) {
                ScoreCounter.getInstance().score(bird);
            }
            if (lastPipe.isInFrame()) {
                addNormalPipe(lastPipe);
            }
        }
    }

    /**
     * 添加普通水管
     *
     * @param lastPipe 最后一根水管
     */
    private void addNormalPipe(Pipe lastPipe) {
        int topHeight = GameUtil.getRandomNumber(MIN_HEIGHT, MAX_HEIGHT + 1); // 随机生成水管高度
        int x = lastPipe.getX() + HORIZONTAL_INTERVAL; // 新水管的x坐标 = 最后一对水管的x坐标 + 水管的间隔

        Pipe top = PipePool.get("Pipe"); // 从水管对象池中获取对象

        // 设置x, y, height, type属性
        top.setAttribute(x, -Pipe.TOP_PIPE_LENGTHENING, topHeight + Pipe.TOP_PIPE_LENGTHENING,
                Pipe.TYPE_TOP_NORMAL, true);

        Pipe bottom = PipePool.get("Pipe");
        bottom.setAttribute(x, topHeight + VERTICAL_INTERVAL, Constant.FRAME_HEIGHT - topHeight - VERTICAL_INTERVAL,
                Pipe.TYPE_BOTTOM_NORMAL, true);

        pipes.add(top);
        pipes.add(bottom);
    }

    /**
     * 判断元素和小鸟是否发生碰撞
     *
     * @param bird 小鸟对象
     */
    public void isCollideBird(Bird bird) {
        if (bird.isDead()) {
            return;
        }
        for (Pipe pipe : pipes) {
            if (pipe.getPipeRect().intersects(bird.getBirdRect())) {
                bird.die();
                return;
            }
        }
    }

    public void reset() {
        for (Pipe pipe : pipes) {
            PipePool.giveBack(pipe);
        }
        pipes.clear();
    }
}
