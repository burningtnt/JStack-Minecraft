package net.burningtnt.jstackmc;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

public final class GameDumpCreator {
    private static final Pattern ACCESS_TOKEN_HIDER = Pattern.compile("--accessToken [0-9a-f]*");

    private static final int TOOL_VERSION = 9;

    private GameDumpCreator() {
    }

    public static void writeDumpTo(JVMCommandExecutor executor, Writer writer, int dumpTime) throws IOException, InterruptedException {
        writeDumpHeadTo(executor, writer);

        writer.write('\n');

        for (int i = 0; i < dumpTime; i++) {
            writeDumpBodyTo(executor, writer);
            writer.write("====================\n");

            if (i < dumpTime - 1) {
                Thread.sleep(3000);
            }
        }
    }

    private static void writeDumpHeadTo(JVMCommandExecutor executor, Writer writer) throws IOException {
        writer.write("===== Minecraft JStack Dump =====\n");
        writeKeyValueTo("Tool Version", String.valueOf(TOOL_VERSION), writer, false);
        writeKeyValueTo("VM PID", executor.virtualMachine.id(), writer, false);
        writeKeyValueTo(
                "VM Command Line",
                ACCESS_TOKEN_HIDER.matcher(executor.executeCommandAsCharSequence("VM.command_line")).replaceAll("--accessToken <access token>"),
                writer,
                true
        );
        writeKeyValueTo(
                "VM Version",
                executor.executeCommandAsCharSequence("VM.version").toString(),
                writer,
                true
        );
    }

    public static void writeKeyValueTo(String key, String value, Writer writer, boolean multilineMode) throws IOException {
        writer.write(key);
        writer.write(':');
        writer.write(' ');

        if (multilineMode) {
            writer.write('{');
            writer.write('\n');

            String[] lines = value.split("\n");

            for (int i = 0; i < lines.length; i++) {
                if (i != lines.length - 1) {
                    writer.write("    ");
                    writer.write(lines[i]);
                    writer.write('\n');
                } else {
                    if (lines[i].length() == 0) {
                        writer.write('}');
                    } else {
                        writer.write("    ");
                        writer.write(lines[i]);
                        writer.write('\n');
                        writer.write('}');
                    }
                }
            }
        } else {
            writer.write(value);
        }
        writer.write('\n');
    }

    private static void writeDumpBodyTo(JVMCommandExecutor executor, Writer writer) throws IOException {
        writer.write(executor.executeCommandAsString("Thread.print"));
    }
}
