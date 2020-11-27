package com.kingyu.rlbird.game.content;

import java.awt.*;
import java.awt.image.BufferedImage;

import com.kingyu.rlbird.game.FlappyBird;
import com.kingyu.rlbird.util.Constant;
import com.kingyu.rlbird.util.GameUtil;
//import org.bird.util.MusicUtil;


/**
 * 小鸟类，小鸟的绘制与飞行逻辑都在此类
 *
 * @author Kingyu
 */
public class Bird {
    private final int x;
    private int y;

    // 小鸟的状态
    private int birdState;
    public static final int BIRD_READY = 0;
    public static final int BIRD_FALL = 1;
    public static final int BIRD_DEAD = 2;

    private final Rectangle birdCollisionRect;
    public static final int RECT_DESCALE = 2; // 碰撞矩形宽高的补偿参数

    private final ScoreCounter scoreCounter;
    static BufferedImage birdImages;

    public static final int BIRD_WIDTH;
    public static final int BIRD_HEIGHT;

    static {
        birdImages = GameUtil.loadBufferedImage(Constant.BIRDS_IMG_PATH);
        assert birdImages != null;
        BIRD_WIDTH = birdImages.getWidth();
        BIRD_HEIGHT = birdImages.getHeight();
    }

    public Bird() {
        scoreCounter = ScoreCounter.getInstance();
        x = Constant.FRAME_WIDTH >> 2;
        y = Constant.FRAME_HEIGHT >> 1;

        int rectX = x - (BIRD_WIDTH >> 1);
        int rectY = y - (BIRD_HEIGHT >> 1) + RECT_DESCALE * 2;
        birdCollisionRect = new Rectangle(rectX + RECT_DESCALE, rectY + RECT_DESCALE * 2, BIRD_WIDTH - RECT_DESCALE * 3,
                BIRD_HEIGHT - RECT_DESCALE * 4); // 碰撞矩形的坐标与小鸟相同
    }

    public void draw(Graphics g) {
        movement();
        drawBirdImg(g);
//        g.setColor(Color.white);
//        g.drawRect((int) birdCollisionRect.getX(), (int)birdCollisionRect.getY(), (int) birdCollisionRect.getWidth(), (int) birdCollisionRect.getHeight());
    }

    public void drawBirdImg(Graphics g) {
        g.drawImage(birdImages, x - (BIRD_WIDTH >> 1), y - (BIRD_HEIGHT >> 1), null);
    }

    public static final int ACC_FLAP = 15; // players speed on flapping
    public static final double ACC_Y = 4; // players downward acceleration
    public static final int MAX_VEL_Y = -25; // max vel along Y, max descend speed
    public static final int BOTTOM_BOUNDARY = Constant.FRAME_HEIGHT - GameBackground.GROUND_HEIGHT - (BIRD_HEIGHT >> 1);

    private int velocity = 0; // bird's velocity along Y, default same as playerFlapped

    private void movement() {
//        keyReleased();
        if (velocity > MAX_VEL_Y)
            velocity -= ACC_Y;
        y = Math.min((y - velocity), BOTTOM_BOUNDARY);
        birdCollisionRect.y = birdCollisionRect.y - velocity;
        if (birdCollisionRect.y < GameElementLayer.MIN_HEIGHT ||
                birdCollisionRect.y > Constant.FRAME_HEIGHT - GameBackground.GROUND_HEIGHT - GameElementLayer.MIN_HEIGHT) {
            FlappyBird.setCurrentReward(0.1f);
        }
        if (birdCollisionRect.y < Constant.WINDOW_BAR_HEIGHT) {
            die();
        }
        if (birdCollisionRect.y >= BOTTOM_BOUNDARY - 10) {
//                    MusicUtil.playCrash();
            die();
        }
    }

    public void birdFlap() {
//        if (keyIsReleased()) {
//            keyPressed();
        if (isDead())
            return;
//            MusicUtil.playFly();
        velocity = ACC_FLAP;
//        }
    }

    public void die() {
        FlappyBird.setCurrentReward(-1f);
        FlappyBird.setCurrentTerminal(true);
        FlappyBird.setGameState(FlappyBird.GAME_OVER);
        birdState = BIRD_DEAD;
    }

    public boolean isDead() {
        return birdState == BIRD_FALL || birdState == BIRD_DEAD;
    }

    public void reset() {
        birdState = BIRD_READY;
        y = Constant.FRAME_HEIGHT >> 1;
        velocity = 0;
        int ImgHeight = birdImages.getHeight();
        birdCollisionRect.y = y + RECT_DESCALE * 4 - ImgHeight / 2;
        scoreCounter.reset();
    }

    public long getCurrentScore() {
        return scoreCounter.getCurrentScore();
    }

    public int getBirdX() {
        return x;
    }

    public Rectangle getBirdCollisionRect() {
        return birdCollisionRect;
    }

    // private boolean keyRelease = true; // 按键状态
//
//    private void keyPressed() {
//        keyRelease = false;
//    }
//
//    private void keyReleased() {
//        keyRelease = true;
//    }
//
//    private boolean keyIsReleased() {
//        return keyRelease;
//    }
}
