package fm.mox.eventsourcingspike.domain;

import fm.mox.eventsourcingspike.common.EntityFactory;

import java.util.List;

public class ADomainEntityFactory implements EntityFactory<ADomainEntity> {
    @Override
    public ADomainEntity from(List<ADomainEntityCreated> domainEvents) {
        ADomainEntity aDomainEntity = new ADomainEntity();
        for (ADomainEntityCreated domainEvent : domainEvents) {
            switch (domainEvent) {
                case ADomainEntityCreated a -> aDomainEntity.handle(a);
            }
        }
        return aDomainEntity;
    }
}
