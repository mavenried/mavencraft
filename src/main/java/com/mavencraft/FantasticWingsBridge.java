package com.mavencraft;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/** Reflection-only bridge to Fantastic Wings, which remains an optional mod dependency. */
public final class FantasticWingsBridge {
    private static final String REGISTRY = "fuzs.fantasticwings.common.init.ModRegistry";
    private static final String FLIGHT = "fuzs.fantasticwings.common.flight.Flight";
    private static final String APPARATUSES = "fuzs.fantasticwings.common.init.FlightApparatuses";
    private static final String APPARATUS = "fuzs.fantasticwings.common.flight.apparatus.FlightApparatus";

    private FantasticWingsBridge() {}

    public static String currentWing(ServerPlayer player) throws ReflectiveOperationException {
        Object flight = invoke(
                staticField(REGISTRY, "FLIGHT_ATTACHMENT_TYPE"), "getOrDefault", player, staticField(FLIGHT, "VOID"));
        Object wings = invoke(flight, "wings");
        if (!(wings instanceof Optional<?> optional) || optional.isEmpty()) return null;

        Object key = invoke(optional.get(), "unwrapKey");
        if (!(key instanceof Optional<?> keyOptional) || keyOptional.isEmpty()) return null;
        return (String) invoke(invoke(keyOptional.get(), "identifier"), "getPath");
    }

    public static void restore(ServerPlayer player, String wing) throws ReflectiveOperationException {
        String fieldName = wing.toUpperCase(java.util.Locale.ROOT) + "_FLIGHT_APPARATUS";
        Object wingKey = staticField(APPARATUSES, fieldName);
        Object registryKey = staticField(APPARATUS, "REGISTRY_KEY");
        Object lookup = invoke(player.registryAccess(), "lookupOrThrow", registryKey);
        Object holder = invoke(lookup, "getOrThrow", wingKey);
        Object flight = invoke(staticField(FLIGHT, "VOID"), "setWings", holder);
        invoke(staticField(REGISTRY, "FLIGHT_ATTACHMENT_TYPE"), "set", player, flight);
    }

    private static Object staticField(String className, String fieldName) throws ReflectiveOperationException {
        Class<?> type = Class.forName(className);
        Field field = type.getField(fieldName);
        return field.get(null);
    }

    private static Object invoke(Object target, String methodName, Object... arguments)
            throws ReflectiveOperationException {
        Method match = null;
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != arguments.length) continue;
            Class<?>[] parameters = method.getParameterTypes();
            boolean compatible = true;
            for (int i = 0; i < parameters.length; i++) {
                if (arguments[i] != null && !parameters[i].isInstance(arguments[i])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                match = method;
                break;
            }
        }
        if (match == null) throw new NoSuchMethodException(target.getClass().getName() + "." + methodName);
        try {
            return match.invoke(target, arguments);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            if (cause instanceof Error error) throw error;
            throw e;
        }
    }
}
