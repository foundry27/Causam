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

package pw.stamina.causam.internal.registration;

import pw.stamina.causam.api.Listener;
import pw.stamina.causam.api.ListenerRegistry;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * @author Foundry
 */
public final class ListenerRegistryFactory {
    private ListenerRegistryFactory() {}

    public static <T, C, L, E> ListenerRegistry<C, L, E>
    makeProxyRegistry(Supplier<T> registrySupplier,
                      BiFunction<T, C, Boolean> registrationFunction,
                      BiFunction<T, C, Boolean> unregistrationFunction,
                      BiFunction<E, T, Optional<Iterable<Listener<L, E>>>> listenerLookupFunction) {
        return new ProxyListenerRegistry<>(registrySupplier, registrationFunction, unregistrationFunction, listenerLookupFunction);
    }
}
