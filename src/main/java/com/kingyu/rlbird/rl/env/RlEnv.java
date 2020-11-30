package com.kingyu.rlbird.rl.env;

import ai.djl.ndarray.NDManager;
import com.kingyu.rlbird.rl.ActionSpace;
import com.kingyu.rlbird.rl.agent.RlAgent;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;

/**
 * An environment to use for reinforcement learning.
 */
public interface RlEnv extends AutoCloseable {

    /**
     * Resets the environment to it's default state.
     */
    void reset();

    /**
     * Returns the observation detailing the current state of the environment.
     *
     * @return the observation detailing the current state of the environment
     */
    NDList getObservation();

    /**
     * Returns the current actions that can be taken in the environment.
     *
     * @return the current actions that can be taken in the environment
     */
    ActionSpace getActionSpace();

    /**
     * Takes a step by performing an action in this environment.
     *
     * @param action   the action to perform
     * @param training true if the step is during training
     */
    void step(NDList action, boolean training);

    /**
     * Runs the environment from reset until done.
     *
     * @param agent    the agent to choose the actions with
     * @param training true to run while training. When training, the steps will be recorded
     * @return the replayMemory
     */
    Step[] runEnvironment(RlAgent agent, boolean training);

    /**
     * Returns a batch of steps from the environment {@link ai.djl.modality.rl.ReplayBuffer}.
     *
     * @return a batch of steps from the environment {@link ai.djl.modality.rl.ReplayBuffer}
     */
    Step[] getBatch();

    /**
     * {@inheritDoc}
     */
    @Override
    void close();

    /**
     * A record of taking a step in the environment.
     */
    interface Step extends AutoCloseable {

        /**
         * Returns the observation before the action which attach to manager.
         *
         * @param manager manager attach to
         * @return the observation before the action which attach to manager.
         */
        NDList getPreObservation(NDManager manager);

        /**
         * Returns the observation before the action
         *
         * @return the observation before the action
         */
        NDList getPreObservation();

        /**
         * Returns the action taken.
         *
         * @return the action taken
         */
        NDList getAction();

        /**
         * Returns the observation after the action which attach to manager.
         *
         * @param manager manager attach to
         * @return the observation after the action which attach to manager.
         */
        NDList getPostObservation(NDManager manager);

        /**
         * Return the observation after the action
         *
         * @return @return the observation after the action
         */
        NDList getPostObservation();

        /**
         * Returns the manager which manage this step.
         *
         * @return the manager which manage this step
         */
        NDManager getManager();


        /**
         * Returns the reward given for the action.
         *
         * @return the reward given for the action
         */
        NDArray getReward();

        /**
         * Returns whether the environment is finished or can accept further actions.
         *
         * @return true if the environment is finished and can no longer accept further actions.
         */
        boolean isTerminal();

        /**
         * {@inheritDoc}
         */
        @Override
        void close();
    }
}
