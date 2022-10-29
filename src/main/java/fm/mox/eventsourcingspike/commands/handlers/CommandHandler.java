package fm.mox.eventsourcingspike.commands.handlers;

public interface CommandHandler<T> {

    void handle(T command);

}
