package ai.amygo.raas.adapter;

public record AdapterDescriptor(
        String adapterType,
        String version,
        String supportLevel
) {}
