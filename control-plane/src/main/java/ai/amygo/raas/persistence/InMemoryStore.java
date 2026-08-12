package ai.amygo.raas.persistence;

import ai.amygo.raas.domain.mission.Task;
import ai.amygo.raas.domain.robot.Robot;
import ai.amygo.raas.domain.shared.RobotEvent;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryStore {
    private final Map<String, Robot> robots = new ConcurrentHashMap<>();
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final Map<String, String> commandIdempotency = new ConcurrentHashMap<>();
    private final List<RobotEvent> events = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> auditLogs = new CopyOnWriteArrayList<>();

    public void saveRobot(Robot robot) {
        robots.put(robot.getId(), robot);
    }

    public Optional<Robot> findRobot(String id) {
        return Optional.ofNullable(robots.get(id));
    }

    public Collection<Robot> listRobots(String tenantId, String siteId) {
        return robots.values().stream()
                .filter(r -> r.getTenantId().equals(tenantId) && r.getSiteId().equals(siteId))
                .toList();
    }

    public void saveTask(Task task) {
        tasks.put(task.getId(), task);
    }

    public Optional<Task> findTask(String id) {
        return Optional.ofNullable(tasks.get(id));
    }

    public Collection<Task> listTasks(String tenantId, String siteId) {
        return tasks.values().stream()
                .filter(t -> t.getTenantId().equals(tenantId) && t.getSiteId().equals(siteId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    public boolean rememberCommandIdempotency(String tenantId, String key, String commandId) {
        String composite = tenantId + "::" + key;
        String existing = commandIdempotency.putIfAbsent(composite, commandId);
        return existing == null || existing.equals(commandId);
    }

    public Optional<String> findCommandByIdempotency(String tenantId, String key) {
        return Optional.ofNullable(commandIdempotency.get(tenantId + "::" + key));
    }

    public void appendEvent(RobotEvent event) {
        events.add(event);
    }

    public boolean hasEvent(String eventId) {
        return events.stream().anyMatch(e -> e.eventId().equals(eventId));
    }

    public List<RobotEvent> listEvents(String tenantId, String siteId) {
        List<RobotEvent> out = new ArrayList<>();
        for (RobotEvent e : events) {
            if (e.tenantId().equals(tenantId) && e.siteId().equals(siteId)) {
                out.add(e);
            }
        }
        return out;
    }

    public void appendAudit(Map<String, Object> entry) {
        auditLogs.add(entry);
    }

    public List<Map<String, Object>> listAudit(String tenantId) {
        return auditLogs.stream().filter(a -> tenantId.equals(a.get("tenantId"))).toList();
    }
}
