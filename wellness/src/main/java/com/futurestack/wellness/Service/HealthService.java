package com.futurestack.wellness.Service;


import com.futurestack.wellness.Model.DailySample;
import com.futurestack.wellness.Model.Summary7d;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HealthService {
    private Summary7d latest;

    public Summary7d getLatest() { return latest; }

    /** Upload the new HAE CSV and compute a 7-day summary from the whitelisted fields. */
    public Summary7d uploadAndSummarize(MultipartFile file) throws Exception {
        // 1) Parse CSV (headers from first row)
        CSVFormat format = CSVFormat.Builder.create().setHeader().setSkipHeaderRecord(true).build();
        List<Map<String,String>> rows = new ArrayList<>();

        try (Reader r = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            var parser = format.parse(r);
            Map<String,Integer> head = parser.getHeaderMap().entrySet().stream()
                    .collect(Collectors.toMap(e -> norm(e.getKey()), Map.Entry::getValue));

            // whitelist headers (normalized)
            String DATE   = req(head, "date");
            String AE     = req(head, "active energy");                  // (kcal)
            String EX     = req(head, "apple exercise time");            // (min)
            String SHOUR  = req(head, "apple stand hour");               // (count)
            String STIME  = req(head, "apple stand time");               // (min)
            String SPO2   = req(head, "blood oxygen saturation (% )", "blood oxygen saturation (%)");
            String AUDIO  = req(head, "environmental audio exposure (dbaspl)");
            String FLIGHTS= req(head, "flights climbed (count)");
            String HRMIN  = req(head, "heart rate [min] (count/min)");
            String HRMAX  = req(head, "heart rate [max] (count/min)");
            String HRAVG  = req(head, "heart rate [avg] (count/min)");
            String HRV    = req(head, "heart rate variability (ms)");
            String PEFF   = any(head, "physical effort (kcal/hr", "physical effort (kcal/hr"); // tolerate odd symbol
            String RENG   = req(head, "resting energy (kcal)");
            String RHR    = req(head, "resting heart rate (count/min)");
            String STAIRD = req(head, "stair speed: down (ft/s)");
            String STAIRU = req(head, "stair speed: up (ft/s)");
            String STEPS  = req(head, "step count (count)");
            String DIST   = req(head, "walking + running distance (mi)");
            String ASYM   = req(head, "walking asymmetry percentage (%)");
            String DSUPP  = req(head, "walking double support percentage (%)");
            String WHRAVG = req(head, "walking heart rate average (count/min)");
            String WSPEED = req(head, "walking speed (mi/hr)");
            String WSTEP  = req(head, "walking step length (in)");

            for (CSVRecord rec : parser) {
                Map<String,String> m = new HashMap<>();
                put(m,"date", rec, head, DATE);
                put(m,"activeEnergy", rec, head, AE);
                put(m,"exerciseMin",  rec, head, EX);
                put(m,"standHour",    rec, head, SHOUR);
                put(m,"standMin",     rec, head, STIME);
                put(m,"spo2",         rec, head, SPO2);
                put(m,"audioDb",      rec, head, AUDIO);
                put(m,"flights",      rec, head, FLIGHTS);
                put(m,"hrMin",        rec, head, HRMIN);
                put(m,"hrMax",        rec, head, HRMAX);
                put(m,"hrAvg",        rec, head, HRAVG);
                put(m,"hrvMs",        rec, head, HRV);
                put(m,"physEff",      rec, head, PEFF);
                put(m,"restEnergy",   rec, head, RENG);
                put(m,"restingHr",    rec, head, RHR);
                put(m,"stairDown",    rec, head, STAIRD);
                put(m,"stairUp",      rec, head, STAIRU);
                put(m,"steps",        rec, head, STEPS);
                put(m,"distanceMi",   rec, head, DIST);
                put(m,"asymPct",      rec, head, ASYM);
                put(m,"doubleSupp",   rec, head, DSUPP);
                put(m,"walkHrAvg",    rec, head, WHRAVG);
                put(m,"walkSpeed",    rec, head, WSPEED);
                put(m,"stepLenIn",    rec, head, WSTEP);
                rows.add(m);
            }
        }

        // 2) Collapse by date (sum/avg as needed)
        Map<LocalDate, List<DailySample>> byDate = rows.stream()
                .collect(Collectors.groupingBy(m -> parseDate(m.get("date"))))
                .entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> toDailySamples(e.getValue())
                ));

        // 3) Last 7 days window
        List<LocalDate> days = new ArrayList<>(byDate.keySet());
        Collections.sort(days);
        if (days.isEmpty()) throw new IllegalArgumentException("No data rows.");
        LocalDate max = days.get(days.size()-1);
        LocalDate from = max.minusDays(6);
        days = days.stream().filter(d -> !d.isBefore(from)).toList();

        // 4) Reduce into 7-day summary
        double totalAE=0, totalRE=0, dist=0, standMinSum=0;
        long stepsSum=0;
        int standGoalDays=0, moveGoalDays=0;
        List<Integer> hrAvgL = new ArrayList<>(), hrMinL = new ArrayList<>(), hrMaxL = new ArrayList<>(),
                rhrL   = new ArrayList<>(), rhrMinL=new ArrayList<>(), rhrMaxL=new ArrayList<>(),
                walkHrL= new ArrayList<>();
        List<Double>  hrvL = new ArrayList<>(), speedL = new ArrayList<>(), stepLenL=new ArrayList<>(),
                dblSuppL=new ArrayList<>(), asymL=new ArrayList<>(), stairUpL=new ArrayList<>(),
                stairDownL=new ArrayList<>(), audioL=new ArrayList<>(), spo2L=new ArrayList<>(), spo2MinL=new ArrayList<>(),
                exMinL=new ArrayList<>();

        for (LocalDate d : days) {
            // we merged each date list into a single DailySample (see toDailySamples)
            DailySample s = byDate.get(d).get(0);

            totalAE += s.activeEnergyKcal();
            totalRE += s.restingEnergyKcal();
            dist    += s.distanceMi();
            stepsSum+= s.steps();
            standMinSum += s.standMinutes();

            if (s.standHourCount() >= 12) standGoalDays++;
            if (s.activeEnergyKcal() >= 500) moveGoalDays++;

            if (s.hrAvg() != null) hrAvgL.add(s.hrAvg());
            if (s.hrMin() != null) hrMinL.add(s.hrMin());
            if (s.hrMax() != null) hrMaxL.add(s.hrMax());

            if (s.restingHr() != null) rhrL.add(s.restingHr());
            if (s.restingHr() != null) { // reusing same as min/max if multiple sources existed
                rhrMinL.add(s.restingHr());
                rhrMaxL.add(s.restingHr());
            }

            if (s.walkHrAvg() != null) walkHrL.add(s.walkHrAvg());
            if (s.hrvMs() != null) hrvL.add(s.hrvMs());
            if (s.walkSpeedMph() != null) speedL.add(s.walkSpeedMph());
            if (s.walkStepLenIn() != null) stepLenL.add(s.walkStepLenIn());
            if (s.walkDoubleSupportPct() != null) dblSuppL.add(s.walkDoubleSupportPct());
            if (s.walkAsymPct() != null) asymL.add(s.walkAsymPct());
            if (s.stairUpFtPerSec() != null) stairUpL.add(s.stairUpFtPerSec());
            if (s.stairDownFtPerSec() != null) stairDownL.add(s.stairDownFtPerSec());
            if (s.envAudioDbA() != null) audioL.add(s.envAudioDbA());
            if (s.spo2Pct() != null) { spo2L.add(s.spo2Pct()); spo2MinL.add(s.spo2Pct()); }
            exMinL.add(s.exerciseMin());
        }

        int n = Math.max(days.size(), 1);
        Summary7d summary = new Summary7d(
                round2(totalAE),
                round2(totalRE),
                round2(avg(exMinL)),
                round2(standMinSum / 60.0),
                standGoalDays,
                moveGoalDays,
                round2(dist),
                stepsSum,

                avgInt(hrAvgL),
                minInt(hrMinL),
                maxInt(hrMaxL),
                avgInt(rhrL),
                minInt(rhrMinL),
                maxInt(rhrMaxL),
                median(hrvL),
                avgInt(walkHrL),

                avgD(speedL),
                avgD(stepLenL),
                avgD(dblSuppL),
                avgD(asymL),
                avgD(stairUpL),
                avgD(stairDownL),

                avgD(audioL),
                avgD(spo2L),
                minD(spo2MinL)
        );

        latest = summary;
        return summary;
    }

    // ---- merge multiple raw rows of same date into ONE DailySample (sum/avg logic) ----
    private List<DailySample> toDailySamples(List<Map<String,String>> raw) {
        // sums
        double ae=0, re=0, dist=0, standMin=0, flights=0;
        long steps=0;
        // “goals” inputs
        double standHour=0;
        // avgs (non-null only)
        List<Integer> hrMinL = new ArrayList<>(), hrMaxL = new ArrayList<>(), hrAvgL = new ArrayList<>(), rhrL = new ArrayList<>(), walkHrL = new ArrayList<>();
        List<Double> hrvL = new ArrayList<>(), spo2L = new ArrayList<>(), audioL=new ArrayList<>(), peffL=new ArrayList<>(),
                stairU=new ArrayList<>(), stairD=new ArrayList<>(), speedL=new ArrayList<>(), stepLenL=new ArrayList<>(),
                dblSuppL=new ArrayList<>(), asymL=new ArrayList<>(), exMinL=new ArrayList<>();

        LocalDate date = null;

        for (var m : raw) {
            if (date == null) date = parseDate(m.get("date"));

            ae      += safeDouble(m.get("activeEnergy"));
            re      += safeDouble(m.get("restEnergy"));
            dist    += safeDouble(m.get("distanceMi"));
            steps   += safeLong(m.get("steps"));
            standMin+= safeDouble(m.get("standMin"));
            flights += safeDouble(m.get("flights"));

            standHour += safeDouble(m.get("standHour"));
            pushInt(hrMinL, m.get("hrMin"));
            pushInt(hrMaxL, m.get("hrMax"));
            pushInt(hrAvgL, m.get("hrAvg"));
            pushInt(rhrL,   m.get("restingHr"));
            pushInt(walkHrL,m.get("walkHrAvg"));
            pushD(hrvL,     m.get("hrvMs"));
            pushD(spo2L,    m.get("spo2"));
            pushD(audioL,   m.get("audioDb"));
            pushD(peffL,    m.get("physEff"));
            pushD(stairU,   m.get("stairUp"));
            pushD(stairD,   m.get("stairDown"));
            pushD(speedL,   m.get("walkSpeed"));
            pushD(stepLenL, m.get("stepLenIn"));
            pushD(dblSuppL, m.get("doubleSupp"));
            pushD(asymL,    m.get("asymPct"));
            pushD(exMinL,   m.get("exerciseMin"));
        }

        DailySample s = new DailySample(
                date,
                ae,
                avg(exMinL),                 // avg exercise minutes across records that day
                standHour,                   // count of hours (sum if multiple records report)
                standMin,
                avg(spo2L),
                avg(audioL),
                flights,
                avgInt(hrMinL),
                avgInt(hrMaxL),
                avgInt(hrAvgL),
                median(hrvL),
                avg(peffL),
                re,
                avgInt(rhrL),
                avg(stairD),
                avg(stairU),
                steps,
                dist,
                avg(asymL),
                avg(dblSuppL),
                avgInt(walkHrL),
                avg(speedL),
                avg(stepLenL)
        );

        return List.of(s);
    }

    // ---- parsing & math helpers ----
    private static String norm(String s){ return s==null? "" : s.replace("\uFEFF","").trim().toLowerCase(Locale.ROOT); }
    private static String req(Map<String,Integer> idx, String... expects){
        for (String e : expects){ String k = norm(e); if (idx.containsKey(k)) return k; }
        // loose contains match for odd symbols like kcal/hr·kg
        for (String key : idx.keySet()) for (String e : expects) if (key.contains(norm(e))) return key;
        throw new IllegalArgumentException("CSV missing required header, expected one of: "+Arrays.toString(expects));
    }
    private static String any(Map<String,Integer> idx, String... needles){
        for (String k : idx.keySet()) for (String n : needles) if (k.contains(norm(n))) return k;
        return null;
    }
    private static void put(Map<String,String> m, String outKey, CSVRecord rec, Map<String,Integer> idx, String inKey){
        if (inKey==null) return; Integer i = idx.get(inKey); if (i==null) return; m.put(outKey, rec.get(i));
    }
    private static void pushInt(List<Integer> list, String s){ if(s!=null && !s.trim().isEmpty()) list.add((int) Math.round(Double.parseDouble(s.trim()))); }
    private static void pushD(List<Double> list, String s){ if(s!=null && !s.trim().isEmpty()) list.add(Double.parseDouble(s.trim())); }

    private static double safeDouble(String s){ return (s==null || s.trim().isEmpty()) ? 0.0 : Double.parseDouble(s.trim()); }
    private static long   safeLong(String s){ return (s==null || s.trim().isEmpty()) ? 0L  : Long.parseLong(s.trim()); }

    private static Double avg(List<Double> l){ return l.isEmpty()? 0.0 : l.stream().mapToDouble(x->x).average().orElse(0.0); }
    private static Integer avgInt(List<Integer> l){ return l.isEmpty()? null : (int)Math.round(l.stream().mapToInt(i->i).average().orElse(0)); }
    private static Double median(List<Double> l){
        if (l.isEmpty()) return null; var a=new ArrayList<>(l); Collections.sort(a); int n=a.size();
        return n%2==0? (a.get(n/2-1)+a.get(n/2))/2.0 : a.get(n/2);
    }
    private static Double minD(List<Double> l){ return l.isEmpty()? null : l.stream().mapToDouble(x->x).min().orElse(Double.NaN); }
    private static Double avgD(List<Double> l){ return l.isEmpty()? null : l.stream().mapToDouble(x->x).average().orElse(Double.NaN); }
    private static Integer minInt(List<Integer> l){ return l.isEmpty()? null : l.stream().mapToInt(x->x).min().orElse(0); }
    private static Integer maxInt(List<Integer> l){ return l.isEmpty()? null : l.stream().mapToInt(x->x).max().orElse(0); }
    private static double round2(double v){ return Math.round(v*100.0)/100.0; }

    private static final DateTimeFormatter ISO   = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("M/d/yy");
    private static final DateTimeFormatter YMD_S = DateTimeFormatter.ofPattern("yyyy/M/d");
    private static LocalDate parseDate(String raw){
        String v = raw==null? "" : raw.trim();
        for (var f : List.of(ISO, SHORT, YMD_S)) try { return LocalDate.parse(v, f); } catch(Exception ignored){}
        return LocalDate.parse(v); // throw with clear msg if totally unexpected
    }

}


