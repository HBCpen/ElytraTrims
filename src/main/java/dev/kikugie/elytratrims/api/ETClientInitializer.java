package dev.kikugie.elytratrims.api;

@FunctionalInterface
public interface ETClientInitializer {
    String EP_NAME = "elytratrims-client";

    void onInitializeClientET();

    /**
     * Determines the ordering for the entrypoint invocation.
     * The default priority is {@code 0} and higher priorities execute first.
     * Elytra Trims' own entrypoint priority is {@code 1000}.
     */
    default int priority() { return 0; }
}
