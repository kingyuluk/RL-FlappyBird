package org.bird.main;

import ai.djl.modality.cv.util.NDImageUtils;
import org.bird.rl.ActionSpace;
import org.bird.rl.LruReplayBuffer;
import org.bird.rl.ReplayBuffer;
import org.bird.rl.agent.RlAgent;
import org.bird.rl.env.RlEnv;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import org.bird.util.GameUtil;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

import static org.bird.ai.DQN.EXPLORE;
import static org.bird.ai.DQN.OBSERVE;
import static org.bird.util.Constant.*;

/**
 * 主窗口类，游戏窗口和绘制的相关内容
 *
 * @author Kingyu
 */

public class GameFrame extends Frame implements Runnable, RlEnv {
    private static final long serialVersionUID = 1L; // 保持版本的兼容性

    NDManager subManager = NDManager.newBaseManager();

    private static int gameMode;
    public static final int NORMAL_MODE = 0;
    public static final int TRAIN_MODE = 1;
    public static final int TEST_MODE = 2;

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
    private final State state;
    private ReplayBuffer replayBuffer;

    /**
     * Constructs a {@link GameFrame} with a basic {@link LruReplayBuffer}.
     *
     * @param manager          the manager for creating the game in
     * @param batchSize        the number of steps to train on per batch
     * @param replayBufferSize the number of steps to hold in the buffer
     */
    public GameFrame(NDManager manager, int batchSize, int replayBufferSize, int gameMode) {
        this(manager, new LruReplayBuffer(batchSize, replayBufferSize));
        setGameMode(gameMode);
        if (gameMode != TRAIN_MODE) {
            initFrame(); // 初始化游戏窗口
        }
        setVisible(true);
        initGame(); // 初始化游戏
        setGameState(GAME_START);
    }

    /**
     * Constructs a {@link GameFrame}.
     *
     * @param manager      the manager for creating the game in
     * @param replayBuffer the replay buffer for storing data
     */
    public GameFrame(NDManager manager, ReplayBuffer replayBuffer) {
        this.manager = manager;
        this.replayBuffer = replayBuffer;
        this.state = new State(bufImg, initObservation(bufImg), currentReward, currentTerminal);
    }


