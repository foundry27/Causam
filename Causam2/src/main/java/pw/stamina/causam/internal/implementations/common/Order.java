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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@SuppressWarnings("unused")
public enum Order {
    @ReadOnly PRE,
    FIRST,
    EARLIER,
    EARLY,
    DEFAULT,
    LATE,
    LATER,
    LAST,
    @ReadOnly POST;

    private final boolean monitor;

    Order() {
        boolean monitor = false;
        try { monitor = Order.class.getField(name()).isAnnotationPresent(ReadOnly.class);
        } catch (NoSuchFieldException ignored) {}
        this.monitor = monitor;
    }

    public boolean isMonitor() {
        return monitor;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface ReadOnly {}
}
