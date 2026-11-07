package com.mavencraft;

import fuzs.fantasticwings.common.flight.Flight;
import fuzs.fantasticwings.common.flight.apparatus.FlightApparatus;
import fuzs.fantasticwings.common.init.FlightApparatuses;
import fuzs.fantasticwings.common.init.ModRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;

/** Direct Fantastic Wings attachment bridge, loaded only when that optional mod is available. */
public final class FantasticWingsBridge {
    private FantasticWingsBridge() {}

    public static String currentWing(ServerPlayer player) {
        Flight flight = ModRegistry.FLIGHT_ATTACHMENT_TYPE.getOrDefault(player, Flight.VOID);
        return flight.wings()
                .map(holder -> holder.unwrapKey()
                        .map(key -> key.identifier().getPath())
                        .orElse(null))
                .orElse(null);
    }

    public static void restore(ServerPlayer player, String wing) {
        var wingKey = switch (wing) {
            case "angel" -> FlightApparatuses.ANGEL_FLIGHT_APPARATUS;
            case "parrot" -> FlightApparatuses.PARROT_FLIGHT_APPARATUS;
            case "slime" -> FlightApparatuses.SLIME_FLIGHT_APPARATUS;
            case "blue_butterfly" -> FlightApparatuses.BLUE_BUTTERFLY_FLIGHT_APPARATUS;
            case "monarch_butterfly" -> FlightApparatuses.MONARCH_BUTTERFLY_FLIGHT_APPARATUS;
            case "fire" -> FlightApparatuses.FIRE_FLIGHT_APPARATUS;
            case "bat" -> FlightApparatuses.BAT_FLIGHT_APPARATUS;
            case "fairy" -> FlightApparatuses.FAIRY_FLIGHT_APPARATUS;
            case "evil" -> FlightApparatuses.EVIL_FLIGHT_APPARATUS;
            case "dragon" -> FlightApparatuses.DRAGON_FLIGHT_APPARATUS;
            case "metallic" -> FlightApparatuses.METALLIC_FLIGHT_APPARATUS;
            default -> null;
        };
        if (wingKey == null) return;
        HolderLookup.RegistryLookup<FlightApparatus> apparatuses =
                player.registryAccess().lookupOrThrow(FlightApparatus.REGISTRY_KEY);
        Holder.Reference<FlightApparatus> holder = apparatuses.getOrThrow(wingKey);
        ModRegistry.FLIGHT_ATTACHMENT_TYPE.set(player, Flight.VOID.setWings(holder));
    }
}