    public GameFrame() { // Normal Mode
        state = new State(bufImg, initObservation(bufImg), currentReward, currentTerminal);
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
    boolean preImgSet = false;

    public void runEnv(RlAgent agent, boolean training) {
        // run the game
        NDList action = agent.chooseAction(this, training);
        Step step = step(action, training);

        System.out.println("TIMESTEP " + timeStep +
                " / " + getBirdMode() +
                " / " + "ACTION " + (action.singletonOrThrow().getInt(0)) +
                " / " + "REWARD " + step.getReward().getFloat() +
                " / " + "SCORE " + getScore());
    }

    /**
     * {@inheritDoc}
     * action[0] == 1 : do nothing
     * aciton[1] == 1 : flap the bird
     */
    @Override
    public Step step(NDList action, boolean training) {
        switch (gameState) {
            case GAME_READY: // Only effective in normal mode
                if (action.singletonOrThrow().getInt(1) == 1) {
                    // 游戏启动界面时接收到动作，小鸟振翅并开始游戏
                    bird.birdFlap();
                    bird.birdFall();
                    setGameState(GAME_START); // 游戏状态改变
                }
                break;
            case GAME_START:
                bird.birdFall();
                if (action.singletonOrThrow().getInt(1) == 1) {
                    //游戏过程中接收到动作则振翅一次
                    bird.birdFlap();
                    bird.birdFall();
                }
                break;
            case GAME_OVER:
                if (gameMode != NORMAL_MODE) {
                    state.reward = -1f;
                    state.terminal = true;
                    resetGame();
                } else if (action.singletonOrThrow().getInt(1) == 1) {
                    resetGame();  //游戏结束时接收到动作重置游戏
                }
                break;
        }
        repaint();
        try {
            Thread.sleep(FPS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        State preState = new State(preBufImg, initObservation(bufImg), currentReward, currentTerminal);
        preImgSet = true;

        State postState = new State(bufImg, setObservation(bufImg), currentReward, currentTerminal);
        currentReward = 0.1f;  // reset reward

        FlappyBirdStep step = new FlappyBirdStep(subManager, preState, postState, action);
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
        return state.getObservation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionSpace getActionSpace() {
        return state.getActionSpace(manager);
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

    Queue<NDArray> imgQueue = new LinkedList<>();

    public NDList initObservation(BufferedImage img) {
        // if queue is empty, init observation队为空则初始化，否则将队里的图片存入NDList作为preObservation
        NDArray observation = NDImageUtils.toTensor(GameUtil.imgPreprocess(img));
        if (imgQueue.size() == 0) {
            for (int i = 0; i < 4; i++)
                imgQueue.offer(observation);
            return new NDList(NDArrays.stack(new NDList(observation, observation, observation, observation), 1));
        }
        // else save the img the queue to the NDList as preObservation
        else {
            NDArray[] buf = new NDArray[4];
            int i = 0;
            for (NDArray nd : imgQueue) {
                buf[i++] = nd;
            }
            return new NDList(NDArrays.stack(new NDList(buf[0], buf[1], buf[2], buf[3]), 1));
        }
    }

    public NDList setObservation(BufferedImage postImg) {
        // 获取连续帧（4）图片：复制当前帧图片 -> 堆积成4帧图片 -> 将获取到得下一帧图片替换当前第4帧，保证当前的batch图片是连续的。
        NDArray postObservation = NDImageUtils.toTensor(GameUtil.imgPreprocess(postImg));
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
//            return GameFrame.isDraw();
            return postState.isTerminal();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
        }
    }

    private static final class State {
        private NDList observation;
        private BufferedImage observationImg;
        private float reward;
        private boolean terminal;

        private State(BufferedImage observationImg, NDList observation, float reward, boolean terminal) {
            this.observationImg = observationImg;
            this.observation = observation;
            this.reward = reward;
            this.terminal = terminal;
        }

        /**
         * {@inheritDoc}
         */
        private BufferedImage getObservationImg() {
            return observationImg;
        }


        private NDList getObservation() {
            return this.observation;
        }

        /**
         * {@inheritDoc}
         */
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

    public void update(Graphics g) {
        Graphics bufG = bufImg.getGraphics();
        if (preImgSet) {
            Graphics postG = preBufImg.getGraphics();
            postG.drawImage(bufImg, 0, 0, null);
            postG.dispose();
        }
        background.draw(bufG, bird);
//        if (gameMode == NORMAL_MODE && gameState == GAME_READY) {
//            welcomeAnimation.draw(bufG);
//        } else {
//            gameElement.draw(bufG, bird);
//        }
        bird.draw(bufG);
        gameElement.draw(bufG, bird);
//        if (gameMode == NORMAL_MODE && bird.isDead()) {
//            gameoverAnimation.draw(bufG, bird);
//        }

//        if (gameMode == NORMAL_MODE) {
//            step(action, isTraining());
//            action = new NDList(manager.create(DO_NOTHING));
//        }
        if (gameMode != TRAIN_MODE) {
            g.drawImage(bufImg, 0, 0, null); // 将图片绘制到屏幕
        }
        birdFlapped = false;

        if (timeStep <= OBSERVE)
            birdMode = "observe";
        else if (timeStep <= OBSERVE + EXPLORE)
            birdMode = "explore";
        else
            birdMode = "train";
        timeStep++;
    }

    @Override
    public void run() {
//        while (true) {
//            repaint(); // 通过调用repaint(),让JVM调用update()
//            try {
//                Thread.sleep(GAME_INTERVAL); // FPS : 30
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }

    /**
     * reset game
     */
    private void resetGame() {
        setGameState(GAME_START);
        gameElement.reset();
        bird.reset();
        setCurrentReward(0.1f);
        setCurrentTerminal(false);
    }

    public static void setGameState(int gameState) {
        GameFrame.gameState = gameState;
    }

    public static int getTimeStep() {
        return timeStep;
    }

    public static void setGameMode(int gameMode) {
        GameFrame.gameMode = gameMode;
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
        GameFrame.currentTerminal = currentTerminal;
    }

    public static void setCurrentReward(float currentReward) {
        GameFrame.currentReward = currentReward;
    }

    public long getScore() {
        return bird.getCurrentScore();
    }
}
