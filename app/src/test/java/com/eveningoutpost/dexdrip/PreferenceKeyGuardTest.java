package com.eveningoutpost.dexdrip;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import lombok.val;

/**
 * Guards the stored preference key behind the wifi uploader battery. The Java identifiers around it
 * are renamed; the key itself must not be, or every user's stored value is silently discarded.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class PreferenceKeyGuardTest {

    private static final String KEY = "\"parakeet_battery\"";

    // ===== stored preference key ========================================================================================

    /** Both writers of the uploader battery still use the legacy key. */
    @Test
    public void theUploaderBatteryKeepsItsLegacyPreferenceKey() throws Exception {
        // :: Setup
        val writers = new String[]{
                "src/main/java/com/eveningoutpost/dexdrip/services/WixelReader.java",
                "src/main/java/com/eveningoutpost/dexdrip/GcmListenerSvc.java",
        };

        // :: Act & Verify
        for (val writer : writers) {
            val source = new String(Files.readAllBytes(locate(writer).toPath()), StandardCharsets.UTF_8);
            assertWithMessage(writer + " still writes the legacy uploader battery key")
                    .that(source).contains(KEY);
        }
    }

    private static File locate(String path) {
        final File fromModule = new File(path);
        if (fromModule.isFile()) return fromModule;
        final File fromRoot = new File("app/" + path);
        assertWithMessage("source file was found; working directory is " + new File(".").getAbsolutePath())
                .that(fromRoot.isFile()).isTrue();
        return fromRoot;
    }
}
