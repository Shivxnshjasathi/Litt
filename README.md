# Lilt Music Discovery

![Lilt App Banner](https://placehold.co/1200x400/1D2671/FFFFFF?text=Lilt%20Music&font=sans)

**Discover your next favorite song with Lilt, a sleek and modern music discovery app built with Jetpack Compose. Lilt provides personalized music recommendations, allowing you to listen to previews and learn more about the tracks you love.**

---

## 🎵 Features

* **Dynamic Home Screen**: A visually rich home screen featuring a collapsing toolbar with a parallax effect.
* **Personalized Recommendations**: A "Picked For You" section with a swipeable carousel of featured songs.
* **Popular Suggestions**: An endlessly scrollable list of popular tracks.
* **Seamless Music Playback**:
    * An elegant, non-intrusive **Mini-Player** for continuous playback while browsing.
    * A fully-featured **Full-Screen Player** with a dynamic, album-art-based gradient background.
* **Intuitive Gestures**:
    * **Swipe-to-Dismiss**: Swipe the full-screen player down to close it.
    * **Draggable Seek Bar**: Easily scrub through the track to find your favorite part.
* **AI-Powered Song Summaries**: Get a brief, AI-generated summary for the currently playing song to learn more about its story and meaning.
* **Modern & Fluid UI**: Built entirely with Jetpack Compose, featuring beautiful animations, transitions, and a polished user experience.

---

## 📸 Screenshots & Demo

*(Here you would add GIFs or screenshots of the app in action)*

| Splash Screen                                                                                                 | Home Screen (Collapsing Toolbar)                                                                                      |
| ------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| ![Splash Screen](https://github.com/user-attachments/assets/bbb62862-493d-4d38-a57c-1b8b83574bf8)               | ![Home Screen](https://github.com/user-attachments/assets/64dbbc45-30bd-4779-b780-c26c11614e1b)                       |
| **Full-Screen Player (Dynamic BG)** | **Featured Songs Carousel** |
| ![Full Screen Player](https://github.com/user-attachments/assets/966c5205-07dc-45b1-aa4c-868e0704e9c9)           | ![Carousel](https://github.com/user-attachments/assets/41e73951-130a-45ac-bfc2-822edd76d74c)                           |

---

## 🛠️ Tech Stack & Architecture

This project is a demonstration of modern Android development practices.

* **UI**: 100% [Jetpack Compose](https://developer.android.com/jetpack/compose) for a declarative, reactive, and modern UI.
* **Architecture**: Follows the recommended **MVVM (Model-View-ViewModel)** architecture.
* **Asynchronous Programming**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) are used extensively for background tasks like API calls and data processing.
* **Networking**:
    * [Retrofit](https://square.github.io/retrofit/) for type-safe HTTP requests to the music API.
    * [OkHttp](https://square.github.io/okhttp/) as the underlying HTTP client, with an interceptor for logging.
* **JSON Parsing**: [Kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) for efficient and safe JSON parsing.
* **Image Loading**: [Coil](https://coil-kt.github.io/coil/) for fast, modern image loading with Jetpack Compose integration.
* **Music Playback**: [ExoPlayer (Media3)](https://developer.android.com/guide/topics/media/media3) for robust and feature-rich audio playback.
* **Dynamic Theming**: [Palette API](https://developer.android.com/training/material-design/palette-api) to extract dominant colors from album art for a dynamic and immersive player UI.
* **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation) for handling navigation between screens.

---

## 🔌 API Reference

Lilt uses a combination of public APIs to deliver its content:

* **Song Data**: Sourced from `energy.ch`'s public API to fetch recent song playouts.
* **Song Summaries**: Generated using the Pollinations AI API, which provides creative text summaries based on the song title and artist.

---

## 🚀 Setup and Installation

To run this project locally, follow these steps:

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/your-username/lilt-music-app.git](https://github.com/your-username/lilt-music-app.git)
    ```
2.  **Open in Android Studio:**
    * Open Android Studio (Hedgehog or newer is recommended).
    * Select `File > Open` and navigate to the cloned project directory.
3.  **Sync Gradle:**
    * Let Android Studio sync the project and download all the required dependencies.
4.  **Run the app:**
    * Select an emulator or connect a physical device.
    * Click the 'Run' button.

---

## ✨ Future Enhancements

This project is a work in progress. Here are some features planned for the future:

* [ ] **User Authentication**: Allow users to create accounts and save their favorite tracks.
* [ ] **Playlist Creation**: Enable users to create and manage their own playlists.
* [ ] **Search Functionality**: Implement a search feature to find specific songs or artists.
* [ ] **Offline Caching**: Cache songs and data for offline playback.
* [ ] **UI/Unit Tests**: Add a comprehensive suite of tests to ensure code quality and stability.

---

## 🤝 Contributing

Contributions are welcome! If you'd like to contribute, please fork the repository and create a pull request. You can also open an issue with the tag "enhancement" to suggest new features.

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

---

## 📜 License

This project is licensed under the MIT License - see the `LICENSE.md` file for details.

---

## 🙏 Acknowledgements

* Thanks to [Energy.ch](https://energy.ch) for providing the song data API.
* Thanks to [Pollinations.ai](https://pollinations.ai/) for the creative text generation API.
