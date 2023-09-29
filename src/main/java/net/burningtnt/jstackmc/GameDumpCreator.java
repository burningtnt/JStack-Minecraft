package net.burningtnt.jstackmc;

import net.burningtnt.tdparser.IThreadDumpAnalyzedResult;
import net.burningtnt.tdparser.ThreadDumpAnalyzer;
import net.burningtnt.tdparser.infos.ThreadInfo;
import net.burningtnt.tdparser.infos.ThreadStateInfo;
import net.burningtnt.tdparser.ThreadDumpReport;
import net.burningtnt.tdparser.stacktrace.AbstractStackTraceElement;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class GameDumpCreator {
    private static final Pattern ACCESS_TOKEN_HIDER = Pattern.compile("--accessToken [0-9a-f]*");

    private static final int TOOL_VERSION = 9;

    private final int dumpTime;

    private final List<Map.Entry<String, IThreadDumpAnalyzedResult>> results;

    private GameDumpCreator(int dumpTime) {
        this.dumpTime = dumpTime;
        this.results = new ArrayList<>(dumpTime);
    }

    public static GameDumpCreator of(int dumpTime) {
        return new GameDumpCreator(dumpTime);
    }

    public void writeDumpTo(JVMCommandExecutor executor, Writer writer) throws IOException, InterruptedException {
        writeDumpHeadTo(executor, writer);

        writer.write('\n');

        for (int i = 0; i < this.dumpTime; i++) {
            generateDump(executor);

            if (i < this.dumpTime - 1) {
                Thread.sleep(3000);
            }
        }

        writeAnalysisTo(writer);
        writer.write('\n');
        for (int i = 0; i < this.dumpTime; i++) {
            writeDumpBodyTo(this.results.get(i).getKey(), writer);

            if (i < this.dumpTime - 1) {
                writer.write("====================\n");
            }
        }
    }

    private void writeDumpHeadTo(JVMCommandExecutor executor, Writer writer) throws IOException {
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

    public void writeKeyValueTo(String key, String value, Writer writer, boolean multilineMode) throws IOException {
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

    private void generateDump(JVMCommandExecutor executor) throws IOException {
        String result = executor.executeCommandAsString("Thread.print");
        this.results.add(Map.entry(result, ThreadDumpAnalyzer.salfeAnalyze(result)));
    }

    private void writeAnalysisTo(Writer writer) throws IOException {
        writer.write("Analysis:\n");
        List<ThreadDumpReport> reports = new ArrayList<>(this.dumpTime);
        for (int i = 0; i < this.dumpTime; i++) {
            IThreadDumpAnalyzedResult result = this.results.get(i).getValue();
            if (!result.success() || result.getResult() == null) {
                PrintWriter pw = new PrintWriter(writer);
                writer.write("Cannot analyze the report because one / some of the thread dump is invalid.\n");
                printException(i, result, pw, writer);
                for (i++; i < this.dumpTime; i++) {
                    result = this.results.get(i).getValue();
                    if (!result.success()) {
                        printException(i, result, pw, writer);
                    }
                }
                writer.write('\n');
                return;
            }

            reports.add(result.getResult());
        }

        for (String threadName : new String[]{"Client Thread", "Server Thread", "Render Thread", "JobScheduler FJ pool 0/3"}) {
            ThreadInfo first = reports.get(0).lookupThread(threadName);
            if (first == null) {
                continue;
            }
            if (first.state() != ThreadStateInfo.RUNNABLE) {
                writer.write(String.format("Thread '%s' is stuck!\n", threadName));
                continue;
            }

            boolean isStuck = true;
            for (int i = 1; i < this.dumpTime; i++) {
                ThreadInfo ti = reports.get(i).lookupThread(threadName);
                if (ti == null) {
                    break;
                }
                if (ti.state() != ThreadStateInfo.RUNNABLE) {
                    break;
                }

                List<AbstractStackTraceElement> fst = first.stackTraceElements();
                List<AbstractStackTraceElement> cst = ti.stackTraceElements();

                if (fst == null || fst.isEmpty() || cst == null || cst.isEmpty()) {
                    break;
                }

                if (!fst.get(0).equals(cst.get(0))) {
                    isStuck = false;
                    break;
                }
            }
            if (isStuck) {
                writer.write(String.format("Thread '%s' is stuck!\n", threadName));
            }
        }
    }

    private static void printException(int index, IThreadDumpAnalyzedResult result, PrintWriter pw, Writer writer) throws IOException {
        writer.write('#');
        writer.write(Integer.toString(index));
        writer.write(": ");
        if (result.getException() == null) {
            writer.write("The result is in the failed status, however, the exception is null.");
        } else {
            result.getException().printStackTrace(pw);
            if (pw.checkError()) {
                throw new IOException("An exception is thrown while printing data into the PrintWriter.");
            }
        }
    }

    private static void writeDumpBodyTo(String result, Writer writer) throws IOException {
        writer.write(result);
    }
}
