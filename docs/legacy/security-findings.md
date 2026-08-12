# Security findings (read-only; no fixes applied)

| Repo | File | Signals |
|---|---|---|
| fms2026 | `com-amygo/com-amygo-appserver/src/main/java/com/amygo/appserver/dto/UserRegisterRequestDTO.java` | hardcoded_password_pattern |
| fms2026 | `com-amygo/com-amygo-appserver/src/main/java/com/amygo/appserver/dto/request/RemoteControlRequestDTO.java` | low_level_or_remote_control |
| fms2026 | `com-amygo/com-amygo-appserver/src/main/java/com/amygo/appserver/security/JwtAuthenticationRequest.java` | hardcoded_password_pattern |
| fms2026 | `com-amygo/com-amygo-appserver/src/main/java/com/amygo/appserver/security/JwtUser.java` | hardcoded_password_pattern |
| fms2026 | `com-amygo/com-amygo-appserver/src/main/java/com/amygo/appserver/rest/TestController.java` | low_level_or_remote_control |
| fms2026 | `com-amygo/com-amygo-persis/src/main/java/com/amygo/persis/domain/User.java` | hardcoded_password_pattern |
| fms2026 | `com-amygo/com-amygo-persis/src/main/java/com/amygo/persis/domain/Administrator.java` | hardcoded_password_pattern |
| fms2026 | `com-amygo/com-amygo-common/src/main/java/com/amygo/common/util/HttpUtil.java` | possible_secret_literal |
| fms2026 | `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/dto/VehicleSteeringWheelCommandDTO.java` | low_level_or_remote_control |
| fms2026 | `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/dto/VehicleAccelerationDecelerationCommandDTO.java` | low_level_or_remote_control |
| fms2026 | `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/handlers/IPCVehicleSteeringWheelHandler.java` | low_level_or_remote_control |
| fms2026 | `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/handlers/IPCVehicleAccelerationDecelerationHandler.java` | low_level_or_remote_control |
| fms2026 | `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/rest/IPCRemoteControlController.java` | low_level_or_remote_control |
| fms2026 | `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/security/Md5PasswordEncoder.java` | hardcoded_password_pattern |
| fms2026 | `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/infrastructure/persistence/jdbc/AdministratorRepositoryJdbc.java` | hardcoded_password_pattern |
| fms2026 | `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/domain/modle/Administrator.java` | hardcoded_password_pattern |
| fms2026 | `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/CarPathPlanController.java` | low_level_or_remote_control |
| fms2026 | `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/CarRemoteControlController.java` | low_level_or_remote_control |
| fms2026 | `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/facade/commondobject/AdministratorCommond.java` | hardcoded_password_pattern |
| fms2026 | `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/facade/commondobject/ProfileCommand.java` | hardcoded_password_pattern |
| fms2026 | `com-website/com-website-cms/src/main/java/com/website/cms/security/Md5PasswordEncoder.java` | hardcoded_password_pattern |
| fms2026 | `com-website/com-website-cms/src/main/java/com/website/cms/infrastructure/persistence/jdbc/AdministratorRepositoryJdbc.java` | hardcoded_password_pattern |
| fms2026 | `com-website/com-website-cms/src/main/java/com/website/cms/domain/modle/Administrator.java` | hardcoded_password_pattern |
| fms2026 | `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/facade/commondobject/AdministratorCommond.java` | hardcoded_password_pattern |
| fms2026 | `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/facade/commondobject/ProfileCommand.java` | hardcoded_password_pattern |
| fms2026 | `com-amygo/com-amygo-appserver/src/main/resources/application.properties` | hardcoded_password_pattern |
| fms2026 | `com-amygo/com-amygo-vcm/src/main/resources/application.properties` | hardcoded_password_pattern |
| fms2026 | `com-amygo/com-amygo-fms/src/main/resources/application.properties` | low_level_or_remote_control |
| fms2026 | `com-website/com-website-portal/src/main/resources/application.properties` | hardcoded_password_pattern |
| app-android2026 | `SmartControl/app/src/main/java/pad/smart/amygo/com/smartcontrol/control/SmartControl.java` | private_ip |
| app-android2026 | `Amygo/AMGObject/src/main/java/com/amygo/amgobject/bean/User.java` | hardcoded_password_pattern |
| app-android2026 | `Amygo/AMGObject/src/main/java/com/amygo/amgobject/request/ReqPwdLogin.java` | hardcoded_password_pattern |
| app-android2026 | `Amygo/AMGObject/src/main/java/com/amygo/amgobject/request/ReqRegisterUser.java` | hardcoded_password_pattern |
| app-android2026 | `Amygo/NetwordLibrary/src/main/java/android/mobile/amygo/com/networdlibrary/util/MsgCode.java` | possible_secret_literal |

Total files with signals: 34


## Priority notes
- SmartControl hardcoded LAN host/port is near-field teleop; RETIRE from cloud control path.
- Remote steering/accel endpoints in FMS/VCM are Legacy low-level controls; isolate in WRAP adapter, never expose as RaaS cloud commands.
- Re-verify any password/token literals before production use; rotate if real secrets.
