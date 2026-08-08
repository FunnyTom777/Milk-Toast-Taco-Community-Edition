package umml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orders discovered mods so that dependencies load before the mods that
 * need them. Mods whose dependencies can never be satisfied are separated
 * out so the scanner can decide (via {@link UMMLOptions#strict()}) whether
 * they should still load.
 */
public final class DependencyResolver {

    private DependencyResolver() {}

    /** Outcome of ordering: mods in load order plus the unresolvable ones. */
    public static final class OrderResult {
        final List<UMMLMod> ordered = new ArrayList<>();
        final List<UMMLMod> unresolvable = new ArrayList<>();
    }

    public static OrderResult order(List<UMMLMod> discovered, UMMLOptions options) {
        Map<String, UMMLMod> available = new HashMap<>();
        for (UMMLMod mod : discovered) {
            available.put(mod.name().toLowerCase(), mod);
        }

        OrderResult result = new OrderResult();
        Set<String> loaded = new HashSet<>();
        List<UMMLMod> remaining = new ArrayList<>(discovered);

        boolean progress = true;
        while (!remaining.isEmpty() && progress) {
            progress = false;
            List<UMMLMod> next = new ArrayList<>();
            for (UMMLMod mod : remaining) {
                if (dependenciesMet(mod, loaded)) {
                    result.ordered.add(mod);
                    loaded.add(mod.name().toLowerCase());
                    progress = true;
                } else {
                    next.add(mod);
                }
            }
            remaining = next;
        }

        if (remaining.isEmpty()) {
            return result;
        }

        // Anything left has a dependency that either does not exist at all,
        // or forms a dependency cycle.
        for (UMMLMod mod : remaining) {
            List<String> missing = new ArrayList<>();
            List<String> cyclic = new ArrayList<>();
            for (String dep : mod.dependencies()) {
                if (available.containsKey(dep.toLowerCase())) {
                    if (!loaded.contains(dep.toLowerCase())) cyclic.add(dep);
                } else {
                    missing.add(dep);
                }
            }

            if (!cyclic.isEmpty()) {
                mod.addError(new UMMLError(UMMLError.Type.CYCLIC_DEPENDENCY, UMMLError.Severity.ERROR,
                        mod.folderName(), mod.name(),
                        "dependency cycle detected", String.join(", ", cyclic)));
            }
            if (!missing.isEmpty()) {
                mod.addError(new UMMLError(UMMLError.Type.UNRESOLVED_DEPENDENCY, UMMLError.Severity.ERROR,
                        mod.folderName(), mod.name(),
                        "dependency not available", String.join(", ", missing)));
            }
            result.unresolvable.add(mod);
        }

        return result;
    }

    private static boolean dependenciesMet(UMMLMod mod, Set<String> loaded) {
        if (mod.dependencies().isEmpty()) return true;
        for (String dep : mod.dependencies()) {
            if (!loaded.contains(dep.toLowerCase())) return false;
        }
        return true;
    }
}
