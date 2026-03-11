# Aegis - Privacy Protection Suite

Aegis is a powerful, real-time privacy monitoring application for Android. It ensures that you are always aware of when and how your device's most sensitive sensors—**Camera, Microphone, and Location**—are being accessed by third-party applications.

## 🚀 Core Features

### 1. Real-Time Privacy Indicators
Never be tracked in secret. Aegis displays unobtrusive, color-coded floating indicators on your screen the moment an app starts using your:
*   🟢 **Camera**
*   🟠 **Microphone**
*   🔵 **Location**

### 2. Privacy Health Score
Get an instant snapshot of your device's privacy status. Aegis calculates a **Health Score** based on app behavior, sensor usage patterns, and suspicious activities detected over the last 24 hours.

### 3. Detailed Access Logs
Keep a chronological history of every sensor access event. See which app used what sensor, exactly when it started, and for how long.

### 4. NLP-Powered Risk Analysis
Aegis uses **On-Device Natural Language Processing (NLP)** and a custom **Sensor Rule Engine** to analyze installed apps. It performs semantic analysis on app names and package IDs to categorize them (e.g., "Finance" or "Calculator") and identify if they are requesting excessive permissions for their category.

### 5. Suspicious Activity Detection
Aegis monitors for high-risk scenarios, such as:
*   Background sensor usage while the screen is off.
*   Concurrent sensor usage and network activity (potential data exfiltration).
*   Permission overreach by simple utility apps.

---

## 🛠️ Technical Architecture

### The "Brain": Accessibility Service
The core of Aegis is the `IndicatorService`, which leverages Android's **Accessibility Service** API. This allows Aegis to:
1.  Detect the foreground application.
2.  Monitor sensor state changes via system callbacks (`CameraManager`, `AudioManager`, `LocationManager`).
3.  Draw the floating indicator overlay over other apps.

### Data & Intelligence
*   **Database:** Uses Room/SQLite for persistent storage of access logs and suspicious activity records.
*   **Semantic Intelligence:** Uses an on-device NLP engine to classify apps and predict "typical" sensor requirements without any battery-heavy external models.
*   **Lifecycle Management:** Utilizes ViewModels and LiveData to ensure a reactive UI that responds safely to background service events.

---

## 🔄 Working Flow

1.  **Initialization:** The user completes a swipable onboarding flow that explains the app's value proposition.
2.  **Activation:** The user enables the **Aegis Accessibility Service** in system settings.
3.  **Real-Time Monitoring:** 
    *   `IndicatorService` runs in the background.
    *   System callbacks trigger whenever a sensor is toggled.
    *   Aegis identifies the accessing app and displays a floating indicator.
4.  **Logging & Analysis:** Every event is logged. The app periodically refreshes the Privacy Health Score.
5.  **User Intervention:** Users review logs or the Risk Analysis dashboard and can quickly navigate to system settings to revoke permissions for untrusted apps.

---

## 💻 Tech Stack
*   **Language:** Kotlin
*   **UI Framework:** Android XML (Material 3) + Jetpack Compose (Onboarding)
*   **Database:** Room Persistence Library
*   **ML Engine:** TensorFlow Lite
*   **Service:** Android Accessibility Service
*   **Dependency Management:** Gradle

---
Developed by **Team Obsidian**