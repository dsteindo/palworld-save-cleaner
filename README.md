# palworld-save-cleaner

This is a small project where I want to play around and clean pal references in save files that are unassigned.<br>
Background for these unassigned references is selling/condensing pals and the game not picking up the changes correctly.

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

## Notes

- The In Game Day seems to be calculated based on the "GameTimeSaveData" in Level.sav (as json)
- Example of In Game Day "187"
```
"GameTimeSaveData": {
    "struct_type": "PalGameTimeSaveData",
    "struct_id": "00000000-0000-0000-0000-000000000000",
    "id": null,
    "value": {
        "GameDateTimeTicks": {
            "id": null,
            "value": 161669830000000,
            "type": "Int64Property"
        },
        "RealDateTimeTicks": {
            "id": null,
            "value": 3600017340000,
            "type": "Int64Property"
        }
    },
    "type": "StructProperty"
},
```
