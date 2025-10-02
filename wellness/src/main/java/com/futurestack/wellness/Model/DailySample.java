package com.futurestack.wellness.Model;

import java.time.LocalDate;

public record DailySample(

        LocalDate date,
        double activeEnergyKcal,
        double exerciseMin,
        double standHourCount,      // “Apple Stand Hour” (count of hours hit)
        double standMinutes,        // “Apple Stand Time” (min)
        Double spo2Pct,
        Double envAudioDbA,
        double flightsClimbed,
        Integer hrMin,
        Integer hrMax,
        Integer hrAvg,
        Double hrvMs,
        Double physicalEffortKcalPerHrPerKg,
        double restingEnergyKcal,
        Integer restingHr,
        Double stairDownFtPerSec,
        Double stairUpFtPerSec,
        long steps,
        double distanceMi,
        Double walkAsymPct,
        Double walkDoubleSupportPct,
        Integer walkHrAvg,
        Double walkSpeedMph,
        Double walkStepLenIn
)
{}

