package org.nerdola.capycode.compiler;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public static void log(String level, String message, int line, int column) {
        String timestamp = "[" + timeFormat.format(new Date()) + "]";
        String levelTag = "[" + level.toLowerCase() + "]";
        String posInfo = (line > 0 && column > 0) ? " at " + line + ":" + column : "";
        System.err.println(String.format("%s%s (%s%s)", timestamp, levelTag, message, posInfo));
    }

    public static void info(String message, int line, int column) {
        log("info", message, line, column);
    }

    public static void warning(String message, int line, int column) {
        log("warning", message, line, column);
    }

    public static void fatal(String message, int line, int column) {
        log("fatal", message, line, column);
        throw new RuntimeException("Fatal error: " + message + " at " + line + ":" + column);
    }
}
