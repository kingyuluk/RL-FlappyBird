package com.kingyu.rlbird.rl.agent;

import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDManager;
import com.kingyu.rlbird.rl.ActionSpace;
import com.kingyu.rlbird.rl.env.RlEnv;
import com.kingyu.rlbird.rl.env.RlEnv.Step;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.training.GradientCollector;
import ai.djl.training.Trainer;
import ai.djl.training.listener.TrainingListener.BatchData;
import ai.djl.translate.Batchifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An {@link RlAgent} that implements Q or Deep-Q Learning.
 *
 * <p>Deep-Q Learning estimates the total reward that will be given until the environment ends in a
 * particular state after taking a particular action. Then, it is trained by ensuring that the
 * prediction before taking the action match what would be predicted after taking the action. More
 * information can be found in the <a
 * href="https://www.cs.toronto.edu/~vmnih/docs/dqn.pdf">paper</a>.
 *
 * <p>It is one of the earliest successful techniques for reinforcement learning with Deep learning.
 * It is also a good introduction to the field. However, many better techniques are commonly used
 * now.
 */
public class QAgent implements RlAgent {

    private final Trainer trainer;
    private final float rewardDiscount;

    /**
     * Constructs a {@link ai.djl.modality.rl.agent.QAgent} with a custom {@link Batchifier}.
     *
     * @param trainer        the trainer for the model to learn
     * @param rewardDiscount the reward discount to apply to rewards from future states
     */
    public QAgent(Trainer trainer, float rewardDiscount) {
        this.trainer = trainer;
        this.rewardDiscount = rewardDiscount;
    }

    private static final Logger logger = LoggerFactory.getLogger(QAgent.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public NDList chooseAction(RlEnv env, boolean training) {
        ActionSpace actionSpace = env.getActionSpace();
        NDArray actionReward = trainer.evaluate(env.getObservation()).singletonOrThrow().get(0);
        logger.info(Arrays.toString(actionReward.toFloatArray()));
        int bestAction = Math.toIntExact(actionReward.argMax().getLong());
        return actionSpace.get(bestAction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trainBatch(Step[] batchSteps) {
        BatchData batchData =
                new BatchData(null, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());

        // temporary manager for attaching NDArray to reduce the gpu memory usage
        NDManager temporaryManager = NDManager.newBaseManager();

        NDList preObservationBatch = new NDList();
        Arrays.stream(batchSteps).forEach(step -> preObservationBatch.addAll(step.getPreObservation(temporaryManager)));
        NDList preInput = new NDList(NDArrays.concat(preObservationBatch, 0));

        NDList postObservationBatch = new NDList();
        Arrays.stream(batchSteps).forEach(step -> postObservationBatch.addAll(step.getPostObservation(temporaryManager)));
        NDList postInput = new NDList(NDArrays.concat(postObservationBatch, 0));

        NDList actionBatch = new NDList();
        Arrays.stream(batchSteps).forEach(step -> actionBatch.addAll(step.getAction()));
        NDList actionInput = new NDList(NDArrays.stack(actionBatch, 0));

        NDList rewardBatch = new NDList();
        Arrays.stream(batchSteps).forEach(step -> rewardBatch.addAll(new NDList(step.getReward())));
        NDList rewardInput = new NDList(NDArrays.stack(rewardBatch, 0));

        try (GradientCollector collector = trainer.newGradientCollector()) {
            NDList QReward = trainer.forward(preInput);
            NDList targetQReward = trainer.forward(postInput);

            NDList Q = new NDList(QReward.singletonOrThrow()
                    .mul(actionInput.singletonOrThrow())
                    .sum(new int[]{1}));

            NDArray[] targetQValue = new NDArray[batchSteps.length];
            for (int i = 0; i < batchSteps.length; i++) {
                if (batchSteps[i].isDone()) {
                    targetQValue[i] = batchSteps[i].getReward();
                } else {
                    targetQValue[i] = targetQReward.singletonOrThrow().get(i)
                            .max()
                            .mul(rewardDiscount)
                            .add(rewardInput.singletonOrThrow().get(i));
                }
            }
            NDList targetQBatch = new NDList();
            Arrays.stream(targetQValue).forEach(value -> targetQBatch.addAll(new NDList(value)));
            NDList targetQ = new NDList(NDArrays.stack(targetQBatch, 0));

            NDArray lossValue = trainer.getLoss().evaluate(targetQ, Q);
            collector.backward(lossValue);
            batchData.getLabels().put(targetQ.singletonOrThrow().getDevice(), targetQ);
            batchData.getPredictions().put(Q.singletonOrThrow().getDevice(), Q);
            this.trainer.step();
        }
        for (Step step : batchSteps) {
            step.attachPostStateManager(step.getManager());
            step.attachPreStateManager(step.getManager());
        }
        temporaryManager.close();  // close the temporary manager
    }
}
