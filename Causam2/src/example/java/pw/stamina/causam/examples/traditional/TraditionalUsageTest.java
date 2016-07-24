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

package pw.stamina.causam.examples.traditional;

import pw.stamina.causam.Causam;
import pw.stamina.causam.api.EventBus;
import pw.stamina.causam.api.configuration.Configurations;
import pw.stamina.causam.internal.implementations.annotated.Ordered;
import pw.stamina.causam.internal.implementations.annotated.Reactive;
import pw.stamina.causam.internal.implementations.common.Order;

/**
 * @author Foundry
 */
public class TraditionalUsageTest {
    private static final EventBus<Object, Event> bus = Causam.makeBus(Object.class, Event.class, Configurations.cachingAnnotated());

    public static void main(String[] args) {
        TraditionalUsageTest instance = new TraditionalUsageTest();
        bus.register(instance);
        bus.dispatch(new BasicEvent());

        bus.unregister(instance);
        bus.dispatch(new BasicEvent());
    }

    @Ordered(Order.LATE)
    @Reactive
    private void onBasicEventLate(BasicEvent event) {
        System.out.println("Late!");
    }

    @Reactive
    private void onBasicEvent(BasicEvent event) {
        System.out.println("Default!");
    }

    @Ordered(Order.EARLIER)
    @Reactive
    private void onBasicEventEarlier(BasicEvent event) {
        System.out.println("Earlier!");
    }

    @Ordered(Order.FIRST)
    @Reactive
    private void onBasicEventFirst(BasicEvent event) {
        System.out.println("First!");
    }

    @Ordered(Order.EARLY)
    @Reactive
    private void onBasicEventEarly(BasicEvent event) {
        System.out.println("Early!");
    }
}
