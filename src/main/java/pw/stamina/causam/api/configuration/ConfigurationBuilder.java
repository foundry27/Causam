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

package pw.stamina.causam.api.configuration;

import pw.stamina.causam.api.Listener;
import pw.stamina.causam.api.ListenerRegistry;
import pw.stamina.causam.api.dispatch.Dispatcher;
import pw.stamina.causam.api.exception.ExceptionHandler;
import pw.stamina.causam.internal.registration.ListenerRegistryFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * @author Foundry
 */
public class ConfigurationBuilder<C, L, E> implements CausamConfiguration<C, L, E> {

    private final Class<C> containerClass;

    private final Class<E> eventClass;

    private ListenerRegistry<C, L, E> listenerRegistry;

    private Dispatcher<L, E> dispatcher;

    private List<ExceptionHandler> exceptionHandlers;

    private ConfigurationBuilder(Class<C> containerClass, Class<E> eventClass) {
        this.containerClass = containerClass;
        this.eventClass = eventClass;
    }

    public <T> ConfigurationBuilder<C, L, E>
    usingRegistry(Supplier<T> registrySupplier,
                  BiFunction<T, C, Boolean> registrationFunction,
                  BiFunction<T, C, Boolean> unregistrationFunction,
                  BiFunction<E, T, Optional<Iterable<Listener<L, E>>>> listenerLookupFunction) {
        listenerRegistry = ListenerRegistryFactory.makeProxyRegistry(registrySupplier, registrationFunction, unregistrationFunction, listenerLookupFunction);
        return this;
    }

    public ConfigurationBuilder<C, L, E> usingRegistry(ListenerRegistry<C, L, E> listenerRegistry) {
        this.listenerRegistry = listenerRegistry;
        return this;
    }

    public ConfigurationBuilder<C, L, E> usingDispatcher(Dispatcher<L, E> dispatcher) {
        this.dispatcher = dispatcher;
        return this;
    }

    public ConfigurationBuilder<C, L, E> withExceptionHandling(ExceptionHandler... handlers) {
        if (exceptionHandlers == null) exceptionHandlers = new ArrayList<>();
        Collections.addAll(exceptionHandlers, handlers);
        return this;
    }

    public static <C, L, E> ConfigurationBuilder<C, L, E> from(Class<C> containerClass, Class<L> listenerType, Class<E> eventClass) {
        return new ConfigurationBuilder<>(containerClass, eventClass);
    }

    @Override
    public Class<C> getContainerType() {
        return containerClass;
    }

    @Override
    public Class<E> getEventType() {
        return eventClass;
    }

    @Override
    public ListenerRegistry<C, L, E> getListenerRegistry() {
        return listenerRegistry;
    }

    @Override
    public Dispatcher<L, E> getDispatcher() {
        return dispatcher;
    }

    @Override
    public Iterable<ExceptionHandler> getExceptionHandlers() {
        return exceptionHandlers;
    }
}
