package org.kingyu.rlflappybird.game;

import ai.djl.training.Trainer;
import org.kingyu.rlflappybird.rl.ActionSpace;
import org.kingyu.rlflappybird.rl.LruReplayBuffer;
import org.kingyu.rlflappybird.rl.ReplayBuffer;
import org.kingyu.rlflappybird.rl.agent.RlAgent;
import org.kingyu.rlflappybird.rl.env.RlEnv;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import org.kingyu.rlflappybird.util.GameUtil;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.*;

import static org.kingyu.rlflappybird.ai.TrainBird.EXPLORE;
import static org.kingyu.rlflappybird.ai.TrainBird.OBSERVE;
import static org.kingyu.rlflappybird.util.Constant.*;

/**
 * 主窗口类，游戏窗口和绘制的相关内容
 *
 * @author Kingyu
 */

public class Game extends Frame implements Runnable, RlEnv {
    private static final long serialVersionUID = 1L; // 保持版本的兼容性

    private final NDManager subManager = NDManager.newBaseManager();

    private static int gameMode;
    public static final int NORMAL_MODE = 0;
    public static final int NOUI_MODE = 1;
    public static final int UI_MODE = 2;

    private static int gameState; // 游戏状态
    public static final int GAME_READY = 0; // 游戏未开始
    public static final int GAME_START = 1; // 游戏开始
    public static final int GAME_OVER = 2; // 游戏结束

    private GameBackground background; // 游戏背景
    private Bird bird; // 小鸟
    private GameElementLayer gameElement; // 游戏元素
    private WelcomeAnimation welcomeAnimation; // 欢迎界面
    private GameOverAnimation gameoverAnimation; //结束界面

    public static boolean birdFlapped = false;

    /**
     * input_actions[0] == 1: do nothing
     * input_actions[1] == 1: flap the bird
     */
    private NDList action;  // for Normal Mode
    private final NDManager manager;
    private ReplayBuffer replayBuffer;
    private State currentState;

    /**
     * Constructs a {@link Game} with a basic {@link LruReplayBuffer}.
     *
     * @param manager          the manager for creating the game in
     * @param batchSize        the number of steps to train on per batch
     * @param replayBufferSize the number of steps to hold in the buffer
     */
    public Game(NDManager manager, int batchSize, int replayBufferSize, int gameMode) {
        this(manager, new LruReplayBuffer(batchSize, replayBufferSize));
        setGameMode(gameMode);
        if (gameMode != NOUI_MODE) {
            initFrame(); // 初始化游戏窗口
            setVisible(true);
        }
        initGame(); // 初始化游戏
        setGameState(GAME_START);
    }

    /**
     * Constructs a {@link Game}.
     *
     * @param manager      the manager for creating the game in
     * @param replayBuffer the replay buffer for storing data
     */
    public Game(NDManager manager, ReplayBuffer replayBuffer) {
        this.manager = manager;
        this.replayBuffer = replayBuffer;
        this.currentState = new State(bufImg, initObservation(bufImg), currentReward, currentTerminal);
    }


    public Game() { // Normal Mode
        currentState = new State(bufImg, initObservation(bufImg), currentReward, currentTerminal);
        gameMode = NORMAL_MODE;
        manager = NDManager.newBaseManager();
        action = new NDList(manager.create(DO_NOTHING));
        initFrame(); // 初始化游戏窗口
        setVisible(true); // 窗口设置为可见
        initGame(); // 初始化游戏
    }

    public static int timeStep = 0;
    private static boolean currentTerminal;
    private static float currentReward = 0.1f;
    private boolean preImgSet = false;
    private Step[] batchSteps;
    RlAgent agent;
    Trainer trainer;
    private State postState;

    public void runEnv(RlAgent agent, Trainer trainer, boolean training) throws CloneNotSupportedException {
        this.agent = agent;
        this.trainer = trainer;
        // run the game
        while (true) {
            NDList action = agent.chooseAction(this, training);
            Step step = step(action, training);
            batchSteps = this.getBatch();
//            if (Game.timeStep > OBSERVE) {
//                agent.trainBatch(batchSteps);
//                trainer.step();
//            }
            if (timeStep <= OBSERVE)
                birdMode = "observe";
            else if (timeStep <= OBSERVE + EXPLORE)
                birdMode = "explore";
            else
                birdMode = "train";
            System.out.println("TIMESTEP " + timeStep +
                    " / " + getBirdMode() +
                    " / " + "ACTION " + (Arrays.toString(action.singletonOrThrow().toArray())) +
                    " / " + "REWARD " + step.getReward().getFloat() +
                    " / " + "SCORE " + getScore());
            this.currentState = postState.clone();
            timeStep++;
        }
    }

