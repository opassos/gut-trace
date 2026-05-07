# GutTrace

An ultra-low-friction meal and gastrointestinal symptom tracking app, prioritizing a local-first, zero-friction experience.

## Mac Mini Server (Backend)

The server acts as the data vault and dashboard for GutTrace. It receives synced events and photos from the Android app, storing them locally.

### Setup and Run
1. Navigate to the server directory:
   ```bash
   cd server
   ```
2. Create a virtual environment and install dependencies:
   ```bash
   python3 -m venv venv
   source venv/bin/activate
   pip install -r requirements.txt
   ```
3. Run the server:
   ```bash
   python3 main.py
   ```
4. Access the dashboard:
   Open [http://localhost:8000](http://localhost:8000) in your browser.

## Android App

Due to the complex nature of Android SDK and Gradle project setup, it is highly recommended to bootstrap the Android application using **Android Studio**:

1. Open Android Studio and select **New Project**.
2. Choose **Empty Activity** (which defaults to Jetpack Compose).
3. Name the project `GutTrace` and set the package name (e.g., `com.example.guttrace`).
4. Select **Kotlin** as the language and choose the minimum SDK (API 26+ recommended).
5. Once created, you'll be ready to add `CameraX`, `Room`, and `WorkManager` dependencies.

Check out the [Android source code](./android-source/) for the full implementation details.
