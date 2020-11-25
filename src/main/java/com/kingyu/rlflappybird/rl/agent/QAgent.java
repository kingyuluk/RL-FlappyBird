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
package com.kingyu.rlflappybird.rl.agent;

import com.kingyu.rlflappybird.rl.ActionSpace;
import com.kingyu.rlflappybird.rl.env.RlEnv;
import com.kingyu.rlflappybird.rl.env.RlEnv.Step;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.training.GradientCollector;
import ai.djl.training.Trainer;
import ai.djl.training.listener.TrainingListener.BatchData;
import ai.djl.translate.Batchifier;


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

    /**
     * {@inheritDoc}
     */
    @Override
    public NDList chooseAction(RlEnv env, boolean training) {
        ActionSpace actionSpace = env.getActionSpace();
        NDArray actionReward = trainer.evaluate(env.getObservation()).singletonOrThrow().get(0);
        System.out.println(Arrays.toString(actionReward.toFloatArray()));
        int bestAction = Math.toIntExact(actionReward.argMax().getLong());
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
            /* Initialize replay memory D to size N
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
                NDList Q = new NDList(
                        trainer.forward(step.getPreObservation()).singletonOrThrow()
                                .mul(step.getAction().singletonOrThrow()).sum());

                NDList targetQ;
                if (step.isDone()) {
                    targetQ = new NDList(step.getReward());
                } else {
                    targetQ = new NDList(
                            trainer.forward(step.getPostObservation()).singletonOrThrow().max()
                                    .mul(rewardDiscount).add(step.getReward()));
                }

                NDArray lossValue = trainer.getLoss().evaluate(targetQ, Q);
                collector.backward(lossValue);
                batchData.getLabels().put(targetQ.get(0).getDevice(), targetQ);
                batchData.getPredictions().put(Q.get(0).getDevice(), Q);
                this.trainer.step();

                /*
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
}
