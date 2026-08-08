# UMML - Unified MTT Mod Loader

UMML is a shared Java library for all future MTT versions: a mod loader
and, since 1.0, a save game system. Instead of every MTT version
re-inventing its own mod loader (V28-V31 had `mod_loader.java`,
V32/V33 had `ModLibrary.java`), they can all hook into this one library.

It scans `MTT_Mods`, loads every valid mod, resolves dependencies, and never
crashes on a corrupt or incomplete mod - it reports exactly which mods are
broken and why.

## What UMML does

- Scans a mods directory (folders only for now; `.zip` is planned later)
- Reads each mod's `moddata.xml`, including the MTT-style files that have
  **two root elements** (`<moddata>` and `<modcontents>`) which are not valid
  XML. Both single-root and double-root files load correctly.
- Resolves `<moddependencies>` and loads mods in dependency order
- Detects dependency cycles and missing dependencies
- Resolves every `<item>` path against the mod folder
- Collects every problem into a report instead of throwing - corrupt XML,
  missing moddata, missing modname, missing item paths, unreadable folders

## Using it in an MTT version

```java
import umml.UMML;
import umml.UMMLReport;
import umml.UMMLMod;
import umml.UMMLItem;
import umml.UMMLError;

// Load everything. Never throws.
UMMLReport report = UMML.scan("MTT_Mods");

for (UMMLMod mod : report.loadedMods()) {
    System.out.println(mod.name() + " v" + mod.version() + " by " + mod.author());
    for (UMMLItem item : mod.items()) {
        System.out.println("  [" + item.type() + "] " + item.name() + " -> " + item.resolvedPath());
    }
}

// Tell the player what was broken instead of crashing.
for (UMMLMod mod : report.failedMods()) {
    for (UMMLError err : mod.modErrors()) {
        System.out.println("Mod " + mod.folderName() + " did not load: " + err.message());
    }
}
```

Strict mode makes unresolved dependencies a hard failure:

```java
UMMLReport report = UMML.scan("MTT_Mods", UMMLOptions.defaults().strict(true));
```

## Save Game System

UMML 1.0 also ships a shared save game system so MTT versions stop
re-inventing save files too. Saves live in a `MTT_saves` folder at the repo
root, one subdirectory per MTT version:

```
MTT_saves/
  MTTV39/  Slot1.xml  Slot2.xml
  MTTV40/  Slot1.xml
  MTTV41/
```

Each save is a dynamic XML file - UMML does not need to know what the game
stores, so any MTT version can save any number of typed key/value entries
plus nested groups:

```java
import umml.UMMLSaveData;
import umml.UMMLSaveResult;
import umml.UMMLSaveSystem;

UMMLSaveData data = new UMMLSaveData();
data.setSavedBy("MTTV41");
data.setString("playername", "Bobby");
data.setInt("money", 5000);
data.setDouble("health", 87.5);
data.setBoolean("hasLicense", true);
data.group("inventory").setString("item_0", "Wrench");

// Load everything. Never throws.
UMMLSaveSystem saves = UMMLSaveSystem.find();
saves.save("MTTV41", "Slot1", data);

UMMLSaveResult result = saves.load("MTTV41", "Slot1");
if (result.isSuccess()) {
    UMMLSaveData loaded = result.data();
    int money = loaded.getInt("money", 0);
}
```

Supported entry types: `string`, `int`, `long`, `double`, `float`, `boolean`.

| `UMMLSaveSystem` method | Meaning |
| --- | --- |
| `UMMLSaveSystem.find()` | Finds `MTT_saves` (CWD, parent, grandparent) |
| `saves.listVersions()` | Version folders, e.g. `MTTV40` |
| `saves.ensureVersion("MTTV41")` | Creates the version folder |
| `saves.listSaves("MTTV41")` | Save slots in a version |
| `saves.save(version, slot, data)` | Writes `MTT_saves/<version>/<slot>.xml` |
| `saves.load(version, slot)` | Reads a save back |
| `saves.delete(version, slot)` | Deletes a save |
| `saves.rename(version, old, new)` | Renames a save |

Every operation returns a `UMMLSaveResult` (`isSuccess()` / `data()` /
`error()`) instead of throwing. A corrupt save file is reported as an
`XML_PARSE` error, never a crash.

## UMML Renderer (1.5)

UMML 1.5 ships a 2D renderer so every future MTT version can put graphics on
screen without adding any libraries or re-writing a game loop. It is built on
the standard Java graphics that come with every JDK (Java2D/Swing), and is
deliberately tiny and easy to remember: a window, a sprite, a loop, done.

