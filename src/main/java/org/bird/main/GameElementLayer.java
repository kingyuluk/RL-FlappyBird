package org.bird.main;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import org.bird.util.Constant;
import org.bird.util.GameUtil;

import static org.bird.main.Bird.BIRD_WIDTH;
import static org.bird.main.Pipe.PIPE_WIDTH;
import static org.bird.util.Constant.GAME_SPEED;

/**
 * 游戏元素层
 *
 * @author Kingyu
 */

public class GameElementLayer {
    private final List<Pipe> pipes; // 水管的容器

    // 构造器
    public GameElementLayer() {
        pipes = new ArrayList<>();
    }

    // 绘制方法
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
        // 碰撞检测
        isCollideBird(bird);
        pipeBornLogic(bird);
    }

    /**
     * 添加水管的逻辑： 当容器中添加的最后一个元素完全显示到屏幕后，添加下一对； 水管成对地相对地出现，空隙高度为窗口高度的1/4；
     * 每对水管的间隔距离为屏幕高度的1/4； 水管的高度的取值范围为窗口的[1/8~5/8]
     */
    public static final int VERTICAL_INTERVAL = Constant.FRAME_HEIGHT / 4;
    public static final int HORIZONTAL_INTERVAL = Constant.FRAME_HEIGHT >> 2;
    public static final int MIN_HEIGHT = Constant.FRAME_HEIGHT >> 3;
    public static final int MAX_HEIGHT = ((Constant.FRAME_HEIGHT) >> 3) * 5;

    private void pipeBornLogic(Bird bird) {
        if (bird.isDead()) {
            // 鸟死后不再添加水管
            return;
        }
        if (pipes.size() == 0) {
            // 若容器为空，则添加一对水管
            int topHeight = GameUtil.getRandomNumber(MIN_HEIGHT, MAX_HEIGHT + 1); // 随机生成水管高度

            Pipe top = PipePool.get("Pipe");
            top.setAttribute(Constant.FRAME_WIDTH, -Constant.TOP_PIPE_LENGTHENING,
                    topHeight + Constant.TOP_PIPE_LENGTHENING, Pipe.TYPE_TOP_NORMAL, true);

            Pipe bottom = PipePool.get("Pipe");
            bottom.setAttribute(Constant.FRAME_WIDTH, topHeight + VERTICAL_INTERVAL,
                    Constant.FRAME_HEIGHT - topHeight - VERTICAL_INTERVAL, Pipe.TYPE_BOTTOM_NORMAL, true);

            pipes.add(top);
            pipes.add(bottom);
        } else {
            // 判断最后一对水管是否完全进入游戏窗口，若进入则添加水管
            Pipe lastPipe = pipes.get(pipes.size() - 1); // 获得容器中最后一个水管
            int currentDistance = lastPipe.getX() - bird.getBirdX() + BIRD_WIDTH / 2; // 小鸟和最后一根水管的距离
            final int SCORE_DISTANCE = PIPE_WIDTH * 2 + PIPE_WIDTH + HORIZONTAL_INTERVAL * 2; // 小于得分距离则得分
            if (pipes.size() >= PipePool.FULL_PIPE
                    && currentDistance < SCORE_DISTANCE
                    && currentDistance > SCORE_DISTANCE - GAME_SPEED) {
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
     * @param lastPipe 最后一根水管以获取x坐标
     */
    private void addNormalPipe(Pipe lastPipe) {
        int topHeight = GameUtil.getRandomNumber(MIN_HEIGHT, MAX_HEIGHT + 1); // 随机生成水管高度
        int x = lastPipe.getX() + HORIZONTAL_INTERVAL; // 新水管的x坐标 = 最后一对水管的x坐标 + 水管的间隔

        Pipe top = PipePool.get("Pipe"); // 从水管对象池中获取对象

        // 设置x, y, height, type属性
        top.setAttribute(x, -Constant.TOP_PIPE_LENGTHENING, topHeight + Constant.TOP_PIPE_LENGTHENING,
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
        // 若鸟已死则不再判断
        if (bird.isDead()) {
            return;
        }
        // 遍历水管容器
        for (Pipe pipe : pipes) {
            // 判断碰撞矩形是否有交集
            if (pipe.getPipeRect().intersects(bird.getBirdRect())) {
                GameFrame.setCurrentReward(-1f);
                GameFrame.setCurrentTerminal(true);
                GameFrame.setGameState(GameFrame.GAME_OVER);
//                bird.deadBirdFall();
                return;
            }
        }
    }

    // 重置元素层
    public void reset() {
        for (Pipe pipe : pipes) {
            PipePool.giveBack(pipe);
        }
        pipes.clear();
    }
}
