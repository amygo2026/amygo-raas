package ai.amygo.raas.domain.shared;

public record Actor(String type, String id) {
    public static Actor system(String id) {
        return new Actor("SYSTEM", id);
    }

    public static Actor user(String id) {
        return new Actor("USER", id);
    }
}
