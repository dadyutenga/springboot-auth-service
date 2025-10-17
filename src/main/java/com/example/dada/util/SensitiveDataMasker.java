package com.example.dada.util;

import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

@UtilityClass
public class SensitiveDataMasker {

    private static final String MASK_CHAR = "*";
    private static final int DEFAULT_VISIBLE_SUFFIX = 4;

    public String maskIdentifier(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        String trimmed = value.trim();
        if (trimmed.length() <= DEFAULT_VISIBLE_SUFFIX) {
            return MASK_CHAR.repeat(trimmed.length());
        }

        int maskLength = trimmed.length() - DEFAULT_VISIBLE_SUFFIX;
        return MASK_CHAR.repeat(maskLength) + trimmed.substring(trimmed.length() - DEFAULT_VISIBLE_SUFFIX);
    }
}
