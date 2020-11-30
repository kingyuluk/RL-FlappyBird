package com.kingyu.rlbird.ai;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Activation;
import ai.djl.nn.Blocks;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.convolutional.Conv2d;
import ai.djl.nn.core.Linear;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.initializer.NormalInitializer;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Adam;
import ai.djl.training.tracker.LinearTracker;
import ai.djl.training.tracker.Tracker;
import com.kingyu.rlbird.game.FlappyBird;
import com.kingyu.rlbird.rl.agent.EpsilonGreedy;
import com.kingyu.rlbird.rl.agent.QAgent;
import com.kingyu.rlbird.rl.agent.RlAgent;
import com.kingyu.rlbird.rl.env.RlEnv;
import com.kingyu.rlbird.util.Arguments;
import com.kingyu.rlbird.util.Constant;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class TrainBird {
    private static final Logger logger = LoggerFactory.getLogger(TrainBird.class);

    public static final int OBSERVE = 1000; // gameSteps to observe before training
    public static final int EXPLORE = 3000000; // frames over which to anneal epsilon
    public static final int SAVE_EVERY_STEPS = 100000; // save model every 100,000 step
    public static final int REPLAY_BUFFER_SIZE = 50000; // number of previous transitions to remember
    public static final float REWARD_DISCOUNT = 0.9f; // decay rate of past observations
    public static final float INITIAL_EPSILON = 0.01f;
    public static final float FINAL_EPSILON = 0.0001f;
    public static final String PARAMS_PREFIX = "dqn-trained";

    static RlEnv.Step[] batchSteps;

    private TrainBird() {}

    public static void main(String[] args) throws ParseException, IOException, MalformedModelException {
        Arguments arguments = Arguments.parseArgs(args);
        Model model = createOrLoadModel(arguments);
        if (arguments.isTesting()) {
            test(model);
        } else {
            train(arguments, model);
        }
    }

    public static Model createOrLoadModel(Arguments arguments) throws IOException, MalformedModelException {
        Model model = Model.newInstance("QNetwork");
        model.setBlock(getBlock());
        if (arguments.usePreTrained()) {
            model.load(Paths.get(Constant.MODEL_PATH), PARAMS_PREFIX);
        }
        return model;
    }

    public static void train(Arguments arguments, Model model) {
        boolean withGraphics = arguments.withGraphics();
        boolean training = !arguments.isTesting();
        int batchSize = arguments.getBatchSize();  // size of mini batch

        FlappyBird game = new FlappyBird(NDManager.newBaseManager(), batchSize, REPLAY_BUFFER_SIZE, withGraphics);

            DefaultTrainingConfig config = setupTrainingConfig();
            try (Trainer trainer = model.newTrainer(config)) {
                trainer.initialize(new Shape(batchSize, 4, 80, 80));
                trainer.notifyListeners(listener -> listener.onTrainingBegin(trainer));

                RlAgent agent = new QAgent(trainer, REWARD_DISCOUNT);
                Tracker exploreRate =
                        new LinearTracker.Builder()
                                .setBaseValue(INITIAL_EPSILON)
                                .optSlope(-(INITIAL_EPSILON - FINAL_EPSILON) / EXPLORE)
                                .optMinValue(FINAL_EPSILON)
                                .build();
                agent = new EpsilonGreedy(agent, exploreRate);

                int numOfThreads = 2;
                List<Callable<Object>> callables = new ArrayList<>(numOfThreads);
                callables.add(new GeneratorCallable(game, agent, training));
                if(training) {
                    callables.add(new TrainerCallable(model, agent));
                }
                ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);
                try {
                    try {
                        List<Future<Object>> futures = new ArrayList<>();
                        for (Callable<Object> callable : callables) {
                            futures.add(executorService.submit(callable));
                        }
                        for (Future<Object> future : futures) {
                            future.get();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        logger.error("", e);
                    }
                } finally {
                    executorService.shutdown();
                }
            }
    }

    public static void test(Model model) {
        FlappyBird game = new FlappyBird(NDManager.newBaseManager(), 1, 1, true);
        DefaultTrainingConfig config = setupTrainingConfig();
        try (Trainer trainer = model.newTrainer(config)) {
            RlAgent agent = new QAgent(trainer, REWARD_DISCOUNT);
            while (true) {
                game.runEnvironment(agent, false);
            }
        }
    }

    private static class TrainerCallable implements Callable<Object> {
        private final RlAgent agent;
        private final Model model;

        public TrainerCallable(Model model, RlAgent agent) {
            this.model = model;
            this.agent = agent;
        }

        @Override
        public Object call() throws Exception {
            while (FlappyBird.trainStep < EXPLORE) {
                Thread.sleep(0);
                if (FlappyBird.gameStep > OBSERVE) {
                    this.agent.trainBatch(batchSteps);
                    FlappyBird.trainStep++;
                    if (FlappyBird.trainStep > 0 && FlappyBird.trainStep % SAVE_EVERY_STEPS == 0) {
                        model.save(Paths.get(Constant.MODEL_PATH), "dqn-" + FlappyBird.trainStep);
                    }
                }
            }
            return null;
        }
    }

    private static class GeneratorCallable implements Callable<Object> {
        private final FlappyBird game;
        private final RlAgent agent;
        private final boolean training;

        public GeneratorCallable(FlappyBird game, RlAgent agent, boolean training) {
            this.game = game;
            this.agent = agent;
            this.training = training;
        }

        @Override
        public Object call() {
            while (FlappyBird.trainStep < EXPLORE) {
                batchSteps = game.runEnvironment(agent, training);
            }
            return null;
        }
    }

    public static SequentialBlock getBlock() {
        // conv -> conv -> conv -> fc -> fc
        return new SequentialBlock()
                .add(Conv2d.builder()
                        .setKernelShape(new Shape(8, 8))
                        .optStride(new Shape(4, 4))
                        .optPadding(new Shape(3, 3))
                        .setFilters(4).build())
                .add(Activation::relu)

                .add(Conv2d.builder()
                        .setKernelShape(new Shape(4, 4))
                        .optStride(new Shape(2, 2))
                        .setFilters(32).build())
                .add(Activation::relu)

                .add(Conv2d.builder()
                        .setKernelShape(new Shape(3, 3))
                        .optStride(new Shape(1, 1))
                        .setFilters(64).build())
                .add(Activation::relu)

                .add(Blocks.batchFlattenBlock())
                .add(Linear
                        .builder()
                        .setUnits(512).build())
                .add(Activation::relu)

                .add(Linear
                        .builder()
                        .setUnits(2).build());
    }

    public static DefaultTrainingConfig setupTrainingConfig() {
        return new DefaultTrainingConfig(Loss.l2Loss())
                .optOptimizer(Adam.builder().optLearningRateTracker(Tracker.fixed(1e-6f)).build())
                .addEvaluator(new Accuracy())
                .optInitializer(new NormalInitializer())
                .addTrainingListeners(TrainingListener.Defaults.basic());
    }
}
