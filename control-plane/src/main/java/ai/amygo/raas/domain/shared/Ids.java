package ai.amygo.raas.domain.shared;

import java.time.Instant;
import java.util.UUID;

public final class Ids {
    private Ids() {}

    public static String newId() {
        return Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
