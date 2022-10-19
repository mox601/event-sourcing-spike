package fm.mox.eventsourcingspike.domain;

import fm.mox.eventsourcingspike.common.DomainEntityInterface;

import java.util.ArrayList;
import java.util.List;

public class ADomainEntity implements DomainEntityInterface {
    private String id;
    private Long version;
    private final List<ADomainEntityCreated> events;

    public ADomainEntity() {
        this.events = new ArrayList<>();
        this.version = -1L;
    }

    //TODO this should be a command, not an event
    public void handle(ADomainEntityCreated aDomainEntityCreated) {
        this.id = aDomainEntityCreated.getEntityId();
        append(aDomainEntityCreated);
        nextVersion();
    }

    private void append(ADomainEntityCreated aDomainEntityCreated) {
        this.events.add(aDomainEntityCreated);
    }

    private void nextVersion() {
        this.version++;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Long getVersion() {
        return this.version;
    }

    @Override
    public List<ADomainEntityCreated> getEvents() {
        return events;
    }
}