    /**
     * {@inheritDoc}
     * action[0] == 1 : do nothing
     * aciton[1] == 1 : flap the bird
     */
    @Override
    public Step step(NDList action, boolean training) {
        currentReward = 0.1f;
        currentTerminal = false;

        // actions[0] == 1: do nothing
        // actions[1] == 1: flap the bird
        if (action.singletonOrThrow().getInt(1) == 1) {
            bird.birdFlap();
        }
        stepFrame();
        if (gameMode != NOUI_MODE) {
            repaint();
            try {
                Thread.sleep(FPS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        State preState = new State(preBufImg, initObservation(bufImg), currentReward, currentTerminal);
        preImgSet = true;
        postState = new State(bufImg, getObservation(bufImg), currentReward, currentTerminal);

        FlappyBirdStep step = new FlappyBirdStep(subManager, preState, postState, action);
        if (gameState == GAME_OVER) {
            resetGame();
        }
        if (training) {
            replayBuffer.addStep(step);
        }
        return step;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NDList getObservation() {
        return currentState.getObservation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionSpace getActionSpace() {
        return currentState.getActionSpace(manager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Step[] getBatch() {
        return replayBuffer.getBatch();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        manager.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
    }

    Queue<NDArray> imgQueue = new ArrayDeque<>(4);
    public NDList initObservation(BufferedImage img) {
        // if queue is empty, init observation
        NDArray observation = GameUtil.imgPreprocess(img);
        if (imgQueue.size() == 0) {
            for (int i = 0; i < 4; i++) {
                imgQueue.offer(observation);
            }
        }
        return new NDList(NDArrays.stack(new NDList(observation, observation, observation, observation), 1));
    }

    public NDList getObservation(BufferedImage postImg) {
        // 获取连续帧（4）图片：复制当前帧图片 -> 堆积成4帧图片 -> 将获取到得下一帧图片替换当前第4帧，保证当前的batch图片是连续的。
        NDArray postObservation = GameUtil.imgPreprocess(postImg);
        imgQueue.remove();
        imgQueue.offer(postObservation);
        NDArray[] buf = new NDArray[4];
        int i = 0;
        for (NDArray nd : imgQueue) {
            buf[i++] = nd;
        }
        return new NDList(NDArrays.stack(new NDList(buf[0], buf[1], buf[2], buf[3]), 1));
    }

    static final class FlappyBirdStep implements RlEnv.Step {

        private final NDManager manager;
        private final State preState;
        private final State postState;
        private final NDList action;

        private FlappyBirdStep(NDManager manager, State preState, State postState, NDList action) {
            this.manager = manager;
            this.preState = preState;
            this.postState = postState;
            this.action = action;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NDList getPreObservation() {
            return preState.getObservation();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NDList getAction() {
            return action;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NDList getPostObservation() {
            return postState.getObservation();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ActionSpace getPostActionSpace() {
            return postState.getActionSpace(manager);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NDArray getReward() {
            return manager.create(postState.getReward());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDone() {
            return postState.isTerminal();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
        }
    }

    private static final class State implements Cloneable {
        private NDList observation;
        private BufferedImage observationImg; // use to debug
        private final float reward;
        private final boolean terminal;

        private State(BufferedImage observationImg, NDList observation, float reward, boolean terminal) {
            this.observationImg = observationImg;
            this.observation = observation;
            this.reward = reward;
            this.terminal = terminal;
        }

        private NDList getObservation() {
            return this.observation;
        }

        private ActionSpace getActionSpace(NDManager manager) {
            ActionSpace actionSpace = new ActionSpace();
            actionSpace.add(new NDList(manager.create(DO_NOTHING)));
            actionSpace.add(new NDList(manager.create(FLAP)));
            return actionSpace;
        }

        private float getReward() {
            return reward;
        }

        private boolean isTerminal() {
            return terminal;
        }

        @Override
        public State clone() throws CloneNotSupportedException {
            State cloned = (State) super.clone();
            cloned.observation = (NDList) this.observation.clone();
            return cloned;
        }
    }

    private void initFrame() {
        setSize(FRAME_WIDTH, FRAME_HEIGHT); // 设置窗口大小
        setTitle(GAME_TITLE); // 设置窗口标题
        setLocation(FRAME_X, FRAME_Y); // 窗口初始位置
        setResizable(false); // 设置窗口大小不可变
        // 添加关闭窗口事件（监听窗口发生的事件，派发给参数对象，参数对象调用对应的方法）
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    private void initGame() {
        if (gameMode == NORMAL_MODE) {
            addKeyListener(new BirdKeyListener()); // 添加按键监听
            setGameState(GAME_READY);
            welcomeAnimation = new WelcomeAnimation();
            gameoverAnimation = new GameOverAnimation();
        }
        background = new GameBackground();
        gameElement = new GameElementLayer();
        bird = new Bird();
        new Thread(this).start();  // 启动线程
    }

    // 接收按键事件
    class BirdKeyListener implements KeyListener {
        public void keyPressed(KeyEvent e) {
            int keycode = e.getKeyChar();
            if (keycode == KeyEvent.VK_SPACE) {
                action = new NDList(manager.create(FLAP));
            }
        }

        public void keyReleased(KeyEvent e) {
            int keycode = e.getKeyChar();
            if (keycode == KeyEvent.VK_SPACE) {
                action = new NDList(manager.create(DO_NOTHING));
            }
        }

        public void keyTyped(KeyEvent e) {
        }
    }

    private String birdMode = "observe";

    /**
     * 绘制游戏内容
     * 当repaint()方法被调用时，JVM会调用update()，参数g是系统提供的画笔，由系统进行实例化
     * 单独启动一个线程，不断地快速调用repaint()，让系统对整个窗口进行重绘
     */
    // 项目中存在两个线程：系统线程，自定义的线程：调用repaint()。
    // 系统线程：屏幕内容的绘制，窗口事件的监听与处理
    // 两个线程会抢夺系统资源，可能会出现一次刷新周期所绘制的内容，并没有在一次刷新周期内完成
    // （双缓冲）单独定义一张图片，将需要绘制的内容绘制到这张图片，再一次性地将图片绘制到窗口
    private final BufferedImage bufImg = new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
    private final BufferedImage preBufImg = new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);

    public void stepFrame() {
        Graphics bufG = bufImg.getGraphics();
        if (preImgSet) {
            Graphics postG = preBufImg.getGraphics();
            postG.drawImage(bufImg, 0, 0, null);
            postG.dispose();
            preImgSet = false;
        }
        background.draw(bufG, bird);
        bird.draw(bufG);
        gameElement.draw(bufG, bird);
    }

    public void update(Graphics g) {
        if (gameMode != NOUI_MODE) {
            g.drawImage(bufImg, 0, 0, null); // 将图片绘制到屏幕
        }
        birdFlapped = false;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (timeStep > OBSERVE) {
                System.out.println("start train");
                this.agent.trainBatch(batchSteps);
                this.trainer.step();
            }
        }
    }

    /**
     * reset game
     */
    private void resetGame() {
        setGameState(GAME_START);
        gameElement.reset();
        bird.reset();
    }

    public static void setGameState(int gameState) {
        Game.gameState = gameState;
    }

    public static int getTimeStep() {
        return timeStep;
    }

    public static void setGameMode(int gameMode) {
        Game.gameMode = gameMode;
    }

    public static int getGameMode() {
        return gameMode;
    }

    public String getBirdMode() {
        return birdMode;
    }

    public boolean isTraining() {
        return gameMode != NORMAL_MODE;
    }

    public static void setCurrentTerminal(boolean currentTerminal) {
        Game.currentTerminal = currentTerminal;
    }

    public static void setCurrentReward(float currentReward) {
        Game.currentReward = currentReward;
    }

    public long getScore() {
        return bird.getCurrentScore();
    }
}
