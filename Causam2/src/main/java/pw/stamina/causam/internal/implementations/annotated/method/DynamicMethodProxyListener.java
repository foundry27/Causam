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

package pw.stamina.causam.internal.implementations.annotated.method;

import pw.stamina.causam.api.Listener;
import pw.stamina.causam.internal.implementations.annotated.method.transformer.ReflectionMethodTransformer;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author Foundry
 */
public class DynamicMethodProxyListener<E> implements Listener<Method, E> {

    private final Method backingMethod;

    private final Consumer<E> dynamicInvoker;

    private final Class<? extends E> event;

    @SuppressWarnings("unchecked")
    public DynamicMethodProxyListener(Object methodParent, Method backingMethod, Class<? extends E> event) {
        this.event = event;
        this.dynamicInvoker = (Consumer<E>) ReflectionMethodTransformer.transform(Consumer.class, methodParent.getClass(), methodParent, (this.backingMethod = backingMethod));
    }

    @Override
    public void invoke(E event) throws Throwable {
        dynamicInvoker.accept(event);
    }

    @Override
    public Method getListenerObject() {
        return backingMethod;
    }

    @Override
    public Class<? extends E> getEventType() {
        return event;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamicMethodProxyListener<?> that = (DynamicMethodProxyListener<?>) o;
        return Objects.equals(backingMethod, that.backingMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(backingMethod);
    }
}
