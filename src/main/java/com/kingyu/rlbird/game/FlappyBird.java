package com.kingyu.rlbird.game;

import com.kingyu.rlbird.game.content.Bird;
import com.kingyu.rlbird.game.content.GameBackground;
import com.kingyu.rlbird.game.content.GameElementLayer;
import com.kingyu.rlbird.rl.ActionSpace;
import com.kingyu.rlbird.rl.LruReplayBuffer;
import com.kingyu.rlbird.rl.ReplayBuffer;
import com.kingyu.rlbird.rl.agent.RlAgent;
import com.kingyu.rlbird.rl.env.RlEnv;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import com.kingyu.rlbird.util.GameUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.*;

import static com.kingyu.rlbird.ai.TrainBird.OBSERVE;
import static com.kingyu.rlbird.util.Constant.*;

/**
 *
 * @author Kingyu
 */

public class FlappyBird extends Frame implements RlEnv {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(FlappyBird.class);

    private static int trainingMode;
    public static final int NOUI_MODE = 0;
    public static final int UI_MODE = 1;

    private static int gameState;
    public static final int GAME_START = 1;
    public static final int GAME_OVER = 2;

    private GameBackground background;
    private Bird bird;
    private GameElementLayer gameElement;

    private final NDManager manager;
    private final ReplayBuffer replayBuffer;
    private final BufferedImage currentImg = new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
    private State currentState;

    /**
     * Constructs a {@link FlappyBird} with a basic {@link LruReplayBuffer}.
     *
     * @param manager          the manager for creating the game in
     * @param batchSize        the number of steps to train on per batch
     * @param replayBufferSize the number of steps to hold in the buffer
     */
    public FlappyBird(NDManager manager, int batchSize, int replayBufferSize, int gameMode) {
        this(manager, new LruReplayBuffer(batchSize, replayBufferSize));
        setTrainingMode(gameMode);
        if (gameMode == UI_MODE) {
            initFrame(); // 初始化游戏窗口
            this.setVisible(true);
        }
        background = new GameBackground();
        gameElement = new GameElementLayer();
        bird = new Bird();
        setGameState(GAME_START);
    }

    /**
     * Constructs a {@link FlappyBird}.
     *
     * @param manager      the manager for creating the game in
     * @param replayBuffer the replay buffer for storing data
     */
    public FlappyBird(NDManager manager, ReplayBuffer replayBuffer) {
        this.manager = manager;
        this.replayBuffer = replayBuffer;
        this.currentState = new State(createObservation(currentImg), currentReward, currentTerminal);
//        this.postState = new State(createObservation(bufImg), currentReward, currentTerminal);
    }

    public static int timeStep = 0;
    public static int trainStep = 0;
    private static boolean currentTerminal = false;
    private static float currentReward = 0.2f;
    private String trainState = "observe";

    /**
     * {@inheritDoc}
     */
    @Override
    public Step[] runEnvironment(RlAgent agent, boolean training) {
        // run the game
        NDList action = agent.chooseAction(this, training);
        step(action, training);
        Step[] batchSteps = this.getBatch();
        if (timeStep <= OBSERVE) {
            trainState = "observe";
        } else {
            trainState = "explore";
        }
//        this.currentState = postState.clone();
        timeStep++;
        return batchSteps;
    }

