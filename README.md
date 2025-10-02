# Wellness Coach – FutureStack25 Hackathon

An AI-powered **personal wellness companion** built with **Spring Boot + Cerebras LLM API**.  
The app ingests health metrics exported from the **Health Auto Export (HAE)** app, computes a 7-day summary, and generates **personalized AI coaching insights**.

---

## 🚀 Features

- **Upload CSV** exported from Health Auto Export.
- Reads **selected health parameters** only:
  - Date  
  - Active Energy (kcal)  
  - Apple Exercise Time (min)  
  - Apple Stand Hour (count)  
  - Apple Stand Time (min)  
  - Blood Oxygen Saturation (%)  
  - Environmental Audio Exposure (dBA SPL)  
  - Flights Climbed (count)  
  - Heart Rate Min / Max / Avg (bpm)  
  - Heart Rate Variability (ms)  
  - Physical Effort (kcal/hr·kg)  
  - Resting Energy (kcal)  
  - Resting Heart Rate (bpm)  
  - Stair Speed: Up / Down (ft/s)  
  - Step Count (count)  
  - Walking + Running Distance (mi)  
  - Walking Asymmetry Percentage (%)  
  - Walking Double Support Percentage (%)  
  - Walking Heart Rate Average (bpm)  
  - Walking Speed (mph)  
  - Walking Step Length (in)

- Computes a **7-day summary**:
  - Energy expenditure, steps, distance
  - Exercise & stand goals achieved
  - Heart rate, HRV, SpO₂, audio exposure
  - Gait metrics (speed, step length, asymmetry, support)
  - Stair speeds

- **Generates AI insights** using the [Cerebras LLM API](https://inference-docs.cerebras.ai):
  - Bullet summary of activity, recovery, and movement quality
  - Flags for potential concerns (e.g. low SpO₂, high resting HR, high noise exposure)
  - 7-day action plan with **concrete goals** (steps, exercise minutes, sleep window, hydration, etc.)
  - Non-diagnostic, coaching-style tone

- **Modern web UI** (HTML + JS + CSS):
  - Upload CSV  
  - View summary table  
  - Get AI Insights with one click  

---

## 🏗️ Architecture & Flow

1. **Frontend (index.html)**  
   - User uploads CSV.  
   - Shows a summary table (7-day aggregate).  
   - On click “Get Insights”, calls backend `/api/insights`.  

2. **Backend (Spring Boot)**  
   - `HealthService`: parses CSV, extracts required fields, computes 7-day summary.  
   - `HealthController`: exposes `/api/health/upload` and `/api/insights`.  
   - `CerebrasService`: sends the 7-day summary to Cerebras LLM API and returns natural language insights.  

3. **Cerebras LLM API**  
   - Processes summary with a **system prompt** tuned for wellness coaching.  
   - Returns structured coaching advice.  

4. **Output**  
   - Summary metrics displayed in a table.  
   - AI Insights shown in a styled box.  

---

## 🖥️ Demo Flow

1. **Upload CSV**  
   Select the exported file from **Health Auto Export**.  

2. **Summary Computation**  
   - App aggregates last 7 days.  
   - Shows steps, energy, exercise, heart metrics, gait, SpO₂, and more.  

3. **Get Insights**  
   - Click the button → request sent to Cerebras LLM API.  
   - Within seconds, you get a personalized wellness report with suggestions.  

---

## ⚙️ Tech Stack

- **Backend**: Java 17, Spring Boot, Commons CSV, OkHttp, Jackson  
- **Frontend**: HTML, CSS, Vanilla JS  
- **AI**: Cerebras LLM API (Llama 3.1-8B)  
- **Deployment**: runs locally (Maven) or on any Java server  

---

## 📦 Setup & Run

### Prerequisites
- Java 17+
- Maven
- [Cerebras API key](https://cloud.cerebras.ai)

### Run
```bash
git clone https://github.com/yourusername/wellness-coach.git
cd wellness-coach
export CEREBRAS_API_KEY=your_api_key_here
mvn spring-boot:run

