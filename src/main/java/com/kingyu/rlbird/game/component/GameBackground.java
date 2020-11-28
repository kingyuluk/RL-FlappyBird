package com.kingyu.rlbird.game.component;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import com.kingyu.rlbird.util.Constant;
import com.kingyu.rlbird.util.GameUtil;

/**
 *
 * @author Kingyu
 */
public class GameBackground {

	private static final BufferedImage BackgroundImg;

	private final int velocity;
	private int layerX;

	public GameBackground() {
		this.velocity = Constant.GAME_SPEED;
		this.layerX = 0;
	}
	public static final int GROUND_HEIGHT;

	static {
		BackgroundImg = GameUtil.loadBufferedImage(Constant.BG_IMG_PATH);
		assert BackgroundImg != null;
		GROUND_HEIGHT = BackgroundImg.getHeight();
	}

	public void draw(Graphics g, Bird bird) {
		if (bird.isDead()) {
			return;
		}
		g.setColor(Constant.BG_COLOR);
		g.fillRect(0, 0, Constant.FRAME_WIDTH, Constant.FRAME_HEIGHT);
		int imgWidth = BackgroundImg.getWidth();
		int count = Constant.FRAME_WIDTH / imgWidth + 2; // 根据窗口宽度得到图片的绘制次数
		for (int i = 0; i < count; i++) {
			g.drawImage(BackgroundImg, imgWidth * i - layerX, Constant.FRAME_HEIGHT - GROUND_HEIGHT, null);
		}
		movement();
	}

	private void movement() {
		layerX += velocity;
		if (layerX > BackgroundImg.getWidth())
			layerX = 0;
	}
}
