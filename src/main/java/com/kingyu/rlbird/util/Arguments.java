package com.kingyu.rlbird.util;

import ai.djl.Device;

import com.kingyu.rlbird.game.FlappyBird;
import org.apache.commons.cli.*;

public class Arguments {

    private final int trainingMode;
    private int batchSize;
    private int maxGpus;
    private boolean preTrained;
    private String outputDir;
    private String modelDir;

    public Arguments(CommandLine cmd) {
        maxGpus = Device.getGpuCount();
        if (cmd.hasOption("game-mode")) {
            trainingMode = Integer.parseInt(cmd.getOptionValue("game-mode"));
        } else {
            trainingMode = FlappyBird.NOUI_MODE;
        }

        if (cmd.hasOption("batch-size")) {
            batchSize = Integer.parseInt(cmd.getOptionValue("batch-size"));
        } else {
            batchSize = maxGpus > 0 ? 32 * maxGpus : 32;
        }

        preTrained = cmd.hasOption("pre-trained");

        if (cmd.hasOption("max-gpus")) {
            maxGpus = Math.min(Integer.parseInt(cmd.getOptionValue("max-gpus")), maxGpus);
        }

        if (cmd.hasOption("output-dir")) {
            outputDir = cmd.getOptionValue("output-dir");
        } else {
            outputDir = "build/model";
        }
        if (cmd.hasOption("model-dir")) {
            modelDir = cmd.getOptionValue("model-dir");
        } else {
            modelDir = null;
        }
    }

    public static Arguments parseArgs(String[] args) throws ParseException {
        Options options = Arguments.getOptions();
        DefaultParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args, null, false);
        return new Arguments(cmd);
    }

    public static Options getOptions() {
        Options options = new Options();
        options.addOption(
                Option.builder("m")
                        .longOpt("game-mode")
                        .argName("GAMEMODE")
                        .hasArg()
                        .desc("Training mode would like to run")
                        .build());
        options.addOption(
                Option.builder("b")
                        .longOpt("batch-size")
                        .hasArg()
                        .argName("BATCH-SIZE")
                        .desc("The batch size of the training data.")
                        .build());
        options.addOption(
                Option.builder("p")
                        .longOpt("pre-trained")
                        .argName("PRE-TRAINED")
                        .desc("Use pre-trained weights")
                        .build());
        options.addOption(
                Option.builder("g")
                        .longOpt("max-gpus")
                        .hasArg()
                        .argName("MAXGPUS")
                        .desc("Max number of GPUs to use for training")
                        .build());
        options.addOption(
                Option.builder("o")
                        .longOpt("output-dir")
                        .hasArg()
                        .argName("OUTPUT-DIR")
                        .desc("Use output to determine directory to save your model parameters")
                        .build());
        options.addOption(
                Option.builder("d")
                        .longOpt("model-dir")
                        .hasArg()
                        .argName("MODEL-DIR")
                        .desc("pre-trained model file directory")
                        .build());
        return options;
    }

    public int getTrainingMode() {
        return trainingMode;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public boolean isPreTrained() {
        return preTrained;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public int getMaxGpus() {
        return maxGpus;
    }

    public String getModelDir() {
        return modelDir;
    }
}
