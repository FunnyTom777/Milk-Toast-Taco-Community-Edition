
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

- **Windows:** run `build.bat`, then `run.bat`
- **Linux/macOS:** run `./build.sh`, then `./run.sh`

Compiled classes land in `out/`. Any external jars dropped in `Libs/` are picked up automatically. The source root is `Systems/` - game systems live as subpackages (`mtt.ai`, `mtt.gameplay`, `mtt.vehicles`, ...).

## Dev Console

The dev console is a Swing-based tool for testing game systems quickly. Launch it with `run-dev.bat` (Windows) or `./run-dev.sh` (Linux/macOS) after building.

- Type `/help` for all commands, `/help <command>` for usage
- `/player`, `/inventory` to inspect state; `/setmoney`, `/addmoney`, `/takemoney`, `/giveitem`, `/rename`, `/newgame` to poke at it
- Up/Down arrows recall command history, Tab autocompletes

### Development Notes:
- Data files are `.xml`, Not JSON or YAML.
- Game is made in Java, With Limited External Dependancies. No Gradle/Maven (Atleast Yet)
- Systems based. Text based at first, With plans for 2D/3D further down the track.