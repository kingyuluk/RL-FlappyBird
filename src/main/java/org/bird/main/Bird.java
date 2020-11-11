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
    private final int x;
    private int y; // 小鸟的坐标

    // 图片资源
    private BufferedImage image; // 实时的小鸟图片

    // 小鸟的状态
    private int state;
    public static final int BIRD_READY = 0;
    public static final int BIRD_FALL = 3;
    public static final int BIRD_DEAD = 4;

    private final Rectangle birdRect; // 碰撞矩形
    public static final int RECT_DESCALE = 2; // 补偿碰撞矩形宽高的参数

    private final ScoreCounter counter; // 计分器

    static BufferedImage birdImages; // 小鸟图片，static保证图片只加载一次

    public static final int BIRD_WIDTH;
    public static final int BIRD_HEIGHT;

    static {
        birdImages = GameUtil.loadBufferedImage(Constant.BIRDS_IMG_PATH[0][0]);  // 读取图片资源
        assert birdImages != null;
        BIRD_WIDTH = birdImages.getWidth();
        BIRD_HEIGHT = birdImages.getHeight();
    }

    public Bird() {
        counter = ScoreCounter.getInstance(); // 计分器
        // 初始化小鸟坐标
        x = Constant.FRAME_WIDTH >> 2;
        y = Constant.FRAME_HEIGHT >> 1;

        // 初始化碰撞矩形
        int rectX = x - (BIRD_WIDTH >> 1);
        int rectY = y - (BIRD_HEIGHT >> 1) + 4; // 位置补偿
        birdRect = new Rectangle(rectX + RECT_DESCALE, rectY + RECT_DESCALE * 2, BIRD_WIDTH - RECT_DESCALE * 3,
                BIRD_HEIGHT - RECT_DESCALE * 4); // 碰撞矩形的坐标与小鸟相同
    }

    // 小鸟中心点计算
    int halfImgWidth = birdImages.getWidth() >> 1;
    int halfImgHeight = birdImages.getHeight() >> 1;

    // 绘制方法
    public void draw(Graphics g) {
        movement();
        g.drawImage(birdImages, x - halfImgWidth, y - halfImgHeight, null); // x坐标于窗口1/4处，y坐标位窗口中心
//        if (state != BIRD_FALL && state != BIRD_DEAD)
//            drawScore(g);
//      // 绘制矩形
//        g.setColor(Color.white);
//        g.drawRect((int) birdRect.getX(), (int) birdRect.getY(), (int) birdRect.getWidth(), (int) birdRect.getHeight());
    }

    public void drawBirdImg(Graphics g) {
        g.drawImage(birdImages, x - halfImgWidth, y - halfImgHeight, null);
    }

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

    public static final int ACC_FLAP = 7; // players speed on flapping
    public static final double ACC_Y = 1; // players downward acceleration
    public static final int MAX_VEL_Y = 10; // max vel along Y, max descend speed
    public static final int BOTTOM_BOUNDARY = Constant.FRAME_HEIGHT - Constant.GROUND_HEIGHT - (birdImages.getHeight() >> 1);
    public static final int TOP_BOUNDARY = 30;

    private int velocity = 0; // bird's velocity along Y, default same as playerFlapped

    // bird's movement
    private void movement() {
        // bottom boundary

        keyReleased();
        if (velocity < MAX_VEL_Y)
            velocity -= ACC_Y;
        y = Math.min((y - velocity), BOTTOM_BOUNDARY);
        birdRect.y = birdRect.y - velocity;
        if (birdRect.y >= BOTTOM_BOUNDARY - 10) {
//                    MusicUtil.playCrash();
            GameFrame.setCurrentReward(-1f);
            GameFrame.setCurrentTerminal(true);
            GameFrame.setGameState(GameFrame.GAME_OVER);
        }
        if (birdRect.y < TOP_BOUNDARY) {
            GameFrame.setCurrentReward(-1f);
            GameFrame.setCurrentTerminal(true);
            GameFrame.setGameState(GameFrame.GAME_OVER);
            birdRect.y = TOP_BOUNDARY;
            y = TOP_BOUNDARY;
        }

    }

    public void birdFlap() {
        if (keyIsReleased()) {
            keyPressed();
            if (state == BIRD_DEAD || state == BIRD_FALL)
                return;
//            MusicUtil.playFly(); // 播放音效
            velocity = ACC_FLAP; // 每次振翅将速度改为上升速度
        }
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
        velocity = 0; // 小鸟速度

        int ImgHeight = birdImages.getHeight();
        birdRect.y = y + 4 - ImgHeight / 2 + RECT_DESCALE * 2; // 小鸟碰撞矩形坐标

        counter.reset(); // 重置计分器
    }

    public long getCurrentScore() {
        return counter.getCurrentScore();
    }

    public long getBestScore() {
        return counter.getBestScore();
    }

    public int getBirdX() {
        return x;
    }

    // 获取小鸟的碰撞矩形
    public Rectangle getBirdRect() {
        return birdRect;
    }
}
