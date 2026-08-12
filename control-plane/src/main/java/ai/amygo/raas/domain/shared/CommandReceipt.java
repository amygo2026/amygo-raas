package ai.amygo.raas.domain.shared;

public record CommandReceipt(
        String commandId,
        CommandReceiptStatus status,
        String reasonCode,
        String detail
) {}
