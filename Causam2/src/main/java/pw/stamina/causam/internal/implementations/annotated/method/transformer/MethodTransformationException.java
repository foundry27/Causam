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

package pw.stamina.causam.internal.implementations.annotated.method.transformer;

/**
 * @author Foundry
 */
public class MethodTransformationException extends RuntimeException {

    private static final long serialVersionUID = -2771117835000342459L;

    MethodTransformationException() {}

    MethodTransformationException(String message) {
        super(message);
    }

    MethodTransformationException(Throwable cause) {
        super(cause);
    }

    MethodTransformationException(String message, Throwable cause) {
        super(message, cause);
    }

    MethodTransformationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
