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

package pw.stamina.causam.examples.contextual.context.contexts;

import pw.stamina.causam.examples.contextual.context.Stoppable;

public abstract class StoppableContext extends AbstractContext implements Stoppable {
    protected boolean stopped;

    @Override
    public void stop() {
        checkLock();
        stopped = true;
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }
}
