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
class ProxyListenerRegistry<T, C, L, E> implements ListenerRegistry<C, L, E> {

    private final T backingRegistry;

    private final BiFunction<T, C, Boolean> registrationFunction;

    private final BiFunction<T, C, Boolean> unregistrationFunction;

    private final BiFunction<E, T, Optional<Iterable<Listener<L, E>>>> listenerLookupFunction;

    ProxyListenerRegistry(Supplier<T> registrySupplier,
                          BiFunction<T, C, Boolean> registrationFunction,
                          BiFunction<T, C, Boolean> unregistrationFunction,
                          BiFunction<E, T, Optional<Iterable<Listener<L, E>>>> listenerLookupFunction) {
        this.backingRegistry = registrySupplier.get();
        this.registrationFunction = registrationFunction;
        this.unregistrationFunction = unregistrationFunction;
        this.listenerLookupFunction = listenerLookupFunction;
    }

    @Override
    public boolean register(C container) {
        return registrationFunction.apply(backingRegistry, container);
    }

    @Override
    public boolean unregister(C container) {
        return unregistrationFunction.apply(backingRegistry, container);
    }

    @Override
    public Optional<Iterable<Listener<L, E>>> findListeners(E event) {
        return listenerLookupFunction.apply(event, backingRegistry);
    }
}
