package openmods.dropdebug;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Log {
    private Log() {}

    private static final Logger logger;

    static {
        logger = LogManager.getLogger("DebugDrops");
    }

    public static void log(Level level, Throwable ex, String format, Object... data) {
        logger.log(level, String.format(format, data), ex);
    }

    public static void log(Level level, String format, Object... data) {
        logger.log(level, String.format(format, data));
    }

    public static void severe(String format, Object... data) {
        log(Level.ERROR, format, data);
    }

    public static void severe(Throwable ex, String format, Object... data) {
        log(Level.ERROR, ex, format, data);
    }

    public static void warn(String format, Object... data) {
        log(Level.WARN, format, data);
    }

    public static void warn(Throwable ex, String format, Object... data) {
        log(Level.WARN, ex, format, data);
    }

    public static void info(String format, Object... data) {
        log(Level.INFO, format, data);
    }

    public static void info(Throwable ex, String format, Object... data) {
        log(Level.INFO, ex, format, data);
    }

    public static void debug(String format, Object... data) {
        log(Level.DEBUG, format, data);
    }

    public static void trace(String format, Object... data) {
        log(Level.TRACE, format, data);
    }

}
