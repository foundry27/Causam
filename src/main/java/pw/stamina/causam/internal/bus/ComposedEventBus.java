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
class ComposedEventBus<C, L, E> implements EventBus<C, E> {

    private final CausamConfiguration<C, L, E> configuration;

    ComposedEventBus(CausamConfiguration<C, L, E> configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean register(C container) {
        return configuration.getListenerRegistry().register(container);
    }

    @Override
    public boolean unregister(C container) {
        return configuration.getListenerRegistry().unregister(container);
    }

    @Override
    public <T extends E> T dispatch(T event) {
        configuration.getListenerRegistry().findListeners(event).ifPresent(it -> configuration.getDispatcher().dispatch(event, it, configuration.getExceptionHandlers()));
        return event;
    }

    @Override
    public Class<C> getContainerType() {
        return configuration.getContainerType();
    }

    @Override
    public Class<E> getEventType() {
        return configuration.getEventType();
    }
}
