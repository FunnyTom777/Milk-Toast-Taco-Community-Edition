package umml;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * A runnable demo of the UMML Renderer - also the best way to learn it.
 *
 * <p>Run with: java umml.UMMLRendererExample
 *
 * <p>What it shows:
 * <ul>
 *   <li>how to open a window ({@link UMMLRenderer#open})</li>
 *   <li>how to make a sprite from an image and from a plain colour</li>
 *   <li>how to move a sprite yourself (arrow keys) and let it move itself
 *       ({@link UMMLSprite#setVelocity} - the coin drifts on its own)</li>
 *   <li>how to make a sprite play an animation ({@link UMMLAnimation} - the
 *       gem flickers between frames on its own)</li>
 *   <li>how to build a level from a tile map ({@link UMMLTilemap} - the
 *       grass floor is a grid of tiles)</li>
 *   <li>how to add a particle effect ({@link UMMLParticleSystem} - clicking
 *       makes a burst of sparks)</li>
 *   <li>how to read the keyboard and mouse ({@link UMMLInput})</li>
 *   <li>how to draw shapes and text ({@link UMMLRenderer#fillCircle},
 *       {@link UMMLRenderer#drawText})</li>
 *   <li>how to make the camera follow a sprite
 *       ({@link UMMLRenderer#centerOn})</li>
 * </ul>
 *
 * <p>Controls: arrow keys move the player, SPACE makes the player spin,
 * clicking makes a particle explosion. ESC closes the window.
 */
public final class UMMLRendererExample {

    private UMMLRendererExample() {}

    public static void main(String[] args) {
        // 1) Open a window. Returns a renderer ready for the game loop.
        UMMLRenderer game = UMMLRenderer.open("UMML Renderer Demo", 800, 600);

        // 2) Make some sprites.
        //    If the picture can't be found you still get a pink box (never a crash).
        UMMLSprite player = new UMMLSprite(UMMLImage.load("assets/player.png"), 100, 100);
        player.setSize(48, 48); // a missing asset is 32x32 by default, so size it explicitly

        //    An animated sprite: four frames played as a loop. The gem
        //    changes frame automatically while the renderer updates it.
        UMMLAnimation gemAnimation = new UMMLAnimation(
                UMMLImage.solid(16, 16, new Color(0xFF, 0xC8, 0x2E)),
                UMMLImage.solid(16, 16, new Color(0xFF, 0xFF, 0x66)),
                UMMLImage.solid(16, 16, new Color(0xFF, 0xFF, 0xCC)),
                UMMLImage.solid(16, 16, new Color(0xFF, 0xFF, 0x66)));
        gemAnimation.setFrameDuration(0.15);
        UMMLSprite gem = new UMMLSprite(gemAnimation.currentImage(), 300, 160);
        gem.setAnimation(gemAnimation);

        //    A solid-colour sprite, and one that moves itself via velocity.
        UMMLSprite coin = new UMMLSprite(new Color(0xFF, 0xC8, 0x2E), 300, 240, 24, 24);
        coin.setVelocity(60, 0);              // drifts right 60 px/s on its own

        // 3) Register them: the renderer now updates + draws them every frame.
        game.addSprite(player);
        game.addSprite(gem);
        game.addSprite(coin);

        //    A tile map for the floor. One 32x32 grass tile repeated in a
        //    grid - no art files needed to show off tile maps.
        UMMLImage grassTile = UMMLImage.solid(32, 32, new Color(0x3A, 0x7B, 0x3A));
        int[][] floorGrid = new int[20][25];
        for (int[] row : floorGrid) {
            java.util.Arrays.fill(row, 0);   // every cell uses tile 0 (the grass)
        }
        UMMLTilemap floor = new UMMLTilemap(grassTile, 32, 32, floorGrid);
        floor.setPosition(-160, 540);

        //    A particle system for explosions. Configured once, then fired
        //    with burst() wherever the player clicks.
        UMMLParticleSystem sparks = new UMMLParticleSystem();
        sparks.setSpeed(200, 140);
        sparks.setLife(0.7, 0.4);
        sparks.setSize(5, 3);
        sparks.setGravity(500);
        sparks.setColor(new Color(0xFF, 0xC8, 0x2E));
        sparks.setEndColor(new Color(0xFF, 0x66, 0x00));
        game.addParticles(sparks);

        // 4) Update code runs ~60 times a second. 'delta' is the seconds since
        //    the last frame - always multiply movement by it so speed stays the
        //    same no matter the framerate.
        game.onUpdate(delta -> {
            double speed = 220 * delta;       // 220 pixels per second
            if (game.input().isDown(KeyEvent.VK_LEFT))  player.move(-speed, 0);
            if (game.input().isDown(KeyEvent.VK_RIGHT)) player.move(speed, 0);
            if (game.input().isDown(KeyEvent.VK_UP))    player.move(0, -speed);
            if (game.input().isDown(KeyEvent.VK_DOWN))  player.move(0, speed);

            if (game.input().wasPressed(KeyEvent.VK_SPACE)) player.setVelocity(60, 60);
            if (game.input().isDown(KeyEvent.VK_SPACE))     player.rotate(120 * delta);

            if (game.input().wasPressed(KeyEvent.VK_ESCAPE)) game.close();

            if (game.input().wasMousePressed(MouseEvent.BUTTON1)) {
                // Mouse is in screen coords; add the camera offset to place
                // the explosion in the world, right where you clicked.
                double wx = game.input().mouseX() + game.cameraX();
                double wy = game.input().mouseY() + game.cameraY();
                sparks.setPosition(wx, wy);
                sparks.burst(40);
            }

            // The coin moves itself (velocity), and bounces off the window edges.
            if (coin.x() > game.width() || coin.x() < 0) coin.setVelocity(-coin.velocityX(), coin.velocityY());

            // Camera follows the player so the world can be bigger than the screen.
            game.centerOn(player.centerX(), player.centerY());
        });

        // 5) Draw code runs after update. Everything drawn here sits behind
        //    the registered sprites (useful for backgrounds / ground).
        game.onDraw(renderer -> {
            renderer.drawTilemap(floor);                                       // the grass floor
            renderer.fillCircle(0, 0, 60, new Color(0x66, 0xCC, 0xFF));        // sky blob
            renderer.drawText("Arrows: move   SPACE: spin   Click: boom!   ESC: quit",
                    20, 30, Color.WHITE, 18);
        });

        // 6) Show the window and run.
        game.setClearColor(new Color(0x1B, 0x2A, 0x4A));
        game.start();
    }
}
