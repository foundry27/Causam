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

package pw.stamina.causam.internal.implementations.common;

import pw.stamina.causam.api.dispatch.Dispatcher;
import pw.stamina.causam.api.exception.ExceptionContext;
import pw.stamina.causam.api.exception.ExceptionHandler;
import pw.stamina.causam.api.Listener;

/**
 * @author Foundry
 */
public class BasicDispatcher<L, E> implements Dispatcher<L, E> {
    @Override
    public void dispatch(E event, Iterable<Listener<L, E>> listeners, Iterable<ExceptionHandler> exceptionHandlers) {
        for (Listener<L, E> listener : listeners) {
            try {
                listener.invoke(event);
            } catch (Throwable t) {
                ExceptionContext<E> context = new ExceptionContext<>(t, event, listener);
                for (ExceptionHandler handler : exceptionHandlers) {
                    handler.handleException(context);
                }
            }
        }
    }
}
