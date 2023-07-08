package net.burningtnt.jstackmc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/*
 JVM:
 cd "release/Jstack Minecraft"&jlink --add-modules java.base,jdk.attach,jdk.internal.jvmstat,jdk.attach,java.instrument --strip-debug --no-man-pages --no-header-files --compress=2 --output jre_Windows
 */

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            realMain();
        } catch (Throwable e) {
            Logger.error("An Uncaught error was thrown.", e);
        }

        Scanner scanner = new Scanner(System.in);
        Logger.info("Presss enter to exit. 按回车关闭。");
        scanner.nextLine();
    }

    private static void realMain() throws URISyntaxException, sun.jvmstat.monitor.MonitorException, IOException, InterruptedException {
        int minecraftPid = MinecraftVMScanner.getMinecraftVM();

        if (minecraftPid == -1) {
            Logger.info("No Minecraft VM detected.");
            throw new UnsupportedEncodingException("No Minecraft VM detected");
        }

        Logger.info(String.format("Find Minecraft VM with pid %s.", minecraftPid));

        Path output = Path.of("dumps", String.format("minecraft-jstack-dump-%s.txt", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd-hh-mm-ss")
        ))).toAbsolutePath();

        Files.createDirectories(output.getParent());

        GameDumpCreator.writeDumpTo(minecraftPid, output);

        openOutputFile(output);
    }

    private static void openOutputFile(Path output) {
        String systemInfo = System.getProperty("os.name");
        if (systemInfo == null) {
            Logger.error("Failed to get System info.", new NullPointerException("Cannot get system info because System.getProperty(\"os.name\") is null."));
            return;
        }

        String commandLine;
        if (systemInfo.toLowerCase().startsWith("windows")) {
            commandLine = String.format("C:\\Windows\\explorer.exe /select,\"%s\"", output.toString());
        } else {
            commandLine = String.format("open -R \"%s\"", output.toString());
        }

        try {
            Runtime.getRuntime().exec(commandLine);

            Logger.info(String.format("Success to create process with the command line \"%s\".", commandLine));
        } catch (Throwable e) {
            Logger.error(String.format("An Error was thrown while creating process with the command line \"%s\".", commandLine), e);
        }
    }
}
