# palworld-save-cleaner

This is a small project where I want to play around and clean pal references in save files that are unassigned.<br>
Background for these unassigned references is selling/condensing pals and the game not picking up the changes correctly.

## Changelog

- 2024.03.12 Started with the project to clean up not used pal references
- 2024.03.13 Added functionality to reset in-game timers for chests and other respawnables
- 2024.09.17 Added logic to reset boss timers, enemy camps, oil rig, foliage and dungeons. Removed pickup and destructable rock world objects to compress save game even further and improve loading/saving times. Estimated save compression 1:4

## Support

Tested with and supports Palworld steam version 0.3.6.x

## Requirements

- Python via Windows App Store
- Save tools
  - Works with drag on drop https://github.com/cheahjs/palworld-save-tools
  - Really good alternative to use https://github.com/deafdudecomputers/PalWorldSaveTools
- Java -> https://sap.github.io/SapMachine/ (add JAVA_HOME and to path)
- Maven -> https://maven.apache.org/download.cgi (add to path manually)
- Visual Studio Code (for development, might add CLI in the future) 

## Building and Execution

- navigate to the root directory (where pom.xml) is located
- execute `mvn install`
- Navigate to %appdata%\..\Local\Pal\Saved\SaveGames and convert Level.sav using palworld save tools
  - Create a backup of your whole save directory before applying any changes to it 
- run App.java via VS Code (might add CLI later)
- convert the newly created "Level.sav.modified.json" back with save tools and override Level.sav
- You can check if changes were applied successfully with Pal Edit https://github.com/EternalWraith/PalEdit
- Start the game and check if everything is working and your base pals still exist

## Changelog

- Initial commit
- Provide CLI args for steamId and worldId, added fallback to first folder in %appdata%\..\Local\Pal\Saved\SaveGames
- Fixed issue where player was removed as well
- Added functionality to respawn all objects (e.g ore/coal nodes and skill fruit tree fruits), because moving in game day in the past blocked respawning of resources
- Added multiple world support
- Logic will export pal parameters to an own file "pal-parameters-export.json"
- Possibility to override pal parameters if file "pal-parameters-import.json" is present, idea is to export from a previous state and import at a later time if needed, e.g. when changes with Pal Edit need to be reverted
- Added my own save files as examples for testing purposes

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
