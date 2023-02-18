package net.burningtnt.jstackmc;

import com.sun.tools.attach.VirtualMachine;
import sun.jvmstat.monitor.*;
import sun.tools.attach.HotSpotVirtualMachine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        try {
            realMain();
        } catch (Throwable e) {
            Logger.error("An Uncaught error was thrown.", e);
        }

        Scanner scanner = new Scanner(System.in);
        Logger.info("Presss enter to exit. 按回车关闭。");
        scanner.next();
    }

    private static void realMain() throws URISyntaxException, MonitorException {
        int minecraftPid = getMinecraftVM();

        if (minecraftPid == -1) {
            Logger.info("No Minecraft VM detected.");

            Scanner scanner = new Scanner(System.in);
            Logger.info("Presss enter to exit. 按回车关闭。");
            scanner.next();

            System.exit(0);
        }


        Logger.info(String.format("Get Jstack output of Minecraft VM with pid %s.", minecraftPid));

        File outputFile = getCurrentOutputFile();
        int time = 5;
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            for (int i = 0; i < time; i++) {
                Logger.info(String.format("Get Jstack output of Minecraft VM with pid %s for %sth(st/rd) time.", minecraftPid, i + 1));

                fileOutputStream.write(getDataDumpHead().getBytes(StandardCharsets.UTF_8));
                String data = Objects.requireNonNullElse(getDataDump(minecraftPid), "Cannot attach to JVM.");
                if (data.toLowerCase().contains("deadlock")) {
                    fileOutputStream.write("Warning: Deadlock detected.\n".getBytes(StandardCharsets.UTF_8));
                }
                fileOutputStream.write('\n');

                fileOutputStream.write(data.getBytes(StandardCharsets.UTF_8));

                fileOutputStream.write(("\n\n" + "=".repeat(20) + "\n").getBytes(StandardCharsets.UTF_8));
                if (i < time - 1) {
                    Thread.sleep(3000);
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(String.format("An Error was thrown while writing file to %s.", outputFile.getAbsolutePath()), e);
        }

        openOutputFile(outputFile);

        Logger.info("Finish.");
    }

    private static int getMinecraftVM() throws URISyntaxException, MonitorException {
        String[] minecraftPackageNameList = new String[]{"net.fabricmc", "net.minecraftforge", "net.minecraft", "cpw.mods"};

        HostIdentifier hostId = new HostIdentifier((String) null);
        MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost(hostId);
        Set<Integer> jvms = monitoredHost.activeVms();

        for (Integer integer : jvms) {
            int pid = integer;

            Logger.info(String.format("Found VM with pid %s.", pid));

            MonitoredVm vm = monitoredHost.getMonitoredVm(new VmIdentifier("//" + pid + "?mode=r"), 0);
            String mainClass = MonitoredVmUtil.mainClass(vm, true);

            for (String minecraftPackageName : minecraftPackageNameList) {
                if (mainClass.startsWith(minecraftPackageName)) {
                    return pid;
                }
            }
        }

        return -1;
    }

    private static void openOutputFile(File outputFile) {
        String systemInfo = System.getProperty("os.name");
        if (systemInfo == null) {
            Logger.error("Failed to get System info.", new NullPointerException("Cannot get system info because System.getProperty(\"os.name\") is null."));
            return;
        }
        systemInfo = systemInfo.toLowerCase();
        String commandLine = null;

        if (systemInfo.startsWith("windows")) {
            commandLine = "C:\\Windows\\explorer.exe /select,\"%s\"";
        } else {
            commandLine = "open -R \"%s\"";
        }

        commandLine = String.format(commandLine, outputFile.getAbsolutePath());
        boolean isSuccess = false;

        try {
            Runtime.getRuntime().exec(commandLine);
            isSuccess = true;
        } catch (Throwable e) {
            Logger.error(String.format("An Error was thrown while creating process with the command line \"%s\".", commandLine), e);
        }

        if (isSuccess) {
            Logger.info(String.format("Success to create process with the command line \"%s\".", commandLine));
        }
    }

    private static File getCurrentOutputFile() {
        File outputDir = new File(System.getProperty("user.dir"), "dumps");
        if (outputDir.isFile()) {
            throw new SecurityException(String.format("Output Directory \"%s\" is a file.", outputDir.getAbsolutePath()));
        }
        if (!outputDir.exists()) {
            if (!outputDir.mkdir()) {
                throw new SecurityException(String.format("Failed to create directory \"%s\".", outputDir.getAbsolutePath()));
            }
        }
        return new File(outputDir, String.format("minecraft-jstack-dump-%s.txt", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd-hh-mm-ss")
        )));
    }

    private static String getDataDumpHead() {
        return String.format("Minecraft JStack Dump at [%s]\n", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy:MM:dd:hh:mm:ss")
        ));
    }

    private static String getDataDump(int pid) {
        try {
            VirtualMachine vm = VirtualMachine.attach(String.valueOf(pid));
            InputStream in = ((HotSpotVirtualMachine) vm).remoteDataDump("-e -l");
            String dataDump = new String(in.readAllBytes());
            in.close();
            vm.detach();
            return dataDump;
        } catch (Throwable e) {
            Logger.error(String.format("An Error was thrown while attaching VM with pid %s", pid), e);
            return null;
        }
    }
}
