package me.evisual.arsenic.core.log;

public interface LogAdapter {
    void info(String message);

    void warn(String message);

    void error(String message, Throwable throwable);
}
