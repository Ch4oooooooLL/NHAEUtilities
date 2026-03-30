package com.github.nhaeutilities.modules.superwirelesskit.runtime;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.relauncher.FMLInjectionData;

public final class SuperWirelessDebugLog {

    public interface DebugSink extends Closeable {

        void write(String line);

        @Override
        void close();
    }

    private static final Logger LOGGER = LogManager.getLogger("NHAEUtilities-SuperWirelessKit-Debug");
    private static final DebugSink NO_OP_SINK = new DebugSink() {

        @Override
        public void write(String line) {}

        @Override
        public void close() {}
    };
    private static final String LOG_FILE_NAME = "superwirelesskit-debug.log";

    private static volatile boolean enabled;
    private static volatile DebugSink installedTestSink;
    private static volatile DebugSink activeSink = NO_OP_SINK;
    private static volatile File resolvedLogFile;

    private SuperWirelessDebugLog() {}

    public static synchronized void configure(boolean debugEnabled) {
        enabled = debugEnabled;
        resolvedLogFile = resolveDefaultLogFile();
        reopenSink();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void log(String event, String pattern, Object... args) {
        if (!enabled) {
            return;
        }

        String message = args == null || args.length == 0 ? pattern : String.format(Locale.ROOT, pattern, args);
        try {
            activeSink.write(formatLine(event, message));
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to write SuperWirelessKit debug log line for event {}", event, e);
        }
    }

    public static synchronized void installSinkForTests(DebugSink sink) {
        installedTestSink = sink;
        reopenSink();
    }

    public static synchronized File getResolvedLogFileForTests() {
        return resolvedLogFile != null ? resolvedLogFile : resolveDefaultLogFile();
    }

    public static synchronized void closeForTests() {
        closeSink(activeSink);
        activeSink = NO_OP_SINK;
    }

    public static synchronized void resetForTests() {
        enabled = false;
        installedTestSink = null;
        closeSink(activeSink);
        activeSink = NO_OP_SINK;
        resolvedLogFile = null;
    }

    private static synchronized void reopenSink() {
        closeSink(activeSink);
        activeSink = NO_OP_SINK;

        if (!enabled) {
            return;
        }

        if (installedTestSink != null) {
            activeSink = installedTestSink;
            return;
        }

        activeSink = new FileDebugSink(getResolvedLogFileForTests());
    }

    private static String formatLine(String event, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT).format(new Date());
        return timestamp + " [" + event + "] " + message;
    }

    private static File resolveDefaultLogFile() {
        return new File(new File(resolveMinecraftHome(), "logs"), LOG_FILE_NAME);
    }

    private static File resolveMinecraftHome() {
        try {
            Field field = FMLInjectionData.class.getDeclaredField("minecraftHome");
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof File) {
                return (File) value;
            }
        } catch (ReflectiveOperationException ignored) {}
        return new File(".");
    }

    private static void closeSink(DebugSink sink) {
        if (sink == null || sink == NO_OP_SINK || sink == installedTestSink) {
            return;
        }
        try {
            sink.close();
        } catch (RuntimeException ignored) {}
    }

    private static final class FileDebugSink implements DebugSink {

        private final File logFile;
        private Writer writer;

        private FileDebugSink(File logFile) {
            this.logFile = logFile;
        }

        @Override
        public synchronized void write(String line) {
            try {
                ensureWriter();
                writer.write(line);
                writer.write(System.lineSeparator());
                writer.flush();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to write SuperWirelessKit debug log", e);
            }
        }

        @Override
        public synchronized void close() {
            if (writer == null) {
                return;
            }
            try {
                writer.close();
            } catch (IOException ignored) {} finally {
                writer = null;
            }
        }

        private void ensureWriter() throws IOException {
            if (writer != null) {
                return;
            }

            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Failed to create directory " + parent.getAbsolutePath());
            }

            writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8));
        }
    }
}
