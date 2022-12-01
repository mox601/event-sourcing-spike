package fm.mox.eventsourcingspike.common;

public interface CommandHandler<T> {

    void handle(T command);

}
