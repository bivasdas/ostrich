package com.bazaarvoice.ostrich.perftest.core.utils;

import com.google.common.base.Strings;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import static com.bazaarvoice.ostrich.pool.ServiceCachingPolicy.ExhaustionAction;

/**
 * This class parses and holds onto the parsed variable.
 * It also handles bad arguments appropriately, and prints message to help rectify them.
 */
@SuppressWarnings ("deprecation")
public class Arguments {

    private enum TestType {
        POOL,
        CACHE
    }

    private Options options = new Options();
    private int _threadSize = 100;
    private int _workSize = 1024 * 5;
    private long _runTimeSecond = Long.MAX_VALUE;
    private int _maxInstance = 10;
    private int _idleTimeSecond = 10;
    private boolean _useMultiThreadedCache = false;
    private ExhaustionAction _exhaustionAction = ExhaustionAction.WAIT;
    private int _reportingIntervalSeconds = 1;
    private int _chaosWorkers = 2;
    private int _chaosInterval = 15;
    private TestType _testType = null;

    private int _zookeeperPort = 2181;
    private int _serverStartingPort = 8000;
    private int _numServers = 10;

    public Arguments(String[] args) {
        setupOptions();
        parseArgs(args);
    }

    public TestType getTestType() {
        return _testType;
    }

    public boolean isPoolTest() {
        return _testType == TestType.POOL;
    }

    public boolean isCacheTest() {
        return _testType == TestType.CACHE;
    }

    public int getThreadSize() {
        return _threadSize;
    }

    public int getWorkSize() {
        return _workSize;
    }

    public long getRunTimeSecond() {
        return _runTimeSecond;
    }

    public int getMaxInstance() {
        return _maxInstance;
    }

    public int getIdleTimeSecond() {
        return _idleTimeSecond;
    }

    public ExhaustionAction getExhaustionAction() {
        return _exhaustionAction;
    }

    public int getReportingIntervalSeconds() {
        return _reportingIntervalSeconds;
    }

    public boolean useMultiThreadedCache() {
        return _useMultiThreadedCache;
    }

    public int getChaosWorkers() {
        return _chaosWorkers;
    }

    public int getChaosInterval() {
        return _chaosInterval;
    }

    public int getZookeeperPort() {
        return _zookeeperPort;
    }

    public int getServerStartingPort() {
        return _serverStartingPort;
    }

    public int getNumServers() {
        return _numServers;
    }

    private void setupOptions() {
        options.addOption("h", "help", false, "Show this help message!");
        options.addOption("T", "test-type", true, "test type to run, POOL or CACHE");

        options.addOption("t", "thread-size", true, "# of workers threads to run, default is 100");
        options.addOption("w", "work-size", true, "length of the string to generate randomly and crunch hash, default is 5120 (5kb)");
        options.addOption("r", "run-time", true, "seconds to run before it kills worker running threads, default is 9223372036854775807 (Long.MAX_VALUE)");

        options.addOption("m", "max-instances", true, "Max instances per end point in service cache, default is 10");
        options.addOption("i", "idle-time", true, "Idle time before service cache should take evict action, default is 10");
        options.addOption("e", "exhaust-action", true, "Exhaust action when cache is exhausted, acceptable values are WAIT|FAIL|GROW, default is WAIT");

        options.addOption("g", "new-cache", false, "Run with new multi threaded cache, default is false");

        options.addOption("c", "chaos-count", true, "Number of chaos workers to use, default is 2");
        options.addOption("l", "chaos-interval", true, "time (in seconds) to wait between chaos, default is 15");

        options.addOption("v", "report-every", true, "Reports on System.out every # seconds");

        options.addOption("z", "zookeeper-port", true, "zookeeper port to use, default is 2181");
        options.addOption("p", "starting-port", true, "starting port to use for services, default is 8000");
        options.addOption("n", "num-servers", true, "number of services to instantiate, default is 10");
    }

    private void parseArgs(String[] args) {

        String opt = null;
        String longOpt = null;
        String value = null;

        try {
            CommandLineParser commandLineParser = new BasicParser();
            CommandLine commandLine = commandLineParser.parse(options, args);

            if (commandLine.hasOption("h")) {
                help();
            }

            if(!commandLine.hasOption("T")) {
                printError(new Exception("Must provide -T/--test-type"), null, null, null);
                help();
            }

            for (Option option : commandLine.getOptions()) {
                opt = option.getOpt();
                longOpt = option.getLongOpt();
                value = option.getValue();

                switch (opt) {
                    case "t":
                        _threadSize = Integer.parseInt(value);
                        break;
                    case "w":
                        _workSize = Integer.parseInt(value);
                        break;
                    case "r":
                        _runTimeSecond = Long.parseLong(value);
                        break;
                    case "m":
                        _maxInstance = Integer.parseInt(value);
                        break;
                    case "i":
                        _idleTimeSecond = Integer.parseInt(value);
                        break;
                    case "e":
                        _exhaustionAction = ExhaustionAction.valueOf(value);
                        break;
                    case "g":
                        _useMultiThreadedCache = true;
                        break;
                    case "v":
                        _reportingIntervalSeconds = Integer.parseInt(value);
                        break;
                    case "c":
                        _chaosWorkers = Integer.parseInt(value);
                        break;
                    case "l":
                        _chaosInterval = Integer.parseInt(value);
                        break;
                    case "T":
                        _testType = TestType.valueOf(value);
                        break;
                    case "z":
                        _zookeeperPort = Integer.parseInt(value);
                        break;
                    case "p":
                        _serverStartingPort = Integer.parseInt(value);
                        break;
                    case "n":
                        _numServers = Integer.parseInt(value);
                        break;
                }
            }
        } catch (IllegalArgumentException ex) {
            printError(opt, longOpt, value);
            help();
        } catch (Exception ex) {
            printError(ex, opt, longOpt, value);
            help();
        }
    }

    private void help() {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("Ostrich Performance Test Suite", options);
        System.exit(0);
    }

    private void printError(String opt, String longOpt, String value) {
        if (!Strings.isNullOrEmpty(opt) && !Strings.isNullOrEmpty(value)) {
            System.err.println(String.format("\"%s\" is not valid value for -%s / --%s", value, opt, longOpt));
        }
    }

    private void printError(Exception ex, String opt, String longOpt, String value) {
        System.err.println(ex.getMessage());
        printError(opt, longOpt, value);
    }
}
