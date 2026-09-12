package sh.orbis.orbzones.api;

@FunctionalInterface
public interface ZoneListenerRegistration extends AutoCloseable {
    @Override
    void close();
}
