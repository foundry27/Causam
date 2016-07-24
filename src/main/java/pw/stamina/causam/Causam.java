/*
 * Causam - A maximally decoupled event system for Java
 * Copyright (C) 2016 Foundry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pw.stamina.causam;

import pw.stamina.causam.api.EventBus;
import pw.stamina.causam.api.configuration.CausamConfiguration;
import pw.stamina.causam.internal.bus.EventBusFactory;

import java.util.function.BiFunction;

/**
 * @author Foundry
 */
public final class Causam {

    private Causam() {
        throw new IllegalStateException(Causam.class.getCanonicalName() + " cannot be instantiated.");
    }

    public static <C, L, E> EventBus<C, E> makeBus(Class<C> containerClass, Class<E> eventClass, BiFunction<Class<C>, Class<E>, CausamConfiguration<C, L, E>> configurationFunction) {
        return EventBusFactory.makeConfiguredBus(configurationFunction.apply(containerClass, eventClass));
    }

    public static String getVersion() {
        return "0.1.0";
    }

    public static String getFormattedVersion() {
        return "Causam v" + getVersion();
    }
}
