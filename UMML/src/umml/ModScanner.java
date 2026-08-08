package umml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Scans a mods directory and produces a {@link UMMLReport}. This is the
 * worker behind {@link UMML#scan(Path, UMMLOptions)}.
 *
 * The scan is defensive: no corrupt or incomplete mod can crash it. Every
 * problem becomes a {@link UMMLError} that is attached to the offending mod
 * and surfaced in the report.
 */
public final class ModScanner {

    private ModScanner() {}

    public static UMMLReport scan(Path modsDir, UMMLOptions options) {
        List<UMMLMod> loaded = new ArrayList<>();
        List<UMMLMod> failed = new ArrayList<>();
        List<UMMLError> errors = new ArrayList<>();

        if (modsDir == null || !Files.isDirectory(modsDir)) {
            errors.add(new UMMLError(UMMLError.Type.IO_ERROR, UMMLError.Severity.ERROR,
                    null, null, "Mods directory not found", String.valueOf(modsDir)));
            return new UMMLReport(modsDir == null ? null : modsDir.toString(), loaded, failed, errors);
        }

        File[] folders = modsDir.toFile().listFiles(File::isDirectory);
        if (folders == null) {
            errors.add(new UMMLError(UMMLError.Type.IO_ERROR, UMMLError.Severity.ERROR,
                    null, null, "Could not list mods directory", modsDir.toString()));
            return new UMMLReport(modsDir.toString(), loaded, failed, errors);
        }

        Arrays.sort(folders, Comparator.comparing(File::getName));

        for (File folder : folders) {
            scanFolder(folder.toPath(), options, loaded, failed, errors);
        }

        // Resolve dependency load order.
        DependencyResolver.OrderResult order = DependencyResolver.order(loaded, options);
        List<UMMLMod> orderedLoaded = order.ordered;

        if (options.strict()) {
            // Unresolvable mods fail to load entirely.
            failed.addAll(order.unresolvable);
        } else {
            // Keep them visible, loaded last, so the player can still see them.
            orderedLoaded.addAll(order.unresolvable);
        }

        // Build the flat error list: directory errors first, then each mod's errors.
        List<UMMLError> allErrors = new ArrayList<>(errors);
        for (UMMLMod mod : orderedLoaded) allErrors.addAll(mod.modErrors());
        for (UMMLMod mod : failed) allErrors.addAll(mod.modErrors());

        return new UMMLReport(modsDir.toString(), orderedLoaded, failed, allErrors);
    }

    private static void scanFolder(Path folder, UMMLOptions options,
                                   List<UMMLMod> loaded, List<UMMLMod> failed,
                                   List<UMMLError> errors) {
        Path moddataFile = folder.resolve("moddata.xml");
        if (!Files.isRegularFile(moddataFile)) {
            if (options.reportFoldersWithoutModdata()) {
                UMMLMod mod = new UMMLMod(folder, null, null);
                mod.addError(new UMMLError(UMMLError.Type.MISSING_MODDATA, UMMLError.Severity.ERROR,
                        folder.getFileName().toString(), null,
                        "folder has no moddata.xml and was not loaded", null));
                failed.add(mod);
            }
            return;
        }

        try {
            ModParser.Result result = ModParser.parse(moddataFile, options);
            if (result.success()) {
                loaded.add(result.mod());
            } else {
                failed.add(result.mod());
            }
        } catch (Exception e) {
            UMMLMod mod = new UMMLMod(folder, moddataFile, null);
            mod.addError(new UMMLError(UMMLError.Type.IO_ERROR, UMMLError.Severity.ERROR,
                    folder.getFileName().toString(), null,
                    "could not read moddata.xml", e.getMessage()));
            failed.add(mod);
        }
    }
}
