package edu.vuamsterdam.MinimalConcepts;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GhettoLogger {
    private static void log(String type, String data){
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        System.out.printf("{\"time\": \"%s\", \"type\": \"%s\", \"data\": %s}%n", now, type, data);
    }
    public static void logStart(String file) {
        log("Start", String.format("{\"filename\": \"%s\"}", file));
    }
    public static void logFinish(String file, long durationMillis, int count) {
        log("Finish", String.format("{\"filename\": \"%s\", \"durationMillis\": %d, \"count\": %d}", file, durationMillis, count));
    }
    public static void logMinimize(long durationMillis, String expression, int origSize) {
        log("Minimize", String.format("{\"durationMillis\": %d, \"expression\": \"%s\", \"origSize\": %d, \"success\": false, \"new\": null, \"newSize\": null}", durationMillis, expression, origSize));
    }
    public static void logMinimize(long durationMillis, String expression, int origSize, String newExpression, int newSize) {
        log("Minimize", String.format("{\"durationMillis\": %d, \"expression\": \"%s\", \"origSize\": %d, \"success\": true, \"new\": \"%s\", \"newSize\": %d}", durationMillis,expression, origSize, newExpression, newSize));
    }
    public static void logTimeout(String expression) {
        log("Timeout", String.format("{\"expression\": \"%s\"}", expression));
    }
    public static void logCrashed(String expression, boolean bugged, boolean feasible) {
        log("Crashed", String.format("{\"expression\": \"%s\", \"bugged\": %b, \"feasible\": %b}", expression, bugged, feasible));
    }
    public static void logHardCrashed(String filepath, String error) {
        log("HardCrashed", String.format("{\"filename\": \"%s\", \"error\": \"%s\"}", filepath, error));
    }
    public static void logSize(String filepath, int sizeTBox, int nrMinimizable) {
        log("Size", String.format("{\"filename\": \"%s\", \"sizeTBox\": %d, \"minimizable\": %d}", filepath, sizeTBox, nrMinimizable));
    }
}
