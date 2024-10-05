
## Requirements
- [Java 8](https://www.oracle.com/java/technologies/javase/javase8u211-later-archive-downloads.html) (**for development**), to use the app normally you can download it from [Here  ⬇️](https://azkar-site.web.app/#download)
- [Maven 3.3+ ](https://maven.apache.org)
    - [(How to Install)](https://youtu.be/--Iv5vBIHjI)
- [Scene Builder](https://gluonhq.com/products/scene-builder/): is a visual layout tool for designing JavaFX user interfaces without writing FXML code manually.

    <details>
      <summary>Make sure to download version 8.5.0, to be compatible with Java 8</summary>    
      <img src="https://github.com/user-attachments/assets/b885c8ad-3ed3-4a70-9c4b-213d55252b6e">
    </details><details>
      <summary>Also its required to import the UI libraries used in Scene Builder if you gonna use it</summary>    
      <img src="https://github.com/user-attachments/assets/2a5ac3b3-9661-4a0f-9404-51c929837341">
      <img src="https://github.com/user-attachments/assets/88c8b5d2-78dd-4b19-bf0d-2863f6705edb">
      <img src="https://github.com/user-attachments/assets/e85b661a-02f2-4b2c-ba0c-5b76af9c9109">
    </details>
  
 
## Installation for development
1. Download the repository files (project) from the download section or clone this project by typing in the bash the following command:

       git clone https://github.com/AbdelrahmanBayoumi/Azkar-App.git
2. Import it in Intellij IDEA or any other Java IDE and let Maven download the required dependencies for you.
3. To use Sentry (for error reporting) you need to add your own DSN in the `src/main/resources/sentry.properties` file.
    - See `sentry.properties.example` for an example.
4. To use the `ip2location` location services, you need to add your `ip2location.apiKey` in the `src/main/resources/config.properties` file.
   - See `config.properties.example` for an example.
5. Run the application 😁
