package com.felixcasa.backendjava.model;

import java.math.BigInteger;
import java.util.Objects;

public enum VersionComparisonMode {
    ANY_DIFFERENT,
    HIGHER_ONLY;

    public boolean qualifies(String fetchedTag, String recordedTag) {
        if (Objects.equals(fetchedTag, recordedTag)) {
            return false;
        }
        if (this == ANY_DIFFERENT) {
            return true;
        }
        String[] fetched = numericComponents(fetchedTag);
        String[] recorded = numericComponents(recordedTag);
        if (fetched == null || recorded == null) {
            return true;
        }
        for (int index = 0; index < Math.max(fetched.length, recorded.length); index++) {
            BigInteger fetchedNumber = index < fetched.length ? new BigInteger(fetched[index]) : BigInteger.ZERO;
            BigInteger recordedNumber = index < recorded.length ? new BigInteger(recorded[index]) : BigInteger.ZERO;
            int comparison = fetchedNumber.compareTo(recordedNumber);
            if (comparison != 0) {
                return comparison > 0;
            }
        }
        return false;
    }

    private static String[] numericComponents(String tag) {
        if (tag == null) {
            return null;
        }
        if (tag.startsWith("v") || tag.startsWith("V")) {
            tag = tag.substring(1);
        }
        String[] components = tag.split("\\.", -1);
        for (String component : components) {
            if (component.isEmpty() || component.chars().anyMatch(character -> character < '0' || character > '9')) {
                return null;
            }
        }
        return components;
    }
}
