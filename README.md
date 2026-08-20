# CampusFlow: BTech Student Helper

**CampusFlow** is a powerful, AI-integrated Android application designed specifically to assist BTech students in managing their academic life efficiently. From automated timetable scheduling to smart attendance tracking, CampusFlow leverages cutting-edge technology to simplify the campus experience.

---

## 🚀 Features

### 📅 Smart AI Timetable Scanner
Stop manually entering your schedule!
- **How it works**: Simply take a photo or upload an image of your timetable.
- **Implementation**: Uses **Google Gemini AI** to perform OCR and intelligently parse complex timetable grids into a structured format stored in the local database.

### 📍 GPS-Based Attendance Tracking
Never miss a class or forget to mark attendance.
- **Geofencing**: Automatically detects if you are within the campus boundaries using **GPS Geofencing**.
- **Period Management**: Tracks attendance for each specific period. If you're in the right place at the right time, CampusFlow helps you stay on top of your attendance criteria.

### 📄 Document Scanner & AI OCR
A dedicated tool for digitizing study materials.
- **Scanner**: High-quality document scanning with filters.
- **AI Analysis**: Uses Gemini AI to not just extract text, but to **analyze and summarize** your scanned notes or documents.

### 🗓️ Academic Calendar Integration
Integrated with the **IIEST Academic Calendar** (and customizable for others), ensuring you're always aware of holidays, exams, and important academic events.

### 📊 Attendance Analytics
Visualize your attendance trends. Stay informed about how many classes you can safely skip or how many you need to attend to meet your 75% requirement.

### 🧩 Home Screen Widgets
- **Live Class Widget**: Shows your current and upcoming classes directly on your home screen.
- **Quick Shortcuts**: Fast access to the scanner, timetable, and attendance logs.

---

## 🛠️ Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Modern, declarative UI)
- **Architecture**: MVVM (Model-View-ViewModel) with Clean Architecture principles.
- **Asynchronous Programming**: Kotlin Coroutines & Flow for reactive data streams.
- **Database**: Room Persistence Library for local data storage.
- **AI Integration**: Google Gemini API for intelligent data parsing and OCR analysis.
- **Networking**: Retrofit & OkHttp for API communication.
- **Image Processing**: Coil for efficient image loading.
- **Dependency Injection**: Manual DI (Hilt/Koin can be integrated if needed).

---

## ⚙️ Setup & Configuration

### Prerequisites
- [Android Studio Ladybug](https://developer.android.com/studio) or newer.
- Android Device/Emulator with API Level 24+.

### API Configuration
CampusFlow requires API keys to power its features.
1. Obtain a Gemini API key from [Google AI Studio](https://aistudio.google.com/).
2. Obtain a Google Maps API key from the [Google Cloud Console](https://console.cloud.google.com/).
3. Add these keys to your `local.properties` file (which is not tracked by Git):
   ```properties
   GEMINI_API_KEY=YOUR_GEMINI_KEY
   maps_api=YOUR_MAPS_KEY
   ```

---

## 👨‍💻 Author

**J Sujal Kumar**

- **GitHub**: [sujal-kumar-jasti](https://github.com/sujal-kumar-jasti)
- **LinkedIn**: [sujalkumarjasti](https://www.linkedin.com/in/sujalkumarjasti)
- **Email**: [sujalkumarjasti751@gmail.com](mailto:sujalkumarjasti751@gmail.com)

---

## 📄 License
This project is for educational purposes. Feel free to contribute and improve the helper for BTech students!
