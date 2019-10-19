package com.github.rahmnathan.localmovie.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.temporal.ChronoUnit;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Duration {
    private ChronoUnit unit;
    private long value;
}
