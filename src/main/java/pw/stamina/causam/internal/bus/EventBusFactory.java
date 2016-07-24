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

package pw.stamina.causam.internal.bus;

import pw.stamina.causam.api.EventBus;
import pw.stamina.causam.api.configuration.CausamConfiguration;

/**
 * @author Foundry
 */
public final class EventBusFactory {
    private EventBusFactory() {}

    public static <C, L, E> EventBus<C, E> makeConfiguredBus(CausamConfiguration<C, L, E> configuration) {
        return new ComposedEventBus<>(configuration);
    }
}
