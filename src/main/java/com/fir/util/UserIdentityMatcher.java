package com.fir.util;

import java.util.Locale;
import java.util.Objects;

import com.fir.model.User;

public final class UserIdentityMatcher {

    private UserIdentityMatcher() {
    }

    public static boolean sameUser(User left, User right) {
        if (left == null || right == null) {
            return false;
        }

        if (left.getId() != null && right.getId() != null) {
            return Objects.equals(left.getId(), right.getId());
        }

        String leftEmail = normalize(left.getEmail());
        String rightEmail = normalize(right.getEmail());
        if (leftEmail != null && rightEmail != null) {
            return leftEmail.equals(rightEmail);
        }

        String leftAadhaar = normalize(left.getAadhaarNumber());
        String rightAadhaar = normalize(right.getAadhaarNumber());
        return leftAadhaar != null && leftAadhaar.equals(rightAadhaar);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}

