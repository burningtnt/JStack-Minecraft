package net.burningtnt.jstackmc;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/*
 JVM:
 cd "release/Jstack Minecraft"&jlink --add-modules java.base,jdk.attach,jdk.internal.jvmstat,jdk.attach,java.instrument --strip-debug --no-man-pages --no-header-files --compress=2 --output jre_Windows
 */

public final class Main {
    private static final int DEFAULT_RETRY_TIME = 3;

    private static final int DEFAULT_DUMP_TIME = 5;

    private Main() {
    }

    private static int getSystemPropertyAsInt(String key, int defaultValue) {
        String value = System.getProperty(key);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    public static void main(String[] args) throws AttachNotSupportedException, IOException, InterruptedException {
        int retryTime = getSystemPropertyAsInt("jstackmc.attach.retrytime", DEFAULT_RETRY_TIME);
        int dumpTime = getSystemPropertyAsInt("jstackmc.threaddump.time", DEFAULT_DUMP_TIME);

        Path output = Path.of("dumps", String.format("minecraft-jstack-dump-%s.txt", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd-hh-mm-ss")
        ))).toAbsolutePath();

        if (!Files.exists(output.getParent())) {
            Files.createDirectories(output.getParent());
        }

        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            JVMCommandExecutor executor;
            try {
                executor = JVMCommandExecutor.of(descriptor, retryTime);
            } catch (IOException e) {
                Logging.getLogger().log(Level.WARNING, String.format("Cannot attach VM %s.", descriptor.id()), e);
                continue;
            }

            try {
                if (JVMDescriptor.isCurrentJVM(executor)) {
                    continue;
                }
                if (!JVMDescriptor.isMinecraftJVM(executor)) {
                    continue;
                }

                Logging.getLogger().log(Level.INFO, String.format("Find Minecraft VM with id %s.", executor.virtualMachine.id()));

                try (Writer writer = Files.newBufferedWriter(output)) {
                    GameDumpCreator.writeDumpTo(executor, writer, dumpTime);
                }
            } finally {
                executor.close();
            }

            openOutputFile(output);

            return;
        }

        Logging.getLogger().log(Level.INFO, "No Minecraft VM detected.");
        throw new AttachNotSupportedException("No Minecraft VM detected");
    }

    private static void openOutputFile(Path output) throws IOException {
        String systemInfo = System.getProperty("os.name");
        if (systemInfo == null) {
            throw new UnsupportedOperationException(String.format("Cannot get system info because System.getProperty(\"os.name\") is null. Please manually open file at %s.", output.toString()));
        }


        new ProcessBuilder(
                systemInfo.toLowerCase().startsWith("windows") ?
                        new String[]{"C:\\Windows\\explorer.exe", "/select,", output.toString()} :
                        new String[]{"/usr/bin/open", "-R", output.toString()}
        ).inheritIO().start();
    }
}
