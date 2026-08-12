# VCM / IPC protocol-related classes

- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/MessageStructure.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/TcpClient.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/TcpClientHandler.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/TcpClientNotStatic.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/config/CompatibleNettyConfig.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/dto/IPCArriveDestinationDTO.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/handlers/IPCDestinationHandler.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/handlers/IPCDestinationPlanHandler.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/handlers/IPCLocationHandler.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/handlers/IPCLoginHandler.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/handlers/IPCPathPlanHandler.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/handlers/IPCRemoteSetHandler.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/handlers/IPCStartAndStopHandler.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/handlers/IPCStartEndPointHandler.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/handlers/IPCVehicleAccelerationDecelerationHandler.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/handlers/IPCVehicleSteeringWheelHandler.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/interfaces/Handler.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/netty/NettyServer.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/netty/NettyServerDecodeHandler.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/netty/NettyServerEncodeHandler.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/netty/NettyServerHandler.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/netty/NettyServerInitializer.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/rest/IPCRemoteControlController.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/rest/IPCSimulatorController.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/service/IPCChannelService.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/timerTask/IPCStartTimerTask.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/timerTask/IPCStartToEndTimerTask.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/util/MessageUtils.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/websocket/LoggerMessage.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/websocket/WebSocketConfig.java`
- `com-amygo/com-amygo-vcm/src/main/java/com/amygo/vcm/websocket/WebSocketServer.java`
- `com-amygo/com-amygo-vcm/src/test/java/org/com/amygo/vcm/CreateIpcDestinationMessage.java`
- `com-amygo/com-amygo-vcm/src/test/java/org/com/amygo/vcm/CreateIpcLocationMessage.java`
- `com-amygo/com-amygo-vcm/src/test/java/org/com/amygo/vcm/CreateIpcLoginMessage.java`
- `com-amygo/com-amygo-vcm/src/test/java/org/com/amygo/vcm/IPCSimulator.java`

Total: 35

## Notes
- Netty TCP server default port historically 9001.
- Handlers cover login, location, path plan, start/stop, remote set, accel/steer.
- Low-level speed/steering commands must be isolated; not part of RaaS cloud command set.
