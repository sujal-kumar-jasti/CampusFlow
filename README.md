# CampusFlow: BTech Student Helper

CampusFlow is an Android application for BTech students to manage their academic life. It uses AI for timetable scheduling and attendance tracking to simplify the campus experience.

---

## Features

### Smart AI Timetable Scanner
Stop manually entering your schedule.
- How it works: Take a photo or upload an image of your timetable.
- Implementation: Uses Google Gemini AI to read the image and save the timetable data to the local database.

### GPS-Based Attendance Tracking
Track your attendance for each class.
- Geofencing: Detects if you are inside the campus boundaries using GPS.
- Period Management: Records attendance for each period if you are in the right place at the right time.

### Document Scanner and AI OCR
A tool to digitize your study materials.
- Scanner: Scan documents with filters.
- AI Analysis: Uses Gemini AI to extract and summarize text from your notes or documents.

### Academic Calendar Integration
Integrated with the IIEST Academic Calendar. It shows holidays, exams, and other important events.

### Attendance Analytics
Check your attendance trends. Stay informed about your attendance percentage and the 75% requirement.

### Home Screen Widgets
- Live Class Widget: Shows current and upcoming classes on your home screen.
- Quick Shortcuts: Quick access to the scanner, timetable, and attendance logs.

---

## Technical Stack

- Language: Kotlin
- UI Framework: Jetpack Compose
- Architecture: MVVM
- Asynchronous Programming: Kotlin Coroutines and Flow
- Database: Room Persistence Library
- AI Integration: Google Gemini API
- Networking: Retrofit and OkHttp
- Image Processing: Coil

---

## Setup and Configuration

### Prerequisites
- Android Studio Ladybug or newer.
- Android Device or Emulator with API Level 24+.

### API Configuration
CampusFlow needs API keys to work.
1. Get a Gemini API key from Google AI Studio.
2. Get a Google Maps API key from the Google Cloud Console.
3. Add these keys to your local.properties file:
   ```properties
   GEMINI_API_KEY=YOUR_GEMINI_KEY
   maps_api=YOUR_MAPS_KEY
   ```

---

## Author

J Sujal Kumar

- GitHub: [sujal-kumar-jasti](https://github.com/sujal-kumar-jasti)
- LinkedIn: [sujalkumarjasti](https://www.linkedin.com/in/sujalkumarjasti)
- Email: [sujalkumarjasti751@gmail.com](mailto:sujalkumarjasti751@gmail.com)

---

## License
This project is for educational purposes. Feel free to contribute and improve the helper for BTech students.
