# UMML 1.0

- [x] Add Save Game system to UMML that MilkToastTaco versions can use alongwith Mod Loading functions. (Part of UMML update 1.0)
- [x] Add Swing Grathical Dashboard for UMML with options for Self Testing of individual UMML systems (Mod Loading, Saves) (Part of UMML Update 1.0)
- [x] A MTT_saves folder in root, With subdirectories for each MTT versions
  EG:

  MTT_saves > MTTV39
            > MTTV40
            > MTTV41
            etc.

- [x] Each save uses XML and should be very dynamic, so diffrent MTT versions can save lots of data to there saves, not just fixed values.
- [x] And UI for viewing existing saves in the UMML dashboard.



# UMML 1.5

- [x] Implement the UMML Renderer as part of 1.5! It should be easy to use in all future MTT versions for 2D rendering, with easy to use functions for drawing sprites, moving sprites, etc.
- [x] the UMML renderer functions should be AS EASY to use as possible, and with EXTENSIVE documentation for future me on how to use it.
- [x] Should use the Built in Java graphics for now, But can be extended/modified later with other rendering systems, while not having to rewrite MTT.





# UMML 2.0! (The biggest version to date!) (Complete)

- UMML Studio! (UMML Studio V1) A proper 2D game making interface, Drag and drop interface, asset browser, object explorer, and a 'RUN' button that automattically compiles your game ('javac') and opens it in its own window!

  - Opens with a Project explorer. See projects, open projects in editor, or rename them, or view there folder. Create new projects, choose a name, choose a file location, and Create!
  - Opens your project in the 'Editor'. 2D viewport in the middle, Object Inspector on the right, Scene Explorer on the left, Console at the bottom, small Run button in the top right corner.
  - Buttons at the top of the Scene Explorer to create new Objects, Scripts (java), or Maps.
  - You can select objects in the 2D viewport and use diffrent tools (Move, scale, Rotate) to manipulate objects, which will change there properties in the Inspector also (You can also precisely manipulate them via the inspector)
  - Apply scripts to Objects by right clicking them in the Scene Explorer, Selecting 'Attach Script', you can select a existing script or create a new one, pick a name and it will open it in your selected text editor (Configurable in settings)

- UMML studio is designed to be VERY lightweight! and as simple to use as possible. No built in text editor to bloat it, Bring your own text editor, No built in pixel art tool, Bring your own, etc. (Settings configurable to be able to select default tools to open cirtain items in, EG. Pngs, java files, xml, etc)

- Save UMML studio projects as XML and load them again whenever you like!
- Easy to use Simple java swing User Interface.

- Add objects via the Scene Explorer. Diffrent types of objects. Sprite2D, Label2D, Button2D, CollisionShape2D, CharicterBody2D, etc.
- Objects can have other objects as childern (EG. CharicterBody2D could have a CollisionShape2D as a child)
- Diffrent objects have diffrent properties. (EG. a Collision Shape will have size properites, and shape properties, you can make it a circle, a square, a rectangle, a pill, etc)
- Every object stored as .xml. Loaded by UMML later.
- UMML prefabs! Create diffrent trees of objects, right click them, Save them as a Prefab (XML), You can reuse this Prefab as many times as you like! Update the prefab, it updates all across your project!

- UMML studio will get its own folder seperate from UMML, in the WoofWorks Root directory. It will just use UMML functions. the UMML jar is automattically installed in projects created with UMML studio.








# UMML 2.5

- [x] Tile maps! A `UMMLTilemap` class for drawing levels out of a grid of tiles from a sprite sheet, with camera culling so huge levels draw fast, plus coordinate helpers for collision. Drawn via `renderer.drawTilemap(map)`.
- [x] Particle effects! A `UMMLParticleSystem` for explosions, smoke, rain and sparks - speed + variance, direction + spread, gravity, lifetime, size, start/end colour fade, one-shot `burst()` or a steady `setRate()` stream, with a live-count cap. Auto-updated/drawn via `renderer.addParticles(system)`.
- [x] Sprite animations! A `UMMLAnimation` class for frame-by-frame animation from a sprite sheet or a list of pictures, loop/one-shot modes and finished callbacks. Attach to a sprite with `sprite.setAnimation(anim)` and it animates itself.
- [x] Added a headless `UMMLRendererExtrasTest` self test (87 checks) wired into `test.bat`.
- [x] Renderer demo (`UMMLRendererExample`) now shows off the tile map floor, an animated gem, and a click-to-explode particle burst.





# UMML 3.0 (Abandoned)