Try the demo any time:

```
java -cp out umml.UMMLRendererExample
```

### The minimum game

```java
import umml.UMMLRenderer;

UMMLRenderer game = UMMLRenderer.open("My Game", 800, 600); // window
game.start();                                                // show it + loop
```

That's a real window that updates and draws 60 times a second. Everything
else is optional and layered on top.

### Sprites: pictures that move

A sprite is the easiest thing in the renderer. It has a picture (or a solid
colour), a position, a size and a velocity. Register it once and the
renderer moves it and draws it every frame:

```java
import umml.UMMLSprite;
import umml.UMMLImage;

UMMLSprite player = new UMMLSprite(UMMLImage.load("assets/player.png"), 100, 100);
player.setSize(48, 48);
player.setVelocity(120, 0);   // drifts right 120 pixels per second on its own
game.addSprite(player);       // now auto-updated + auto-drawn
```

Move it yourself by calling `move()` in your update code (see below), or
teleport it with `setPosition(x, y)`. Sprites can be rotated
(`setRotation(degrees)` / `rotate(degrees)`), hidden
(`setVisible(false)`), resized (`setSize(w, h)`), and checked for overlap
with `intersects(otherSprite)` or `contains(x, y)` - the last two are all
you need for very simple collision.

```java
player.move(50, 0);          // shift by 50 px
player.setPosition(400, 300);// jump to the centre
player.rotate(90);           // spin 90 degrees
```

### Images

```java
import umml.UMMLImage;

UMMLImage car = UMMLImage.load("assets/car.png");          // file or classpath
UMMLImage box = UMMLImage.solid(200, 30, new Color(0x8B, 0x5A, 0x2B)); // no art file needed
game.drawImage(car, 50, 50, 128, 64);                      // draw it directly
```

Loading a picture that does not exist **never crashes** - you get a pink
placeholder box instead, so a missing asset is obvious on screen. PNG, JPG,
GIF and BMP all work; PNG is best for game art because it supports
transparency.

### Shapes and text

```java
game.fillRect(0, 0, 100, 50);                        // filled rectangle
game.fillRect(0, 60, 100, 50, Color.RED);            // ...in one colour
game.drawRect(120, 0, 50, 50);                       // outline
game.fillCircle(200, 200, 30);                       // filled circle
game.drawCircle(200, 260, 30);                       // outline
game.drawLine(0, 0, 200, 200);                       // line
game.drawText("Hello UMML!", 20, 30, Color.WHITE, 18); // text, colour + size
```

### Input

```java
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

if (game.input().isDown(KeyEvent.VK_LEFT))   player.move(-200 * delta, 0);
if (game.input().wasPressed(KeyEvent.VK_SPACE)) game.addSprite(explosion);

int mx = game.input().mouseX();              // mouse position in the window
if (game.input().isMouseDown(MouseEvent.BUTTON1)) { ... }
if (game.input().wasMousePressed(MouseEvent.BUTTON1)) { ... } // once per click

UMMLInput.keyName(KeyEvent.VK_SPACE);        // "SPACE" - for "press X to start" hints
```

