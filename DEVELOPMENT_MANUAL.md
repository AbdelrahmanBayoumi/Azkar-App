
## Requirements
- [Java 8](https://www.oracle.com/java/technologies/javase/javase8u211-later-archive-downloads.html) (**for development**), to use the app normally you can download it from [Here  ⬇️](https://azkar-site.web.app/#download)
- [Maven 3.3+ ](https://maven.apache.org)
    - [(How to Install)](https://youtu.be/--Iv5vBIHjI)

## Installation for development
1. Download the repository files (project) from the download section or clone this project by typing in the bash the following command:

       git clone https://github.com/AbdelrahmanBayoumi/Azkar-App.git
2. Import it in Intellij IDEA or any other Java IDE and let Maven download the required dependencies for you.
3. To use Sentry (for error reporting) you need to add your own DSN in the `src/main/resources/sentry.properties` file.
    - See `sentry.properties.example` for an example.
4. Run the application 😁