- [ ] **UMLua!** A hand-written mini-Lua interpreter in Java - Lua scripting for games, built with ZERO external libraries, just like the renderer. UMML stays Java, but now it can receive Lua code, interpret it, and tell the Java API what to do. Almost Love2D simple.

  - [ ] **The language subset** (tree-walking interpreter, no dependencies):
    - Values: `nil`, booleans, numbers (doubles), strings, tables, functions/closures.
    - Variables: globals + locals.
    - Operators: `+ - * / % ^`, `..` (string concat), all comparisons, `and/or/not`, unary `-`.
    - Control: `if/elseif/else`, `while`, numeric `for i=1,10`, generic `for k,v in pairs(t)`.
    - Functions: `function`, anonymous `function(...) end`, `return`, closures over upvalues.
    - Tables: `{}`, `{1,2,3}`, `{x=1}`, mixed + nested, `t[i]` and `t.name` indexing, `#` length.
    - Tiny stdlib: `print`, `tostring`, `tonumber`, `type`, `math.*` (abs, floor, max, min, random, sqrt), `string.format`.
    - Deliberately excluded (documented as "not Lua yet"): metatables, coroutines, varargs, `goto`, classes. Keeps it a game scripting language, not a language lab.
  - [ ] **Lexer** - turns Lua source into tokens (`UMMLLuaLexer`). Handles `--` line comments, strings, numbers, keywords, operators.
  - [ ] **Parser** - turns tokens into an AST (`UMMLLuaParser` + AST nodes). Good error messages with line numbers.
  - [ ] **Interpreter** - a tree-walking evaluator (`UMMLLuaInterpreter`) with environment scopes (globals, locals, upvalues for closures) and values/tables (`UMMLLuaTable`).
  - [ ] **Lua -> Java bridge** - register UMML functions as Lua globals: `sprite`, `tilemap`, `particles`, `animation`, `input`, `graphics`, `camera`. Lua calls `player:move(200*dt, 0)` (`:` is sugar for `player.move(player, ...)`), which dispatches to whitelisted methods on the Java object. `player.x` / `player.y` read fields.
  - [ ] **Java -> Lua callbacks** - the game loop calls Lua functions each frame: `on_start()`, `on_update(dt)`, `on_draw()`, `on_key_pressed(key)`, `on_key_released(key)`, `on_mouse_pressed(x, y, button)`, `on_mouse_moved(x, y)`.
  - [ ] **UMMLLuaGame** - the "make a game in one file" wrapper. Run with `java -cp out umml.UMMLLuaGame game.lua`. Example game.lua:
    ```lua
    -- game.lua - the whole game, no Java
    function on_start()
        player = sprite(100, 100, "assets/player.png")
        sparks = particles { speed = 200, gravity = 500 }
    end
    function on_update(dt)
        if input.is_down("LEFT") then player:move(-200 * dt, 0) end
        if input.was_pressed("SPACE") then sparks:burst(40) end
    end
    function on_draw()
        graphics.draw_image(player)
        graphics.draw_text("Hello UMML!", 20, 30, {1, 1, 1}, 18)
    end
    ```
  - [ ] **Never crash, the UMML way** - a Lua runtime error in `on_update` prints a line-numbered traceback to the console and the game keeps running. Missing `on_draw` just draws nothing.
  - [ ] **`on_error(err)` callback** - Lua can hook errors itself (nice for "show a message then restart the level").
  - [ ] **`print()` goes to the console AND the UMML dashboard** (if open) - so people scripting games can see their logs.
  - [ ] **UMMLLuaTest** - headless self test: loads a pile of .lua snippets and checks the results (math, strings, tables, loops, functions, closures, error cases), wired into `test.bat`.
  - [ ] **A Lua demo game** (`example/game.lua`) showing off the whole 2.5 renderer (sprites, tilemap, particles, animations) scripted in pure Lua.
  - [ ] **Save system bridge** - `save:set_string("playername", "Bobby")` / `save:get_int("money")` etc., so Lua games can use the shared MTT_saves system too.
  - [ ] Documentation in README.md: "Making a game with Lua" section + full API reference for the Lua side.

  - Files (in a `umml.lua` subpackage so the main `umml` package stays tidy):
    - `src/umml/lua/UMMLLuaLexer.java` - tokeniser
    - `src/umml/lua/UMMLLuaParser.java` - source -> AST
    - `src/umml/lua/UMMLLuaAst.java` - the AST node types
    - `src/umml/lua/UMMLLuaInterpreter.java` - the evaluator
    - `src/umml/lua/UMMLLuaValue.java` - Lua values (number/string/bool/nil/table/function/java object)
    - `src/umml/lua/UMMLLuaTable.java` - Lua tables (array + map in one)
    - `src/umml/lua/UMMLLuaError.java` - runtime errors with line numbers
    - `src/umml/lua/UMMLLua.java` - entry point: `UMMLLua.run(script)` returns the result
    - `src/umml/lua/UMMLLuaGame.java` - ties the interpreter to the renderer + callbacks
    - `src/umml/lua/UMMLLuaTest.java` - the headless self test
    - `src/umml/lua/UMMLLuaExample.java` + `example/game.lua` - runnable demo

  - Milestones:
    - [ ] 1) Lexer + parser + interpreter (numbers/strings/tables/functions/closures + stdlib), proven by headless tests.
    - [ ] 2) The Java <-> Lua bridge (both directions) + `UMMLLuaGame`.
    - [ ] 3) Demo game + README/todo updates + `test.bat` wiring.

## UMML 3.5 (ideas, not promised)

- [ ] **Lua mods!** Give mods an optional `mod.lua` with an `on_load()` entry point - content creators write Lua, never Java. XML is for the data. Lua is for the mods function.
- [ ] **More stdlib** - `string` extras (sub, len, upper, lower), `table.insert/remove`, `math.random` seeds, a tiny `os.clock()` for dt-independent timing.

















# UMML 3.5

- 