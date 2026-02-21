package ing.boykiss.gearworks.launcher;

import net.fabricmc.loader.impl.launch.knot.KnotClient;

import java.util.Arrays;

/**
 * Entry point for GearworksLauncher.jar.
 * <p>
 * Two modes:
 * - Normal:       Shows the Vulkan launcher UI (version picker, download, launch).
 * - Knot launch:  {@code --knot-launch} flag present — skips UI and hands off
 * directly to KnotClient. Used by ProcessUtils when spawning
 * the game after the user clicks Play.
 */
public class LauncherMain {
    public static void main(String[] args) throws Exception {
        // LWJGL's thread-local MemoryStack defaults to 64 KB. VkInstance construction
        // internally enumerates all device extensions onto the stack on top of whatever
        // the caller already allocated, which easily blows the 64 KB limit on systems
        // with many Vulkan extensions. Must be set before the first stackPush() call.
        System.setProperty("org.lwjgl.system.stackSize", "256"); // KB

        if (hasFlag(args, ProcessUtils.KNOT_LAUNCH_FLAG)) {
            // Strip --knot-launch from args before handing off to Knot
            String[] knotArgs = Arrays.stream(args)
                    .filter(a -> !a.equals(ProcessUtils.KNOT_LAUNCH_FLAG))
                    .toArray(String[]::new);

            KnotClient.main(knotArgs);
        } else {
            new LauncherApp().run();
        }
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) if (arg.equals(flag)) return true;
        return false;
    }
}
