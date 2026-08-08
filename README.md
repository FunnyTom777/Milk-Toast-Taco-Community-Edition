
<center>
    <img src="Assets/Logos/MTT_Logo1.png" alt="Logo" width="400"><br>
    <a href="LICENSE">Read the License Here</a>
</center>
<br>

# Welcome to Milk Toast Taco!

This is the repository for the **Milk Toast Taco Community Edition**, licensed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html#license-text).

The source code for **Milk Toast Taco 26** is *closed source*.

Updates to **Milk Toast Taco 26** will be added to the Community Edition approximately **2–3 weeks after their release** in MTT 26.


















## Building and Running

Requires a **Java 21+ JDK**. No Gradle or Maven yet - plain `javac` and `java`.

Everything is driven by one launcher. Run `launcher.bat` (Windows) or `./launcher.sh` (Linux/macOS) from the repo root. It builds MTT **and** UMML automatically, then gives you a menu:

- `1` Run MTT (main game)
- `2` Run MTT Dev Console
- `3` Launch UMML Dashboard
- `4` Run UMML Self Tests
- `5` Package a Binary
- `6` Exit

Compiled classes land in `out/` (MTT) and `UMML/out/` (UMML). Any external jars dropped in `Libs/` are picked up automatically. The source root is `Systems/` - game systems live as subpackages (`mtt.ai`, `mtt.gameplay`, `mtt.vehicles`, ...).

## Dev Console

The dev console is a Swing-based tool for testing game systems quickly. Pick **option 2** in `launcher.bat` / `launcher.sh` (Windows opens it in its own window).

- Type `/help` for all commands, `/help <command>` for usage
- `/player`, `/inventory` to inspect state; `/setmoney`, `/addmoney`, `/takemoney`, `/giveitem`, `/rename`, `/newgame` to poke at it
- Up/Down arrows recall command history, Tab autocompletes

## Packaging a Binary

Pick **option 5** in `launcher.bat` / `launcher.sh`. It builds the project, bundles it into a jar, and runs `jpackage` to produce a self-contained app that needs no installed Java.

- Output: `dist/MilkToastTaco/` containing `MilkToastTaco.exe` (Windows) plus its bundled runtime
- Requires a **JDK 14+** with `jpackage`; the scripts find it via `JAVA_HOME` or common install paths
- The `dist/MilkToastTaco` folder can be zipped and shipped as-is.

## UMML - Unified MTT Mod Loader

`UMML/` is a Java library bundled with **Milk Toast Taco Community Edition**. Unlike the old UMML, it is only designed to serve this project - not every MTT version. It gives MTT CE a shared **mod loader** and a **save game system** so it does not have to re-invent them every build.

### Building UMML

UMML is compiled automatically every time you run `launcher.bat` / `launcher.sh` - no separate build step needed. Its classes land in `UMML/out/` and its jar in `UMML/lib/umml.jar`.

From the launcher menu: **option 3** opens the Swing dashboard (scan and inspect mods, browse and edit saves, run the self tests) and **option 4** runs the mod loading and save system self tests, then scans `MTT_Mods` and prints a report.

To use UMML in MTT CE code, drop `UMML\lib\umml.jar` into `Libs\` (the game build automatically picks up every jar there) and `import umml.*`.

### Mod loading

UMML scans a mods directory (e.g. `MTT_Mods`), loads every valid mod, resolves `<moddependencies>` in order, and **never crashes** on a broken mod - it reports exactly which mods failed and why:

```java
import umml.UMML;
import umml.UMMLReport;
import umml.UMMLMod;

UMMLReport report = UMML.scan("MTT_Mods");   // never throws

for (UMMLMod mod : report.loadedMods()) {
    System.out.println(mod.name() + " v" + mod.version() + " by " + mod.author());
}

for (UMMLMod mod : report.failedMods()) {
    for (var err : mod.modErrors()) {
        System.out.println("Mod " + mod.folderName() + " did not load: " + err.message());
    }
}
```

Strict mode turns unresolved dependencies into a hard failure: `UMML.scan("MTT_Mods", UMMLOptions.defaults().strict(true))`.

### Save game system

Saves **always** go to a `saves/` folder at the root of the project - `saves/<version>/<slot>.xml` - one subfolder per MTT CE version so versions never trample each other's saves. `UMMLSaveSystem.find()` finds the project root by walking up until it hits the `Systems/` source folder, then uses `saves/` under it. The `saves/` folder is gitignored, so saves are never uploaded.

Each save is a dynamic XML file - MTT CE can store any number of typed values plus nested groups, and UMML never has to know what the game saves:

```java
import umml.UMMLSaveData;
import umml.UMMLSaveResult;
import umml.UMMLSaveSystem;

UMMLSaveData data = new UMMLSaveData();
data.setSavedBy("MTTV40");
data.setString("playername", "Bobby");
data.setInt("money", 5000);
data.setDouble("health", 87.5);
data.setBoolean("hasLicense", true);
data.group("inventory").setString("item_0", "Wrench");

UMMLSaveSystem saves = UMMLSaveSystem.find();   // project root / saves
saves.save("MTTV40", "Slot1", data);            // writes saves/MTTV40/Slot1.xml

UMMLSaveResult result = saves.load("MTTV40", "Slot1");
if (result.isSuccess()) {
    int money = result.data().getInt("money", 0);
}
```

Every operation returns a `UMMLSaveResult` (`isSuccess()` / `data()` / `error()`) instead of throwing, and a corrupt save file is reported as an `XML_PARSE` error, never a crash. Supported value types: `string`, `int`, `long`, `double`, `float`, `boolean`, plus nested `group(name)`.

### Development Notes:
- Data files are `.xml`, Not JSON or YAML.
- Game is made in Java, With Limited External Dependancies. No Gradle/Maven (Atleast Yet)
- Systems based. Text based at first, With plans for 2D/3D further down the track.