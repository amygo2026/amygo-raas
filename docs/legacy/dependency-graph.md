# Dependency graph (logical)

```
Amygo App  --REST/JWT-->  appserver  --DB-->  amygo schema
Ops Web (Thymeleaf) ---->  fms        --DB-->  amygo schema
fms / appserver -------->  vcm (Netty/WebSocket) <--> Vehicle IPC
Website CMS/Portal ----->  website-*  --DB-->  website schema
SmartControl --UDP LAN-->  vehicle local control (not cloud)
```

## Maven modules (fms2026/com-amygo)
- parent com-amygo-parent
- appserver, common, fms, persis, vcm (+ discovery-eureka in Bitbucket variant)

## Android modules (Amygo)
- app, AmygoCore, AMGObject, NetwordLibrary, PubLibrary, DBLibrary, weight
