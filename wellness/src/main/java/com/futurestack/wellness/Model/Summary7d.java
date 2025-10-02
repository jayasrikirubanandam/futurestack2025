package com.futurestack.wellness.Model;

public record Summary7d(
        // Activity & energy
        double totalActiveEnergyKcal,
        double totalRestingEnergyKcal,
        double avgExerciseMinPerDay,
        double totalStandHours,      // standMinutes / 60 across 7 days
        int    standGoalDays,        // days Stand Hour >= 12
        int    moveGoalDays,         // days Active Energy >= 500 kcal (tunable)
        double totalDistanceMi,
        long   totalSteps,

        // Heart & recovery
        Integer hrAvg,
        Integer hrMin,
        Integer hrMax,
        Integer restingHrAvg,
        Integer restingHrMin,
        Integer restingHrMax,
        Double  hrvMedianMs,
        Integer walkHrAvg,

        // Gait & stairs
        Double  walkSpeedAvgMph,
        Double  stepLenAvgIn,
        Double  doubleSupportAvgPct,
        Double  asymmetryAvgPct,
        Double  stairUpAvgFtPerSec,
        Double  stairDownAvgFtPerSec,

        // Environment & SpO2
        Double  envAudioAvgDbA,
        Double  spo2AvgPct,
        Double  spo2MinPct)
{}
