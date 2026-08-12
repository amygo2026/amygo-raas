package ai.amygo.raas.domain.robot;

import java.time.Instant;
import java.util.Objects;

public class Robot {
    private final String id;
    private final String tenantId;
    private final String siteId;
    private String displayName;
    private String modelProfile;
    private String adapterType;
    private ConnectivityStatus connectivityStatus;
    private OperationalStatus operationalStatus;
    private MissionStatus missionStatus;
    private BatteryStatus batteryStatus;
    private LocalizationStatus localizationStatus;
    private SafetyStatus safetyStatus;
    private MaintenanceStatus maintenanceStatus;
    private String leaseTaskId;
    private Instant leaseExpiresAt;
    private long version;

    public Robot(
            String id,
            String tenantId,
            String siteId,
            String displayName,
            String modelProfile,
            String adapterType
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.siteId = siteId;
        this.displayName = displayName;
        this.modelProfile = modelProfile;
        this.adapterType = adapterType;
        this.connectivityStatus = ConnectivityStatus.ONLINE;
        this.operationalStatus = OperationalStatus.AVAILABLE;
        this.missionStatus = MissionStatus.IDLE;
        this.batteryStatus = BatteryStatus.NORMAL;
        this.localizationStatus = LocalizationStatus.LOCALIZED;
        this.safetyStatus = SafetyStatus.NORMAL;
        this.maintenanceStatus = MaintenanceStatus.OK;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getSiteId() { return siteId; }
    public String getDisplayName() { return displayName; }
    public String getModelProfile() { return modelProfile; }
    public String getAdapterType() { return adapterType; }
    public ConnectivityStatus getConnectivityStatus() { return connectivityStatus; }
    public OperationalStatus getOperationalStatus() { return operationalStatus; }
    public MissionStatus getMissionStatus() { return missionStatus; }
    public BatteryStatus getBatteryStatus() { return batteryStatus; }
    public LocalizationStatus getLocalizationStatus() { return localizationStatus; }
    public SafetyStatus getSafetyStatus() { return safetyStatus; }
    public MaintenanceStatus getMaintenanceStatus() { return maintenanceStatus; }
    public String getLeaseTaskId() { return leaseTaskId; }
    public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
    public long getVersion() { return version; }

    public boolean hasActiveLease(Instant now) {
        return leaseTaskId != null && leaseExpiresAt != null && leaseExpiresAt.isAfter(now);
    }

    public void acquireLease(String taskId, Instant expiresAt) {
        this.leaseTaskId = taskId;
        this.leaseExpiresAt = expiresAt;
        this.operationalStatus = OperationalStatus.BUSY;
        this.version++;
    }

    public void releaseLease() {
        this.leaseTaskId = null;
        this.leaseExpiresAt = null;
        this.operationalStatus = OperationalStatus.AVAILABLE;
        this.missionStatus = MissionStatus.IDLE;
        this.version++;
    }

    public void markExecuting() {
        this.missionStatus = MissionStatus.EXECUTING;
        this.version++;
    }

    public void setConnectivityStatus(ConnectivityStatus connectivityStatus) {
        this.connectivityStatus = Objects.requireNonNull(connectivityStatus);
        this.version++;
    }

    public void markOnline() {
        this.connectivityStatus = ConnectivityStatus.ONLINE;
        this.version++;
    }

    public RobotSnapshot toSnapshot() {
        return new RobotSnapshot(
                id,
                connectivityStatus,
                operationalStatus,
                missionStatus,
                batteryStatus,
                localizationStatus,
                safetyStatus,
                maintenanceStatus
        );
    }
}
