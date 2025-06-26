# 📱 Kids Watchtower

**Kids Watchtower** is a mobile-based **parental control application** developed as a graduation project by a team of IT students at **Kafr Elsheikh University**. The app empowers parents to manage and monitor their children's Android devices, promoting safer digital behavior and screen time habits through real-time controls and alerts.

---

## 📌 Table of Contents
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [System Architecture](#-system-architecture)
- [Screenshots](#-screenshots)
- [Setup Instructions](#-setup-instructions)
- [Testing](#-testing)
- [Future Work](#-future-work)
- [Team](#-team)
- [License](#-license)

---

## 🚀 Features

- 🔒 **App Blocking**: Block or schedule usage of specific applications (e.g., TikTok, YouTube).
- 📍 **Real-Time Location Tracking**: Live GPS tracking with geofencing alerts.
- ⏱ **Screen Time Management**: Enforce daily/weekly time limits per app or device.
- 🛑 **Device Locking**: Remotely lock the child’s device for study or sleep time.
- 🧠 **Suspicious File Monitoring**: Detect and block suspicious or harmful files.
- 🔔 **Alerts and Reports**: Receive instant alerts and generate activity reports.
- 👥 **Dual Role System**: Separate login experiences for Parents and Children.
- 🔐 **Secure Firebase Integration**: Ensures encrypted data and authentication.
- 📶 **Realtime Sync**: Seamless data updates between parent and child devices.

---

## 🧰 Tech Stack

| Component              | Technology                        |
|------------------------|------------------------------------|
| Mobile Development     | Kotlin (Android)                   |
| UI/UX Design           | Figma, XML Layouts                 |
| Backend Services       | Firebase Authentication            |
| Realtime Database      | Firebase Realtime DB, Firestore    |
| Notifications          | Firebase Cloud Messaging (FCM)     |
| Location Tracking      | Google Maps SDK                    |
| Scheduling Tasks       | WorkManager                        |
| Device Controls        | Android Device Policy APIs         |
| Testing Tools          | Espresso, Firebase Test Lab        |

---

## 🏗 System Architecture

- **Parent App**: Control panel to manage restrictions, view location, receive alerts.
- **Child App**: Installed on child’s device to be monitored and restricted.
- **Firebase Backend**: Handles all authentication, data syncing, and cloud storage.
- **Admin Dashboard**: (Optional) Internal interface to manage users, monitor issues.

---

## 📸 Screenshots

> Insert UI screenshots here from both the **Parent** and **Child** apps, such as:
- Login screen
- App blocker interface
- Real-time location map
- Usage reports view

---

## ✅ Testing

The app has been tested through:

- 🧪 **Unit & Integration Testing**: Using JUnit and Espresso to validate individual modules (e.g., app blocking, location fetching) and their interaction.
- 📱 **Compatibility Testing**: Tested across more than 20 Android devices, including various brands and OS versions (Android 7.0+).
- 📶 **Load & Stress Testing**: Simulated multiple parent-child sessions and real-time GPS updates to ensure stable performance under high activity.
- 🔒 **Security Audits**: Firebase Security Rules and authentication mechanisms were reviewed to ensure compliance with child privacy standards (e.g., GDPR, COPPA).

---

## 🧠 Future Work

- 🔍 **AI-Based Content Filtering**: Implement machine learning to detect inappropriate content in apps and web usage.
- 📈 **Enhanced Usage Analytics**: Provide parents with detailed trend graphs and insights about their child's screen habits.
- 🌐 **iOS Support via Flutter**: Expand cross-platform compatibility with a Flutter version targeting iOS devices.
- 🧑‍💼 **Multi-Child Dashboard**: Enable parents to manage multiple child profiles from a unified interface.
- 🗣 **AI Parenting Assistant**: Introduce a chatbot to guide parents on setting digital habits and handling alerts intelligently.
