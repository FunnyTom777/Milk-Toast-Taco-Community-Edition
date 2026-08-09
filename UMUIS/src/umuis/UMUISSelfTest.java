package umuis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Headless self test for UMUIS. Parses every menu XML in a directory (the
 * project's {@code UMUIS/menus} by default) and checks that:
 *
 * <ol>
 *   <li>every file parses as a valid menu, and</li>
 *   <li>every button {@code target} points at an existing menu file.</li>
 * </ol>
 *
 * <p>Prints a report and exits with a non-zero code if anything failed.
 * Useful for validating menus without opening a window.
 */
public final class UMUISSelfTest {

    private UMUISSelfTest() {}

    public static void main(String[] args) {
        Path dir = args.length > 0 ? Path.of(args[0]) : UMUIS.menusDirectory();
        if (!Files.isDirectory(dir)) {
            System.err.println("Menu directory not found: " + dir);
            System.exit(1);
            return;
        }

        List<Path> files = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".xml"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(files::add);
        } catch (IOException e) {
            System.err.println("Could not list menu directory: " + e.getMessage());
            System.exit(1);
            return;
        }

        System.out.println();
        System.out.println("=== UMUIS - Unified MTT User Interface System ===");
        System.out.println("Directory  : " + dir);
        System.out.println("Menus found: " + files.size());
        System.out.println();

        int failed = 0;
        int elementCount = 0;
        for (Path file : files) {
            try {
                UMUISMenu menu = UMUISParser.parse(file);
                elementCount += menu.elements().size();
                List<String> problems = new ArrayList<>();
                for (UMUISElement el : menu.elements()) {
                    if (el.target() != null && !el.target().isBlank()) {
                        Path resolved = file.getParent().resolve(el.target()).normalize();
                        if (!Files.isRegularFile(resolved)) {
                            problems.add("button '" + el.text() + "' targets missing menu: " + el.target());
                        }
                    }
                }
                if (problems.isEmpty()) {
                    System.out.println("  OK    " + file.getFileName());
                } else {
                    failed++;
                    System.out.println("  FAIL  " + file.getFileName());
                    for (String p : problems) {
                        System.out.println("        - " + p);
                    }
                }
            } catch (Exception e) {
                failed++;
                System.out.println("  FAIL  " + file.getFileName());
                System.out.println("        - " + e.getMessage());
            }
        }

        System.out.println();
        System.out.println("Menus parsed : " + (files.size() - failed) + "/" + files.size());
        System.out.println("Elements     : " + elementCount);
        System.out.println("Failures     : " + failed);
        System.out.println();

        System.exit(failed == 0 ? 0 : 1);
    }
}
