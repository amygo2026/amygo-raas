package ai.amygo.raas.domain.mission;

import java.util.Locale;
import java.util.Set;

/**
 * Supported service mission task types for MVP Mock / Simulator.
 * Unsupported types must surface CAPABILITY_NOT_SUPPORTED at the edge.
 */
public final class TaskTypes {
    public static final String DELIVERY = "DELIVERY";
    public static final String CLEANING = "CLEANING";
    public static final String HOTEL_DELIVERY = "HOTEL_DELIVERY";

    private static final Set<String> SUPPORTED = Set.of(DELIVERY, CLEANING, HOTEL_DELIVERY);

    private TaskTypes() {}

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return DELIVERY;
        }
        String t = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return switch (t) {
            case "DELIVERY", "DELIVER" -> DELIVERY;
            case "CLEANING", "CLEAN" -> CLEANING;
            case "HOTEL", "HOTEL_DELIVERY", "HOTELDELIVERY" -> HOTEL_DELIVERY;
            default -> t;
        };
    }

    public static boolean isSupported(String normalized) {
        return SUPPORTED.contains(normalized);
    }

    public static String requiredCapability(String normalizedTaskType) {
        return switch (normalizedTaskType) {
            case CLEANING -> "cleaning";
            case HOTEL_DELIVERY -> "compartment";
            default -> "delivery";
        };
    }

    public static String startCommandType(String normalizedTaskType) {
        return switch (normalizedTaskType) {
            case CLEANING -> "CLEANING_START";
            case HOTEL_DELIVERY -> "HOTEL_DELIVERY_START";
            default -> "DELIVERY_START";
        };
    }

    /** Prefer model profiles that hint the scenario (sim.cleaning.v1 etc.). */
    public static boolean prefersProfile(String normalizedTaskType, String modelProfile) {
        if (modelProfile == null) {
            return false;
        }
        String p = modelProfile.toLowerCase(Locale.ROOT);
        return switch (normalizedTaskType) {
            case CLEANING -> p.contains("clean");
            case HOTEL_DELIVERY -> p.contains("hotel");
            case DELIVERY -> p.contains("delivery") || p.contains("deliver");
            default -> false;
        };
    }
}
