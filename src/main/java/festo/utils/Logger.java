package festo.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public static void log(String message) {
        System.out.println("[" + sdf.format(new Date()) + "] " + message);
    }

    public static void error(String message) {
        System.err.println("[" + sdf.format(new Date()) + "] ERROR: " + message);
    }

    public static void warn(String message) {
        System.out.println("[" + sdf.format(new Date()) + "] WARN: " + message);
    }
}