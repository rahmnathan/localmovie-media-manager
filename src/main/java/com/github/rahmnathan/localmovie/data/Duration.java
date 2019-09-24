package com.github.rahmnathan.localmovie.data;

import java.time.temporal.ChronoUnit;

public class Duration {
    private ChronoUnit unit;
    private long value;

    public Duration() {
    }

    public Duration(ChronoUnit unit, long value) {
        this.unit = unit;
        this.value = value;
    }

    public void setUnit(ChronoUnit unit) {
        this.unit = unit;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public ChronoUnit getUnit() {
        return unit;
    }

    public long getValue() {
        return value;
    }
}
