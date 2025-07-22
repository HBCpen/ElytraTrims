package dev.kikugie.elytratrims.api;

@FunctionalInterface
public interface ETCommonInitializer {
    String EP_NAME = "elytratrims-common";
    void onInitializeCommonET();
}
