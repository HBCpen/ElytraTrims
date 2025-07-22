package dev.kikugie.elytratrims.api;

@FunctionalInterface
public interface ETClientInitializer {
    String EP_NAME = "elytratrims-client";
    void onInitializeClientET();
}
