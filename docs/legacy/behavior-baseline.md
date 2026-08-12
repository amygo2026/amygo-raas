# Behavior baseline (characterization targets)

## Passenger / requester journey (appserver + Amygo)
1. Auth: verify-code / password login, register, nickname, logout, token refresh
2. Availability: infoInitial (geo fence / hours / nearby cars / unfinished order)
3. Stations: selectStartLocation / selectEndLocation
4. Order: create, cancel, unlockAndGetOn, finish, detail, unfinished
5. Journey: start, modifyDestination, addStation, pullOver, resume
6. Vehicle location polling
7. Feedback / evaluation

## Ops journey (fms web)
1. Admin RBAC (administrator/role/menu/resource)
2. Car CRUD + monitor
3. Remote control / path plan command pages
4. Operation region / time / electronic fence
5. Orders list + track
6. Feedback/evaluation reply
7. Statistics + syslog

## Vehicle link (vcm)
1. TCP login/heartbeat
2. Location / destination / path events
3. Command dispatch and ACK semantics (needs deeper packet dictionary in L0)
4. WebSocket fanout to ops UI

## Characterization test candidates
- Order state transitions under cancel/complete race
- infoInitial decision matrix
- Fence/time window checks
- Idempotency gaps around unlock/open-door
- VCM reconnect and duplicate message handling
