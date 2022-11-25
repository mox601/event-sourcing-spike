package fm.mox.eventsourcingspike.adapter.persistence;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fm.mox.eventsourcingspike.domain.DomainEvent;

public class InMemoryDomainEventsPersistenceAdapter implements DomainEventsPersistenceAdapter {

    private final Map<String, Map<Long, List<DomainEvent>>> map;

    public InMemoryDomainEventsPersistenceAdapter(Map<String, Map<Long, List<DomainEvent>>> map) {
        this.map = map;
    }

    @Override
    public List<DomainEvent> findById(String entityType, String entityId) {
        String key = keyFrom(entityType, entityId);
        return this.map.get(key)
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .flatMap((Function<Map.Entry<Long, List<DomainEvent>>, Stream<DomainEvent>>) longListEntry -> longListEntry.getValue()
                        .stream())
                .collect(Collectors.toList());
    }

    @Override
    public String save(String entityType, String id, List<DomainEvent> events, Long version) {
        // use version in key
        String key = keyFrom(entityType, id);
        Map<Long, List<DomainEvent>> versionToEvents = this.map.get(key);
        if (versionToEvents == null) {
            //this is the first entry ever, initialize a map and update it
            versionToEvents = new HashMap<>();
            versionToEvents.put(version, events);
            this.map.put(key, versionToEvents);
        } else {
            // check existing map for the version: if null, can
            if (versionToEvents.get(version) == null) {
                //can write this only if it respects the key order
                Comparator<Map.Entry<Long, List<DomainEvent>>> entryComparator = Map.Entry.comparingByKey();
                Comparator<Map.Entry<Long, List<DomainEvent>>> reversed = entryComparator.reversed();
                List<Long> descendingVersionEntries = versionToEvents
                        .keySet()
                        .stream()
                        .sorted(Comparator.reverseOrder())
                        .toList();
                Long lastVersion = descendingVersionEntries.get(0); //read the first since it's sorted reversed
                if (lastVersion < version) {
                    versionToEvents.put(version, events);
                } else {
                    throw new RuntimeException(
                            "can't write new version: " + version + " is lower than the current last version " + lastVersion);
                }
            } else {
                // can't write this, there's already the same version
                throw new RuntimeException("can't write new version: " + version + " is equal to the current last version");
            }
        }
        return key;
    }

    private String keyFrom(String entityType, String entityId) {
        return entityType + "-" + entityId;
    }
}
