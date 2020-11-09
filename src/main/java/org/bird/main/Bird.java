package org.bird.main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.bird.util.Constant;
import org.bird.util.GameUtil;
//import org.bird.util.MusicUtil;


/**
 * 小鸟类，小鸟的绘制与飞行逻辑都在此类
 *
 * @author Kingyu
 */
public class Bird {
    public static final int IMG_COUNT = 1; // 图片数量
    public static final int FLY_STATE = 4; // 飞行形态数量
    private final BufferedImage birdImages; // 小鸟的图片数组对象
    private final int x;
    private int y; // 小鸟的坐标

    // 图片资源
    private BufferedImage image; // 实时的小鸟图片

    // 小鸟的状态
    private int state;
    public static final int BIRD_READY = 0;
    public static final int BIRD_UP = 1;
    public static final int BIRD_DOWN = 2;
    public static final int BIRD_FALL = 3;
    public static final int BIRD_DEAD = 4;

    private final Rectangle birdRect; // 碰撞矩形
    public static final int RECT_DESCALE = 2; // 补偿碰撞矩形宽高的参数

    private final ScoreCounter counter; // 计分器
    public static final int FLAP_ACC = 40; // 小鸟向上的速度
    public static final double DOWN_ACC = 16; // 重力加速度
    public static final double T = 0.2; // 小鸟的下落函数执行的时间

    public Bird() {
        counter = ScoreCounter.getInstance(); // 计分器
        // 读取图片资源
        birdImages = GameUtil.loadBufferedImage(Constant.BIRDS_IMG_PATH[0][0]);

        // 初始化小鸟坐标
        x = Constant.FRAME_WIDTH >> 2;
        y = Constant.FRAME_HEIGHT >> 1;

        int ImgWidth = birdImages.getWidth();
        int ImgHeight = birdImages.getHeight();

        // 初始化碰撞矩形
        int rectX = x - ImgWidth / 2;
        int rectY = y - ImgHeight / 2;
        birdRect = new Rectangle(rectX + RECT_DESCALE, rectY + RECT_DESCALE * 2, ImgWidth - RECT_DESCALE * 3,
                ImgHeight - RECT_DESCALE * 4); // 碰撞矩形的坐标与小鸟相同
    }

    // 绘制方法
    public void draw(Graphics g) {
        fly();
        // 小鸟中心点计算
        int halfImgWidth = birdImages.getWidth() >> 1;
        int halfImgHeight = birdImages.getHeight() >> 1;

        g.drawImage(birdImages, x - halfImgWidth, y - halfImgHeight, null); // x坐标于窗口1/4处，y坐标位窗口中心

        if (state != BIRD_FALL && state != BIRD_DEAD)
            drawScore(g);
//      // 绘制矩形
//      g.setColor(Color.black);
//      g.drawRect((int) birdRect.getX(), (int) birdRect.getY(), (int) birdRect.getWidth(), (int) birdRect.getHeight());
    }

    private double speed = 0; // 小鸟的初速度

    private boolean keyRelease = true; // 按键状态

    private void keyPressed() {
        keyRelease = false;
    }

    private void keyReleased() {
        keyRelease = true;
    }

    private boolean keyIsReleased() {
        return keyRelease;
    }

    // 小鸟的飞行逻辑
    private void fly() {
        // 下方边界: 窗口的高度 - 地面的高度 - 小鸟图片的高度
        final int bottomBoundary = Constant.FRAME_HEIGHT - Constant.GROUND_HEIGHT - (birdImages.getHeight() >> 1);
        final int topBoundary = -50;

        switch (state) {
            case BIRD_DOWN:
                // 自由落体
                keyReleased();
                speed -= DOWN_ACC * T;
                double h = speed * T - DOWN_ACC * T * T / 2;
                y = Math.min((int) (y - h), bottomBoundary);
                birdRect.y = Math.min((int) (birdRect.y - h), bottomBoundary);
                if (birdRect.y >= bottomBoundary) {
//                    MusicUtil.playCrash();
                    GameFrame.setGameState(GameFrame.GAME_OVER);
                }
                break;

            case BIRD_FALL:
                // 自由落体
                speed -= DOWN_ACC * T;
                h = speed * T - DOWN_ACC * T * T / 2;
                y = Math.min((int) (y - h), bottomBoundary);
                birdRect.y = Math.min((int) (birdRect.y - h), bottomBoundary);
                if (birdRect.y >= bottomBoundary) {
                    GameFrame.setGameState(GameFrame.GAME_OVER);
                }
                break;

            case BIRD_DEAD:
                GameFrame.setGameState(GameFrame.GAME_OVER);
                break;
        }

        // 控制上方边界
        if (birdRect.y < topBoundary) {
            birdRect.y = topBoundary;
            y = topBoundary;
        }

    }

    // 小鸟振翅
    public void birdFlap() {
        if (keyIsReleased()) {
            keyPressed();
            if (state == BIRD_DEAD || state == BIRD_FALL)
                return;
//            MusicUtil.playFly(); // 播放音效
            speed = FLAP_ACC; // 每次振翅将速度改为上升速度
        }
    }

    // 小鸟下降
    public void birdFall() {
        if (state == BIRD_DEAD || state == BIRD_FALL)
            return;
        state = BIRD_DOWN;
    }

    // 小鸟坠落（已死）
    public void deadBirdFall() {
        if (GameFrame.getGameMode() == GameFrame.RL_MODE) {
            GameFrame.setCurrentTerminal(true);
            GameFrame.setCurrentReward(-1f);
            state = BIRD_DEAD;
            return;
        }
        state = BIRD_FALL;
//        MusicUtil.playCrash(); // 播放音效
        speed = 0;  // 速度置0，防止小鸟继续上升与水管重叠
    }

    // 小鸟死亡
    public void birdDead() {
        GameFrame.setCurrentTerminal(true);
        GameFrame.setCurrentReward(-1f);
        state = BIRD_DEAD;
        counter.isSaveScore(); // 判断是否保存纪录
    }

    // 判断小鸟是否死亡
    public boolean isDead() {
        return state == BIRD_FALL || state == BIRD_DEAD;
    }

    // 绘制实时分数
    private void drawScore(Graphics g) {
        g.setColor(Color.white);
        g.setFont(Constant.CURRENT_SCORE_FONT);
        String str = Long.toString(counter.getCurrentScore());
        int x = Constant.FRAME_WIDTH - GameUtil.getStringWidth(Constant.CURRENT_SCORE_FONT, str) >> 1;
        g.drawString(str, x, Constant.FRAME_HEIGHT / 10);
    }

    // 重置小鸟
    public void reset() {
        state = BIRD_READY; // 小鸟状态
        y = Constant.FRAME_HEIGHT >> 1; // 小鸟坐标
        speed = 0; // 小鸟速度

        int ImgHeight = birdImages.getHeight();
        birdRect.y = y - ImgHeight / 2 + RECT_DESCALE * 2; // 小鸟碰撞矩形坐标

        counter.reset(); // 重置计分器
    }

    public long getCurrentScore() {
        return counter.getCurrentScore();
    }

    public long getBestScore() {
        return counter.getBestScore();
    }

    // 获取小鸟的碰撞矩形
    public Rectangle getBirdRect() {
        return birdRect;
    }
}
