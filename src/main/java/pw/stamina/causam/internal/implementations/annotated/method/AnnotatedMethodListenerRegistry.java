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

import pw.stamina.causam.api.ListenerRegistry;
import pw.stamina.causam.api.Listener;
import pw.stamina.causam.internal.implementations.annotated.Ordered;
import pw.stamina.causam.internal.implementations.annotated.Reactive;
import pw.stamina.causam.internal.implementations.common.Order;
import pw.stamina.causam.internal.util.ConcurrentWeakHashMap;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * @author Foundry
 */
public class AnnotatedMethodListenerRegistry<C, E> implements ListenerRegistry<C, Method, E> {

    private final Class<E> eventType;

    private final boolean caching;

    private final Map<C, Set<Listener<Method, E>>> instanceListenerCache;

    private final Map<Class<? extends E>, Set<Listener<Method, E>>> eventToListenerMap;

    public AnnotatedMethodListenerRegistry(Class<E> eventType, Map<Class<? extends E>, Set<Listener<Method, E>>> backingMap, boolean caching) {
        this.eventToListenerMap = backingMap;
        this.eventType = eventType;
        this.instanceListenerCache = ((this.caching = caching) ? new ConcurrentWeakHashMap<>() : null);
    }

    @Override
    public boolean register(C container) {
        boolean added = false;
        for (Listener<Method, E> listener : (caching ? instanceListenerCache.computeIfAbsent(container, this::getInstanceListeners) : getInstanceListeners(container))) {
            added |= eventToListenerMap.computeIfAbsent(listener.getEventType(), s -> new ConcurrentSkipListSet<>((m1, m2) -> {
                Ordered firstOrdering = m1.getListenerObject().getDeclaredAnnotation(Ordered.class);
                Ordered secondOrdering = m2.getListenerObject().getDeclaredAnnotation(Ordered.class);
                int discriminant;
                if (firstOrdering != null && secondOrdering != null) {
                    discriminant = firstOrdering.value().compareTo(secondOrdering.value());
                } else if (firstOrdering == null && secondOrdering != null) {
                    discriminant = Order.DEFAULT.compareTo(secondOrdering.value());
                } else if (firstOrdering != null) {
                    discriminant = firstOrdering.value().compareTo(Order.DEFAULT);
                } else {
                    discriminant = 0;
                }
                return discriminant == 0 ? System.identityHashCode(m2) - System.identityHashCode(m1) : discriminant;
            })).add(listener);
        }
        return added;
    }

    @Override
    public boolean unregister(C container) {
        boolean removed = false;
        if (caching) {
            final Set<Listener<Method, E>> listenerCache = instanceListenerCache.get(container);
            if (listenerCache != null && !listenerCache.isEmpty()) {
                for (Listener<Method, E> listener : listenerCache) {
                    final Set<Listener<Method, E>> activeListeners = eventToListenerMap.get(listener.getEventType());
                    if (activeListeners != null && !activeListeners.isEmpty()) {
                        removed |= activeListeners.remove(listener);
                    }
                }
            }
        } else {
            for (Method m : container.getClass().getDeclaredMethods()) {
                if (m.isAnnotationPresent(Reactive.class) && m.getParameterCount() == 1 && eventType.isAssignableFrom(m.getParameterTypes()[0])) {
                    Set<Listener<Method, E>> activeListeners = eventToListenerMap.get(m.getParameterTypes()[0].asSubclass(eventType));
                    if (activeListeners != null && !activeListeners.isEmpty()) {
                        removed |= activeListeners.removeIf(l -> l.getListenerObject().getDeclaringClass() == container.getClass());
                    }
                }
            }
        }
        return removed;
    }

    @Override
    public Optional<Iterable<Listener<Method, E>>> findListeners(E event) {
        return Optional.ofNullable(eventToListenerMap.get(event.getClass()))
                .filter(set -> !set.isEmpty())
                .map(set -> (Iterable<Listener<Method, E>>) set);
    }

    private Set<Listener<Method, E>> getInstanceListeners(C p) {
        return Arrays.stream(p.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Reactive.class))
                .filter(method -> method.getParameterCount() == 1 && eventType.isAssignableFrom(method.getParameterTypes()[0]))
                .map(method -> (Listener<Method, E>) new DynamicMethodProxyListener<>(p, method, method.getParameterTypes()[0].asSubclass(eventType)))
                .collect(Collectors.toSet());
    }
}
