package net.burningtnt.jstackmc;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class Logger {
    private Logger() {
    }

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss");

    public static void info(String text) {
        System.out.printf(
                "[%s] [Minecraft JStack/INFO]: %s\n", LocalTime.now().format(timeFormatter), text
        );
    }

    public static void error(String text, Throwable e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        String res = stringWriter.toString();
        try {
            stringWriter.close();
        } catch (IOException ignored) {
        }
        System.out.printf(
                "[%s] [Minecraft JStack/ERROR]: %s\n%s\n", LocalTime.now().format(timeFormatter), text, res
        );
    }
}
