package com.arklandia.arkapi.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class SchedulerCompat {

    public interface TaskHandle {
        void cancel();
    }

    private static final boolean IS_FOLIA = checkFolia();
    private static Method globalRegionSchedulerMethod;
    private static Method asyncSchedulerMethod;
    
    private static Method globalExecuteMethod;
    private static Method globalRunDelayedMethod;
    private static Method globalRunAtFixedRateMethod;
    
    private static Method asyncRunNowMethod;
    
    private static Method entitySchedulerMethod;
    private static Method entityRunMethod;
    private static Method entityRunDelayedMethod;

    static {
        if (IS_FOLIA) {
            try {
                Class<?> serverClass = Bukkit.getServer().getClass();
                globalRegionSchedulerMethod = serverClass.getMethod("getGlobalRegionScheduler");
                asyncSchedulerMethod = serverClass.getMethod("getAsyncScheduler");
                
                Object globalScheduler = globalRegionSchedulerMethod.invoke(Bukkit.getServer());
                globalExecuteMethod = globalScheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
                globalRunDelayedMethod = globalScheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
                globalRunAtFixedRateMethod = globalScheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
                
                Object asyncScheduler = asyncSchedulerMethod.invoke(Bukkit.getServer());
                asyncRunNowMethod = asyncScheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
                
                entitySchedulerMethod = Player.class.getMethod("getScheduler");
            } catch (Throwable ignored) {
            }
        }
    }

    private SchedulerCompat() {
    }

    private static boolean checkFolia() {
        try {
            Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static void run(JavaPlugin plugin, Runnable task) {
        if (IS_FOLIA) {
            try {
                Object server = Bukkit.getServer();
                Object global = globalRegionSchedulerMethod.invoke(server);
                globalExecuteMethod.invoke(global, plugin, task);
                return;
            } catch (Throwable ignored) {
            }
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public static TaskHandle runLater(JavaPlugin plugin, Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            try {
                Object server = Bukkit.getServer();
                Object global = globalRegionSchedulerMethod.invoke(server);
                Consumer<Object> consumer = ignored -> task.run();
                Object handle = globalRunDelayedMethod.invoke(global, plugin, consumer, delayTicks);
                return () -> cancelReflectively(handle);
            } catch (Throwable ignored) {
                return () -> {};
            }
        }
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        return bukkitTask::cancel;
    }

    public static TaskHandle runRepeating(JavaPlugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (IS_FOLIA) {
            try {
                Object server = Bukkit.getServer();
                Object global = globalRegionSchedulerMethod.invoke(server);
                Consumer<Object> consumer = ignored -> task.run();
                Object handle = globalRunAtFixedRateMethod.invoke(global, plugin, consumer, delayTicks, periodTicks);
                return () -> cancelReflectively(handle);
            } catch (Throwable ignored) {
                return () -> {};
            }
        }
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        return bukkitTask::cancel;
    }

    public static void runAsync(JavaPlugin plugin, Runnable task) {
        if (IS_FOLIA) {
            try {
                Object server = Bukkit.getServer();
                Object async = asyncSchedulerMethod.invoke(server);
                Consumer<Object> consumer = ignored -> task.run();
                asyncRunNowMethod.invoke(async, plugin, consumer);
                return;
            } catch (Throwable ignored) {
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public static void runForEntity(JavaPlugin plugin, Player player, Runnable task) {
        if (player == null) {
            run(plugin, task);
            return;
        }
        if (IS_FOLIA) {
            try {
                Object entityScheduler = entitySchedulerMethod.invoke(player);
                if (entityRunMethod == null) {
                    entityRunMethod = entityScheduler.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class);
                }
                Consumer<Object> consumer = ignored -> task.run();
                entityRunMethod.invoke(entityScheduler, plugin, consumer, (Runnable) () -> {});
                return;
            } catch (Throwable ignored) {
            }
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public static void runForEntityLater(JavaPlugin plugin, Player player, Runnable task, long delayTicks) {
        if (player == null) {
            runLater(plugin, task, delayTicks);
            return;
        }
        if (IS_FOLIA) {
            try {
                Object entityScheduler = entitySchedulerMethod.invoke(player);
                if (entityRunDelayedMethod == null) {
                    entityRunDelayedMethod = entityScheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);
                }
                Consumer<Object> consumer = ignored -> task.run();
                entityRunDelayedMethod.invoke(entityScheduler, plugin, consumer, (Runnable) () -> {}, delayTicks);
                return;
            } catch (Throwable ignored) {
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    private static void cancelReflectively(Object handle) {
        if (handle == null) {
            return;
        }
        try {
            Method cancel = handle.getClass().getMethod("cancel");
            cancel.invoke(handle);
        } catch (Throwable ignored) {
        }
    }
}
