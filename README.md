# AlphaMonitoringApp
AlphaMonitoringApp is a child-side Android application designed for parental control and monitoring. It captures and uploads key data from the child’s device to Firebase, allowing parents to monitor activity through a connected web dashboard.

# 📱 AlphaMonitoringApp (Android – Child App)

**AlphaMonitoringApp** is a child-side Android application built to enable **real-time monitoring** of a child's mobile device by parents. It works seamlessly with a Firebase backend and a React-based web dashboard (used by the parent) to collect and display various types of activity data from the child’s phone.

---

## 🚀 Features

- 📞 **Call Log Monitoring** – Tracks incoming, outgoing, and missed calls.
- 💬 **SMS Monitoring** – Captures sent and received messages.
- 👥 **Contact Sync** – Uploads phonebook contacts to Firebase.
- 📷 **Photo Access** – Detects and uploads newly added images from local storage.
- 📧 **Gmail Monitoring** – Fetches email metadata and content using Gmail API.
- ☁️ **Google Drive Access** – Lists and monitors files stored in the child's Google Drive.
- 🖼️ **Google Photos Sync** – Monitors media stored in the child’s Google Photos.
- 🔄 **Automatic Uploads** – Uses ForegroundService + ContentObservers to sync new data.
- 🔐 **Google Sign-In Integration** – Secure and simple login using the child’s Google account.
- ☁️ **Firebase Integration** – Auth, Firestore, and Storage used for secure and scalable cloud sync.

---

## 🧠 Architecture

- **Language**: Kotlin  
- **UI**: XML  
- **Backend**: Firebase (Firestore, Authentication, Storage)  
- **Google APIs**: Gmail API, Drive API, Photos API  
- **Services**: ForegroundService, BroadcastReceivers, ContentObservers  
- **Pattern**: MVVM (recommended for production apps)

---

## ⚙️ Permissions Required

```xml
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.INTERNET" />
