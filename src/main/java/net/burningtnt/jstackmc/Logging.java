/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2021  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.burningtnt.jstackmc;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.*;

public final class Logging {
    private Logging() {
    }

    private static final Logger LOGGER = Logger.getLogger("JStack Minecraft");
    private static final ByteArrayOutputStream storedLogs = new ByteArrayOutputStream(8 * 1024);
    private static final Formatter formatter = new Formatter() {
        @Override
        public String format(LogRecord record) {
            return record.getMessage();
        }
    };

    public static Logger getLogger() {
        return LOGGER;
    }

    static {
        LOGGER.setLevel(Level.ALL);
        LOGGER.setUseParentHandlers(false);
        LOGGER.setFilter(record -> {
            record.setMessage(format(record));
            return true;
        });

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatter);
        consoleHandler.setLevel(Level.FINER);
        LOGGER.addHandler(consoleHandler);

        StreamHandler streamHandler = new StreamHandler(storedLogs, formatter) {
            @Override
            public synchronized void publish(LogRecord record) {
                super.publish(record);
                flush();
            }
        };
        try {
            streamHandler.setEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        streamHandler.setLevel(Level.ALL);
        LOGGER.addHandler(streamHandler);
    }

    private static final MessageFormat FORMAT = new MessageFormat("[{0,date,HH:mm:ss}] [{1}.{2}/{3}] {4}\n");

    private static String format(LogRecord record) {
        Throwable thrown = record.getThrown();

        StringWriter writer;
        StringBuffer buffer;
        if (thrown == null) {
            writer = null;
            buffer = new StringBuffer(256);
        } else {
            writer = new StringWriter(1024);
            buffer = writer.getBuffer();
        }

        FORMAT.format(new Object[]{
                new Date(record.getMillis()),
                record.getSourceClassName(), record.getSourceMethodName(), record.getLevel().getName(),
                record.getMessage()
        }, buffer, null);

        if (thrown != null) {
            try (PrintWriter printWriter = new PrintWriter(writer)) {
                thrown.printStackTrace(printWriter);
            }
        }
        return buffer.toString();
    }
}
