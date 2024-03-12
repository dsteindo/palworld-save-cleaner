# palworld-save-cleaner

This is a small project where I want to play around and clean pal references in save files that are unassigned.<br>
Background for these unassigned references is selling/contensing pals and the game not picking up the changes correctly.

## Requirements

- Python via Windows App Store
- Save tools -> https://github.com/cheahjs/palworld-save-tools
- Java -> https://sap.github.io/SapMachine/ (add JAVA_HOME and to path)
- Maven -> https://maven.apache.org/download.cgi (add to path manually)
- Visual Studio Code (for development, might add CLI in the future) 

## Building and Execution

- navigate to the root directory (where pom.xml) is located
- execute `mvn install`
- Navigate to %appdata%\..\Local\Pal\Saved\SaveGames and convert Level.sav using palworld save tools
- - Create a backup of your whole save directory before applying any changes to it 
- run App.java via VS Code (might add CLI later)
- convert the newly created "Level.sav.modified.json" back with save tools and override Level.sav
- You can check if changes were applied successfully with Pal Edit https://github.com/EternalWraith/PalEdit
- Start the game and check if everything is working and your base pals still exist

## Changelog

- Initial commit
- Provide CLI args for steamId and worldId, added fallback to first folder in %appdata%\..\Local\Pal\Saved\SaveGames
- Fixed issue where player was removed as well
