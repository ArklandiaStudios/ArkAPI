package com.arklandia.arkapi.providers;

public interface OptimizationProvider {
    double getLastTps();
    void setMobAiEnabled(boolean enabled);
    boolean isMobAiEnabled();
    void setStackingEnabled(boolean enabled);
    boolean isStackingEnabled();
}
