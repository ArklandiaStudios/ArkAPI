package com.arklandia.arkapi;

import com.arklandia.arkapi.providers.StabilityProvider;
import com.arklandia.arkapi.util.SchedulerCompat;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class ArkAPI extends JavaPlugin implements StabilityProvider {

    private static ArkAPI instance;
    private final Map<Class<?>, Object> providers = new HashMap<>();
    private final boolean isFolia;
    private EmergencyState emergencyState = EmergencyState.NORMAL;

    public ArkAPI() {
        boolean folia;
        try {
            Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler");
            folia = true;
        } catch (Throwable ignored) {
            folia = false;
        }
        this.isFolia = folia;
    }

    @Override
    public void onEnable() {
        instance = this;
        registerProvider(StabilityProvider.class, this);
        
        // Auto-monitoring task
        SchedulerCompat.runRepeating(this, () -> {
            double mspt = getMSPT();
            if (mspt > 100.0 && emergencyState == EmergencyState.DOWN) setEmergencyState(EmergencyState.KILL);
            else if (mspt > 70.0 && emergencyState == EmergencyState.CRITICAL) setEmergencyState(EmergencyState.DOWN);
            else if (mspt > 55.0 && emergencyState == EmergencyState.NORMAL) setEmergencyState(EmergencyState.CRITICAL);
            else if (mspt < 40.0 && emergencyState != EmergencyState.NORMAL) setEmergencyState(EmergencyState.NORMAL);
        }, 100L, 100L);

        getLogger().info("ArkAPI enabled. Folia: " + isFolia);
    }

    public static ArkAPI getInstance() { return instance; }

    public <T> void registerProvider(Class<T> clazz, T provider) {
        providers.put(clazz, provider);
    }

    public <T> T getProvider(Class<T> clazz) {
        return clazz.cast(providers.get(clazz));
    }

    @Override public boolean isFolia() { return isFolia; }
    @Override public double getMSPT() { return isFolia ? Bukkit.getServer().getAverageTickTime() : Bukkit.getAverageTickTime(); }
    @Override public EmergencyState getEmergencyState() { return emergencyState; }
    @Override public void setEmergencyState(EmergencyState state) { 
        if (this.emergencyState != state) {
            this.emergencyState = state;
            getLogger().warning("SERVER EMERGENCY STATE CHANGED TO: " + state);
        }
    }
}
