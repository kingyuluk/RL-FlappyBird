package org.kingyu.rlflappybird.util;

import ai.djl.Device;

import org.apache.commons.cli.*;
import org.kingyu.rlflappybird.game.Game;

public class Arguments {

    private int gameMode;
    private int maxGpus;
    private boolean preTrained;
    private String outputDir;
    private String modelDir;

    public Arguments(CommandLine cmd) {
        maxGpus = Device.getGpuCount();
        if (cmd.hasOption("game-mode")) {
            gameMode = Math.min(Integer.parseInt(cmd.getOptionValue("game-mode")), gameMode);
        }
        else{
            gameMode = Game.UI_MODE;
        }

        if (cmd.hasOption("max-gpus")) {
            maxGpus = Math.min(Integer.parseInt(cmd.getOptionValue("max-gpus")), maxGpus);
        }
        preTrained = cmd.hasOption("pre-trained");

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

    public static Arguments parseArgs(String[] args) {
        Options options = Arguments.getOptions();
        try {
            DefaultParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args, null, false);
            if (cmd.hasOption("help")) {
                printHelp("./gradlew run --args='[OPTIONS]'", options);
                return null;
            }
            return new Arguments(cmd);
        } catch (ParseException e) {
            printHelp("./gradlew run --args='[OPTIONS]'", options);
        }
        return null;
    }

    public static Options getOptions() {
        Options options = new Options();
        options.addOption(
                Option.builder("h").longOpt("help").hasArg(false).desc("Print this help.").build());
        options.addOption(
                Option.builder("m")
                        .longOpt("game-mode")
                        .argName("GAMEMODE")
                        .hasArg()
                        .desc("Game mode would like to run")
                        .build());
        options.addOption(
                Option.builder("g")
                        .longOpt("max-gpus")
                        .hasArg()
                        .argName("MAXGPUS")
                        .desc("Max number of GPUs to use for training")
                        .build());
        options.addOption(
                Option.builder("p")
                        .longOpt("pre-trained")
                        .argName("PRE-TRAINED")
                        .desc("Use pre-trained weights")
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



    public int getMaxGpus() {
        return maxGpus;
    }

    public int getGameMode(){
        return gameMode;
    }

    public boolean isPreTrained() {
        return preTrained;
    }

    public String getModelDir() {
        return modelDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    private static void printHelp(String msg, Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setLeftPadding(1);
        formatter.setWidth(120);
        formatter.printHelp(msg, options);
    }
}