    /**
     * {@inheritDoc}
     * action[0] == 1 : do nothing
     * action[1] == 1 : flap the bird
     */
//    int FRAME_PER_ACTION = 1;
    @Override
    public void step(NDList action, boolean training) {
        currentReward = 0.2f;
        currentTerminal = false;
//        if (timeStep % FRAME_PER_ACTION == 0 && action.singletonOrThrow().getInt(1) == 1) {
        if (action.singletonOrThrow().getInt(1) == 1) {
            bird.birdFlap();
        }
        stepFrame();
        if (trainingMode == UI_MODE) {
            repaint();
            try {
                Thread.sleep(FPS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

//        State preState = new State(postState.getObservation(), currentReward, currentTerminal);
        State preState = new State(currentState.getObservation(), currentReward, currentTerminal);
//        postState = new State(createObservation(bufImg), currentReward, currentTerminal);
        currentState = new State(createObservation(currentImg), currentReward, currentTerminal);

//        FlappyBirdStep step = new FlappyBirdStep(manager.newSubManager(), preState, postState, action);
        FlappyBirdStep step = new FlappyBirdStep(manager.newSubManager(), preState, currentState, action);
        if (training) {
            replayBuffer.addStep(step);
        }
        logger.info("TIME_STEP " + timeStep +
                " / " + "TRAIN_STEP " + trainStep +
                " / " + getTrainState() +
                " / " + "ACTION " + (Arrays.toString(action.singletonOrThrow().toArray())) +
                " / " + "REWARD " + step.getReward().getFloat() +
                " / " + "SCORE " + getScore());
        if (gameState == GAME_OVER) {
            restartGame();
        }
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
        currentReward = 0.2f;
        currentTerminal = false;
    }

    private final Queue<NDArray> imgQueue = new ArrayDeque<>(4);

    /**
     * Convert image to CNN input.
     * Copy the initial frame image, stack into NDList,
     * then replace the fourth frame with the current frame to ensure that the batch picture is continuous.
     *
     * @param currentImg  the image of current frame
     * @return the CNN input
     */
    public NDList createObservation(BufferedImage currentImg) {
        NDArray observation = GameUtil.imgPreprocess(currentImg);
        if (imgQueue.isEmpty()) {
            for (int i = 0; i < 4; i++) {
                imgQueue.offer(observation);
            }
            return new NDList(NDArrays.stack(new NDList(observation, observation, observation, observation), 1));
        } else {
            imgQueue.remove();
            imgQueue.offer(observation);
            NDArray[] buf = new NDArray[4];
            int i = 0;
            for (NDArray nd : imgQueue) {
                buf[i++] = nd;
            }
            return new NDList(NDArrays.stack(new NDList(buf[0], buf[1], buf[2], buf[3]), 1));
        }
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
        public NDList getPreObservation() {
            return preState.getObservation();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
        }
    }

    private static final class State{
        private final NDList observation;
        private final float reward;
        private final boolean terminal;

        private State(NDList observation, float reward, boolean terminal) {
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

//        @Override
//        public State clone() throws CloneNotSupportedException {
//            State cloned = (State) super.clone();
//            cloned.observation = (NDList) this.observation.clone();
//            return cloned;
//        }
    }

    /**
     * Draw one frame by performing all elements' draw function.
     */
    public void stepFrame() {
        Graphics bufG = currentImg.getGraphics();
        background.draw(bufG, bird);
        bird.draw(bufG);
        gameElement.draw(bufG, bird);
    }

    /**
     * Initialize the game frame
     */
    private void initFrame() {
        setSize(FRAME_WIDTH, FRAME_HEIGHT); // 设置窗口大小
        setTitle(GAME_TITLE); // 设置窗口标题
        setLocation(FRAME_X, FRAME_Y); // 窗口初始位置
        setResizable(false); // 设置窗口大小不可变
        setVisible(true);
        // 添加关闭窗口事件（监听窗口发生的事件，派发给参数对象，参数对象调用对应的方法）
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    /**
     * Restart game
     */
    private void restartGame() {
        setGameState(GAME_START);
        gameElement.reset();
        bird.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Graphics g) {
        g.drawImage(currentImg, 0, 0, null);
    }

    public static void setGameState(int gameState) {
        FlappyBird.gameState = gameState;
    }

    public static void setTrainingMode(int trainingMode) {
        FlappyBird.trainingMode = trainingMode;
    }

    public static int getTrainingMode() {
        return trainingMode;
    }

    public String getTrainState() {
        return this.trainState;
    }

    public static void setCurrentTerminal(boolean currentTerminal) {
        FlappyBird.currentTerminal = currentTerminal;
    }

    public static void setCurrentReward(float currentReward) {
        FlappyBird.currentReward = currentReward;
    }

    public long getScore() {
        return this.bird.getCurrentScore();
    }
}
