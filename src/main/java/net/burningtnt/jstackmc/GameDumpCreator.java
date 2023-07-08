package net.burningtnt.jstackmc;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class GameDumpCreator {
    private static final Pattern ACCESS_TOKEN_HIDER = Pattern.compile("--accessToken [0-9a-f]*");

    private static final int TOOL_VERSION = 8;

    private static final int DUMP_TIME = 3;

    private GameDumpCreator() {
    }

    private static final class DumpHead {
        private final Map<String, String> infos = new LinkedHashMap<>();

        public void push(String key, String value) {
            infos.put(key, value);
        }

        public void writeTo(PrintWriter printWriter) {
            printWriter.write("===== Minecraft JStack Dump =====\n");
            for (Map.Entry<String, String> entry : infos.entrySet()) {
                printWriter.write(entry.getKey());
                printWriter.write(": ");

                if (entry.getValue().contains("\n")) {
                    // Multiple Line Value
                    printWriter.write('{');
                    printWriter.write('\n');

                    String[] lines = entry.getValue().split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        if (i != lines.length - 1) {
                            printWriter.write("    ");
                            printWriter.write(lines[i]);
                            printWriter.write('\n');
                        } else {
                            // Last line
                            if (lines[i].length() == 0) {
                                // An empty last Line
                                printWriter.write('}');
                            } else {
                                // Not an empty last lien
                                printWriter.write("    ");
                                printWriter.write(lines[i]);
                                printWriter.write('\n');
                                printWriter.write('}');
                            }
                        }
                    }
                } else {
                    // Single Line Value
                    printWriter.write(entry.getValue());
                }
                printWriter.write('\n');
            }
            printWriter.write('\n');
            printWriter.write('\n');
        }
    }

    public static void writeDumpTo(long pid, Path path) throws IOException, InterruptedException {
        try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(path), false)) {
            writeDumpHeadTo(pid, printWriter);

            for (int i = 0; i < DUMP_TIME; i++) {
                writeDumpBodyTo(pid, printWriter);
                printWriter.write("====================\n");

                if (i < DUMP_TIME - 1) {
                    Thread.sleep(3000);
                }
            }
        }
    }

    private static void writeDumpHeadTo(long lvmid, PrintWriter printWriter) {
        DumpHead dumpHead = new DumpHead();
        dumpHead.push("Tool Version", String.valueOf(TOOL_VERSION));
        dumpHead.push("VM PID", String.valueOf(lvmid));
        {
            StringBuilder stringBuilder = new StringBuilder();
            Attach.attachVM(lvmid, "VM.command_line", stringBuilder);
            dumpHead.push("VM Command Line", ACCESS_TOKEN_HIDER.matcher(stringBuilder).replaceAll("--accessToken <access token>"));
        }
        {
            StringBuilder stringBuilder = new StringBuilder();
            Attach.attachVM(lvmid, "VM.version", stringBuilder);
            dumpHead.push("VM Version", stringBuilder.toString());
        }

        dumpHead.writeTo(printWriter);
    }

    private static void writeDumpBodyTo(long lvmid, PrintWriter printWriter) {
        Attach.attachVM(lvmid, "Thread.print", printWriter);
    }
}
