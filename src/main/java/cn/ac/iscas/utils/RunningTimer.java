package cn.ac.iscas.utils;

public class RunningTimer {

    public enum TimeType {
        NS,
        MS
    }

    private long timePre;
    private TimeType timeType;

    public RunningTimer(TimeType timeType) {
        this.timeType = timeType;
        start();
    }

    public long start() {
        if (timeType.equals(TimeType.NS)) {
            timePre = System.nanoTime();
        } else if (timeType.equals(TimeType.MS)) {
            timePre = System.currentTimeMillis();
        }

        return timePre;
    }

    public long cut() {
        long interval = 0l;
        if (timeType.equals(TimeType.NS)) {
            interval = System.nanoTime() - timePre;
            timePre = System.nanoTime();
        } else if (timeType.equals(TimeType.MS)) {
            interval = System.currentTimeMillis() - timePre;
            timePre = System.currentTimeMillis();
        }

        return interval;
    }
}