- `isDown(key)` - true while the key is held
- `wasPressed(key)` - true on exactly the frame it went down (for jump/fire,
  so it doesn't repeat every frame)
- `wasReleased(key)` - true on the frame it was released

### The game loop: update and draw

The renderer runs a classic game loop ~60 times per second. In each frame it
clears the window, runs your **update** code, then your **draw** code, then
auto-draws every registered sprite.

```java
game.onUpdate(delta -> {
    // Runs every frame before drawing. 'delta' is seconds since the last
    // frame (~0.016). ALWAYS multiply speed by it so things move the same
    // no matter the framerate.
    player.move(200 * delta, 0);   // 200 pixels per second, always
});

game.onDraw(renderer -> {
    // Runs after update, behind the registered sprites. Draw the
    // background / ground here.
    renderer.fillRect(0, 500, 2000, 100, new Color(0x3A, 0x7B, 0x3A));
    renderer.drawText("Score: " + score, 20, 30, Color.WHITE, 20);
});
```

### Camera

By default world coordinates match window coordinates. When your world is
bigger than the window, move the camera - everything you draw through the
renderer (and every sprite) is affected:

```java
game.setCamera(100, 0);          // show the world shifted right
game.moveCamera(10, 0);          // scroll right
game.centerOn(player.centerX(), player.centerY()); // follow a sprite
```

### Extensible, not throwaway

The renderer draws through the `UMMLGraphics` interface - that's the
"backend". UMML 1.5 ships one implementation, `UMMLGraphics2D`, built on the
standard Java graphics. If a faster/different rendering system is wanted
later, only a new `UMMLGraphics` implementation needs to be written; MTT
code keeps calling the same `UMMLRenderer` methods and nothing is rewritten.

### Renderer reference

| Method | What it does |
| --- | --- |
| `UMMLRenderer.open(title, w, h)` | Creates a window and renderer |
| `game.start()` | Shows the window, starts the loop |
| `game.close()` | Stops the loop and closes the window |
| `game.onUpdate(delta -> ...)` | Per-frame update code |
| `game.onDraw(renderer -> ...)` | Per-frame draw code (behind sprites) |
| `game.addSprite(s)` / `removeSprite(s)` | Auto-update/draw a sprite |
| `game.input()` | Keyboard + mouse state for the current frame |
| `game.setCamera(x, y)` / `centerOn(x, y)` | Move the view |
| `game.setClearColor(c)` | Background colour (default black) |
| `game.setTargetFps(fps)` | Loop speed (default 60) |
| `game.fillRect / drawRect / fillCircle / drawCircle / drawLine / drawText / drawImage` | Drawing, all respect the camera |
| `new UMMLSprite(img or color, x, y)` | A movable picture/colour box |
| `sprite.setPosition / move / setVelocity / update(delta)` | Movement |
| `sprite.setSize / setRotation / setVisible` | Appearance |
| `sprite.intersects(other)` / `sprite.contains(x, y)` | Simple collision |
| `UMMLImage.load(path)` | Load PNG/JPG/GIF/BMP (placeholder on failure) |
| `UMMLImage.solid(w, h, color)` | Program-made art, no files needed |

### Tile maps (2.5)

A tile map is a grid of little pictures (tiles) that make up a level,
drawn from one sprite sheet. Ground, walls, floors - all just tiles.

```java
import umml.UMMLTilemap;

UMMLImage sheet = UMMLImage.load("assets/tiles.png");   // e.g. 4x4 tiles of 32x32
UMMLTilemap map = new UMMLTilemap(sheet, 32, 32);       // 32x32 pixel tiles

map.setTiles(new int[][] {
    { -1, -1,  1,  1,  1, -1 },   // negative = empty cell
    {  2,  2,  2,  2,  2,  2 },   // numbers pick tiles from the sheet
});
map.setPosition(0, 400);          // where the map's top-left sits in the world

renderer.drawTilemap(map);        // draws only the tiles on screen
```

Tile numbers count left-to-right then top-to-bottom through the sheet (0 is
the top-left tile). `drawTilemap` skips every tile outside the window, so a
huge level draws as fast as a tiny one.

Handy extras: `map.tileAt(col, row)` / `map.isEmpty(col, row)` for
collision, `map.toWorldCol(x)` / `map.toWorldRow(y)` to find which cell a
point is in, `map.tileWorldX(col)` / `map.tileWorldY(row)` for the cell's
world position, and `map.setTile(col, row, index)` which grows the grid.

### Particles (2.5)

A particle system is a pile of small dots with a starting speed, a lifetime
and gravity. Explosions, smoke, sparks, rain, confetti - all the same trick.

```java
import umml.UMMLParticleSystem;

UMMLParticleSystem boom = new UMMLParticleSystem();
boom.setSpeed(180, 120);                 // 180 px/s, plus or minus up to 120
boom.setLife(0.8, 0.4);                  // each particle lives 0.4 - 1.2 s
boom.setSize(6, 3);                      // drawn size, plus or minus 3
boom.setGravity(300);                    // pulled down 300 px/s^2
boom.setColor(Color.ORANGE);             // start colour...
boom.setEndColor(Color.RED);             // ...fading to this (default: transparent)
renderer.addParticles(boom);             // auto-updated and auto-drawn

boom.burst(40);                          // explode now
// or boom.setRate(50).setEnabled(true); // steady stream while on
```

Angles are screen-style degrees: 0 right, 90 down, 180 left, 270 up.
`setDirection(angle, spread)` controls where particles fly; the default is
every direction at once. `setImage(UMMLImage)` makes particles draw a
picture instead of a circle. The system caps its live particle count (500)
so effects can never lag the game.

### Sprite animations (2.5)

An animation is a list of frames played in order - either cut from a sprite
sheet or handed in as separate pictures.

```java
import umml.UMMLAnimation;

UMMLImage sheet = UMMLImage.load("assets/hero_walk.png");  // frames in a grid
UMMLAnimation walk = new UMMLAnimation(sheet, 32, 32);     // all of them
UMMLAnimation run = new UMMLAnimation(sheet, 32, 32, 4, 4);// just frames 4-7
walk.setFrameDuration(0.1);                                // per-frame time

UMMLSprite hero = new UMMLSprite(sheet, 100, 100);
hero.setAnimation(walk);           // one line = an animated player
renderer.addSprite(hero);
```

Animations loop by default; `setLoop(false)` plays once (optionally with
`setOnFinished(...)`). `pause()` / `play()` / `stop()` control playback, and
any sprite attached to an animation advances it automatically while it
updates.

## Dashboard

UMML 1.0 ships a Swing graphical dashboard:

```
dashboard.bat
```

Three tabs:

- **Mods** - point it at a mods directory, scan, and inspect loaded/failed
  mods with per-mod error details.
- **Saves** - browse `MTT_saves` by version and slot, view and lightly edit
  save contents, create and delete saves.
- **Self Test** - run the mod loading and save system self tests with
  captured console output.

## Report contents

| Member | Meaning |
| --- | --- |
| `report.loadedMods()` | Mods that loaded, in load order (dependencies first) |
| `report.failedMods()` | Mods that could not be loaded at all, with reasons |
| `report.errors()` | Every error and warning collected during the scan |
| `report.hasErrors()` / `errorCount()` | Quick problem checks |
| `report.vehicleCount()` | Vehicles across loaded mods |
| `report.modByName(name)` | Look up a loaded mod |

Each `UMMLMod` also carries its own `modErrors()` and `isFullyLoaded()`.

## Building

```
build.bat        -> compiles src\umml to out\ and packages lib\umml.jar
test.bat         -> runs the mod loading self test, the save system self
                    test, the renderer self test, the renderer extras self
                    test, then scans MTT_Mods and prints a report
dashboard.bat    -> launches the Swing dashboard
```

Try the renderer demo (a window with a player sprite, moving coins, camera
follow, mouse clicks and on-screen hints):

```
java -cp out umml.UMMLRendererExample
```

## CLI

```
java -cp out umml.UMMLMain [mods_directory] [--strict] [--verbose]
```

With no directory it looks in `MTT_Mods`, `../MTT_Mods`, `../../MTT_Mods`.

## Files

- `src\umml\UMML.java` - public entry point (`UMML.scan`)
- `src\umml\ModScanner.java` - walks the directory, never throws
- `src\umml\ModParser.java` - robust `moddata.xml` parsing (multi-root aware)
- `src\umml\DependencyResolver.java` - load ordering, cycle/missing detection
- `src\umml\UMMLMod.java` / `UMMLItem.java` / `UMMLError.java` / `UMMLReport.java` - result models
- `src\umml\UMMLOptions.java` - behaviour flags
- `src\umml\UMMLMain.java` - command line front end
- `src\umml\UMMLSelfTest.java` - automated mod loading self test
- `src\umml\UMMLSaveData.java` - dynamic typed save data (entries + groups)
- `src\umml\UMMLSaveResult.java` - save operation results (never throws)
- `src\umml\UMMLSaveSystem.java` - `MTT_saves` version/slot handling + XML
- `src\umml\UMMLSaveSystemTest.java` - automated save system self test
- `src\umml\UMMLDashboard.java` - Swing dashboard (Mods / Saves / Self Test)
- `src\umml\UMMLRenderer.java` - 2D renderer: window, game loop, camera, drawing
- `src\umml\UMMLGraphics.java` - the renderer's backend interface (swap for other engines)
- `src\umml\UMMLGraphics2D.java` - the built-in Java2D backend
- `src\umml\UMMLImage.java` - texture loading (PNG/JPG/GIF/BMP, placeholder on failure)
- `src\umml\UMMLSprite.java` - movable drawable object (picture or colour + velocity)
- `src\umml\UMMLInput.java` - keyboard/mouse state per frame
- `src\umml\UMMLRendererTest.java` - automated headless renderer self test
- `src\umml\UMMLRendererExample.java` - runnable renderer demo / tutorial
- `src\umml\UMMLTilemap.java` - tile map: grid of tiles drawn from one sheet (2.5)
- `src\umml\UMMLParticleSystem.java` - particle effects: speed, gravity, life, fade (2.5)
- `src\umml\UMMLAnimation.java` - sprite-sheet / image-list animations (2.5)
- `src\umml\UMMLRendererExtrasTest.java` - automated headless test for the 2.5 extras
