package chill.config;


import chill.db.ChillMigrations;
import chill.env.ChillEnv;
import chill.env.ChillMode;
import chill.script.shell.ChillShell;
import chill.util.DirectoryWatcher;
import chill.web.ChillHelper;
import chill.workers.Foreman;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import chill.web.WebServer;

import java.nio.file.*;

import static chill.utils.TheMissingUtils.safely;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static chill.utils.ChillLogs.*;
public class ChillApp {

    static String BANNER = """
                                                                                        \s
                                                                                        \s
            ==========================================================
                 ________  ___  ___  ___  ___       ___         \s
                |\\   ____\\|\\  \\|\\  \\|\\  \\|\\  \\     |\\  \\        \s
                \\ \\  \\___|\\ \\  \\\\\\  \\ \\  \\ \\  \\    \\ \\  \\       \s
                 \\ \\  \\    \\ \\   __  \\ \\  \\ \\  \\    \\ \\  \\      \s
                  \\ \\  \\____\\ \\  \\ \\  \\ \\  \\ \\  \\____\\ \\  \\____ \s
                   \\ \\_______\\ \\__\\ \\__\\ \\__\\ \\_______\\ \\_______\\
                    \\|_______|\\|__|\\|__|\\|__|\\|_______|\\|_______|\s
                                                                                        \s
            ==========================================================
                                  Let's chill..
            """;

    @Option(names = {"--console"}, arity = "0..1", description = "start console", defaultValue = "unset", fallbackValue = "jline")
    String console;

    @Option(names = {"--migrations"}, arity = "0..1", description = "Execute a migration command or start the console", defaultValue = "unset", fallbackValue = "console")
    String migrationCommand;

    @Option(names = {"-m", "--mode"}, description = "The mode that the system is started in")
    ChillMode mode;

    @Option(names = {"-w", "--web"}, description = "Start the chill web server")
    boolean web;

    @Option(names = {"-k", "--worker"}, description = "Start the chill job processor")
    boolean workers;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
    boolean helpRequested = false;

    public void migrationConsole() {
        exec(new String[]{"--migrations"});
    }

    public void reload() {
        safely(() -> Thread.sleep(300));
        WebServer.INSTANCE.restart();
        ChillHelper.INSTANCE.reload();
    }

    public ChillApp exec(String[] args) {

        SLF4JSupport.init();

        CommandLine cmd = new CommandLine(this);

        cmd.parseArgs(args);

        info(BANNER);

        if (helpRequested) {
            cmd.usage(cmd.getOut());
            System.exit(cmd.getCommandSpec().exitCodeOnUsageHelp());
        }

        // if no arguments are passed in, we are in dev mode
        if (mode == null && !web && !workers) {
            String comment = "No mode, web or worker argument passed in.  Starting in dev mode.";
            info(comment);

            workers = true;
            web = true;

            ChillEnv.setMode(ChillMode.Modes.DEV, comment);

            Path tomlFilePath = Path.of("src/main/resources/config/chill.toml");
            if (tomlFilePath.toFile().exists()) {
                Path classesDir = Path.of("target/classes");
                if (classesDir.toFile().exists()) {
                    // reload config on redefinition of /web/* classes
                    DirectoryWatcher.watch(classesDir, evt -> {
                        String modifiedFile = evt.second.toAbsolutePath().toString();
                        if (evt.first.kind() == ENTRY_MODIFY && modifiedFile.endsWith(".class") && modifiedFile.contains("/web/")) {
                            info("Detected change to web config, reloading...");
                            reload();
                        }
                    });
                } else {
                    warn(classesDir + " does not exist, no hot-reloading of routes, etc.");
                }
            } else {
                warn(tomlFilePath + " does not exist, is the app running in the right working directory?");
            }
        }

        // initialize Chill Environment
        ChillEnv.init(this);

        // init db connections
        ChillDataSource.init();

        if (!"unset".equals(migrationCommand)) {
            ChillMigrations.execute(migrationCommand);
            System.exit(0);
            return this;
        }

        if (!"unset".equals(console)) {
            ChillShell chillShell = new ChillShell().withImport("model.*");
            if ("jline".equals(console)) {
                chillShell.jline();
            } else {
                chillShell.simple();
            }
            System.exit(0);
            return this;
        }

        ChillMigrations.checkPendingMigrations(ChillEnv.MODE.isDev());

        if (web) {
            info("Starting web node");
            WebServer.INSTANCE.start();
        }

        if (workers) {
            info("Starting worker node");
            Foreman.INSTANCE.initWorkersAndWeb();
        } else {
            Foreman.INSTANCE.initClient();
        }

        return this;
    }

    public void chillConsole() {
        exec(new String[]{"--console", "simple"});
    }
}