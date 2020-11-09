/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.bird.rl.agent;

import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.ndarray.types.Shape;
import org.bird.main.GameFrame;
import org.bird.rl.ActionSpace;
import org.bird.rl.env.RlEnv;
import org.bird.rl.env.RlEnv.Step;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.training.GradientCollector;
import ai.djl.training.Trainer;
import ai.djl.training.listener.TrainingListener.BatchData;
import ai.djl.translate.Batchifier;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

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

    private Trainer trainer;
    private float rewardDiscount;
    private Batchifier batchifier;

    /**
     * Constructs a {@link ai.djl.modality.rl.agent.QAgent}.
     *
     * <p>It uses the {@link ai.djl.translate.StackBatchifier} as the default batchifier.
     *
     * @param trainer        the trainer for the model to learn
     * @param rewardDiscount the reward discount to apply to rewards from future states
     */
    public QAgent(Trainer trainer, float rewardDiscount) {
        this(trainer, rewardDiscount, Batchifier.STACK);
    }

    /**
     * Constructs a {@link ai.djl.modality.rl.agent.QAgent} with a custom {@link Batchifier}.
     *
     * @param trainer        the trainer for the model to learn
     * @param rewardDiscount the reward discount to apply to rewards from future states
     * @param batchifier     the batchifier to join inputs with
     */
    public QAgent(Trainer trainer, float rewardDiscount, Batchifier batchifier) {
        this.trainer = trainer;
        this.rewardDiscount = rewardDiscount;
        this.batchifier = batchifier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NDList chooseAction(RlEnv env, boolean training) {
        ActionSpace actionSpace = env.getActionSpace();
//        NDList[] inputs = buildInputs(env.getObservation(), actionSpace);
//        NDArray actionScores =
//                trainer.evaluate(batchifier.batchify(inputs)).singletonOrThrow().squeeze(-1);
        NDArray actionScores = trainer.evaluate(env.getObservation()).singletonOrThrow().get(0);
        int bestAction = Math.toIntExact(actionScores.argMax().getLong());
        System.out.print("Q_MAX " + actionScores.max().getFloat());
        return actionSpace.get(bestAction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // TODO : may have problem
    public void trainBatch(Step[] batchSteps) {
        BatchData batchData =
                new BatchData(null, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        for (Step step : batchSteps) {

            /** Initialize replay memory D to size N
             * Initialize action-value function Q with random weights
             * for episode = 1, M do
             *     Initialize state s_1
             *     for t = 1, T do
             *         With probability ϵ select random action a_t
             *         otherwise select a_t=max_a  Q(s_t,a; θ_i)
             *         Execute action a_t in emulator and observe r_t and s_(t+1)
             *         Store transition (s_t,a_t,r_t,s_(t+1)) in D
             *         Sample a minibatch of transitions (s_j,a_j,r_j,s_(j+1)) from D
             *         Set y_j:=
             *             r_j for terminal s_(j+1)
             *             r_j+γ*max_(a^' )  Q(s_(j+1),a'; θ_i) for non-terminal s_(j+1)
             *         Perform a gradient step on (y_j-Q(s_j,a_j; θ_i))^2 with respect to θ
             *     end for
             * end for
             **/
            try (GradientCollector collector = trainer.newGradientCollector()) {
                NDArray results = trainer.forward(step.getPreObservation()).singletonOrThrow(); // [0,0]

                NDList preQ = new NDList(results.get(0).sum());
                NDList postQ;
                if (step.isDone()) {
                    postQ = new NDList(step.getReward());
//                    postQ = new NDList(manager.create(new float[]{-1 * step.getReward().getFloat(), step.getReward().getFloat()}));
                } else {
                    NDArray bestAction = results.get("1:").max();
                    postQ = new NDList(bestAction.mul(rewardDiscount).add(step.getReward()));
//                    NDArray predictionQ = bestAction.mul(rewardDiscount).add(step.getReward());
//                    postQ = new NDList(manager.create(new float[]{-1 * predictionQ.getFloat(), predictionQ.getFloat()}));
                }

                NDArray lossValue = trainer.getLoss().evaluate(postQ, preQ);
                collector.backward(lossValue);
                batchData.getLabels().put(postQ.get(0).getDevice(), postQ);
                batchData.getPredictions().put(preQ.get(0).getDevice(), preQ);

                /**
                 * self.trainStep.run(feed_dict={
                 *        self.yInput : y_batch,
                 *        self.actionInput : action_batch,
                 *         self.observationInput : observation_batch
                 *         })
                 */
            }
        }
//        trainer.notifyListeners(listener -> listener.onTrainingBatch(trainer, batchData));

    }

    public NDArray l2LossEvaluate(NDList preQ, NDList prediction) {
        NDArray pred = prediction.singletonOrThrow();
        NDArray qAction = preQ.singletonOrThrow();
        NDArray loss = qAction.sub(pred).square().mul(1.f / 2);
        return loss.mean();
    }

    private NDList[] buildInputs(NDList observation, List<NDList> actions) {
        NDList[] inputs = new NDList[actions.size()];
        for (int i = 0; i < actions.size(); i++) {
            NDList nextData = new NDList().addAll(observation);
            inputs[i] = nextData;
        }
        return inputs;
    }
}
