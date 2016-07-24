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

package pw.stamina.causam.api.exception;

import pw.stamina.causam.api.Listener;

/**
 * @author Foundry
 */
public final class ExceptionContext<E> {
    private final Throwable cause;
    private final E event;
    private final Listener<?, E> listener;

    public ExceptionContext(Throwable cause, E event, Listener<?, E> listener) {
        this.cause = cause;
        this.event = event;
        this.listener = listener;
    }

    public Throwable getCause() {
        return cause;
    }

    public E getEvent() {
        return event;
    }

    public Listener<?, E> getListener() {
        return listener;
    }
}
