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

import pw.stamina.causam.api.exception.ExceptionHandler;
import pw.stamina.causam.internal.implementations.annotated.method.AnnotatedMethodListenerRegistry;
import pw.stamina.causam.internal.implementations.common.BasicDispatcher;
import pw.stamina.causam.internal.implementations.common.BasicExceptionHandler;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * @author Foundry
 */
public final class Configurations {

    private Configurations() {}

    public static <C, E> BiFunction<Class<C>, Class<E>, CausamConfiguration<C, Method, E>> defaultAnnotated(ExceptionHandler... exceptionHandlers) {
        return (c, e) -> {
            ConfigurationBuilder<C, Method, E> b = ConfigurationBuilder.from(c, Method.class, e)
                    .usingRegistry(new AnnotatedMethodListenerRegistry<>(e, new ConcurrentHashMap<>(), false))
                    .usingDispatcher(new BasicDispatcher<>());
            return (exceptionHandlers.length > 0 ? b.withExceptionHandling(exceptionHandlers) : b.withExceptionHandling(BasicExceptionHandler.getInstance()));
        };
    }

    public static <C, E> BiFunction<Class<C>, Class<E>, CausamConfiguration<C, Method, E>> cachingAnnotated(ExceptionHandler... exceptionHandlers) {
        return (c, e) -> {
            ConfigurationBuilder<C, Method, E> b = ConfigurationBuilder.from(c, Method.class, e)
                    .usingRegistry(new AnnotatedMethodListenerRegistry<>(e, new ConcurrentHashMap<>(), true))
                    .usingDispatcher(new BasicDispatcher<>());
            return (exceptionHandlers.length > 0 ? b.withExceptionHandling(exceptionHandlers) : b.withExceptionHandling(BasicExceptionHandler.getInstance()));
        };
    }
}
