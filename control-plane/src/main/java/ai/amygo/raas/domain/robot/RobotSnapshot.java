package ai.amygo.raas.domain.robot;

public record RobotSnapshot(
        String robotId,
        ConnectivityStatus connectivityStatus,
        OperationalStatus operationalStatus,
        MissionStatus missionStatus,
        BatteryStatus batteryStatus,
        LocalizationStatus localizationStatus,
        SafetyStatus safetyStatus,
        MaintenanceStatus maintenanceStatus
) {
    public boolean schedulable() {
        return connectivityStatus == ConnectivityStatus.ONLINE
                && operationalStatus == OperationalStatus.AVAILABLE
                && safetyStatus == SafetyStatus.NORMAL
                && localizationStatus == LocalizationStatus.LOCALIZED
                && batteryStatus != BatteryStatus.CRITICAL
                && maintenanceStatus != MaintenanceStatus.BLOCKED;
    }
}
