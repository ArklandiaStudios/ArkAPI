package com.arklandia.arkapi.providers;

public interface StabilityProvider {
    
    enum EmergencyState {
        NORMAL,
        CRITICAL,
        DOWN,
        KILL
    }

    boolean isFolia();
    double getMSPT();
    EmergencyState getEmergencyState();
    void setEmergencyState(EmergencyState state);
}
