package com.sequenceiq.datalake.service.sdx.attach;

import java.util.Date;

import org.springframework.stereotype.Component;

/**
 * Provides a central logic point for generation of a detached SDX cluster name, as
 * well as reversing a detached name.
 */
@Component
public class SdxDetachNameGenerator {
    private static final String DELIMITER = "-";

    String generateDetachedClusterName(String originalName) {
        return originalName + DELIMITER + new Date().getTime();
    }

    String generateOriginalNameFromDetached(String detachedName) throws Exception {
        // Just find last instance of delimiter and cut.
        int delimiterPos = detachedName.lastIndexOf(DELIMITER);
        if (delimiterPos < 0) {
            throw new RuntimeException(
                    "Provided detached name for generateOriginalNameFromDetached does not have expected delimiter!"
            );
        }
        return detachedName.substring(0, delimiterPos);
    }
}
