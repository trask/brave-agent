/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.brave;

import org.junit.Test;

import org.glowroot.xyzzy.instrumentation.api.Span;

import static org.assertj.core.api.Assertions.assertThat;

// the tests simulate the method calls that the engine makes, and then validate that the reporter
// collected the expected spans
public class MainThreadTest extends BaseTest {

    @Test
    public void testWithNoOutgoingSpans() {
        // when
        Span span = startIncomingSpan("A", "B", "C");
        span.end();

        // then
        assertThat(reporter.getSpans()).hasSize(1);
    }

    @Test
    public void testWithOneOutgoingSpan() {
        // when
        Span span = startIncomingSpan("A", "B", "C");

        Span serviceCallEntry = startOutgoingSpan("X", "Y", "Z");
        serviceCallEntry.end();

        span.end();

        // then
        assertThat(reporter.getSpans()).hasSize(2);
    }

    @Test
    public void testWithMultipleOutgoingSpans() {
        // when
        Span span = startIncomingSpan("A", "B", "C");

        for (int i = 0; i < 10; i++) {
            Span serviceCallEntry = startOutgoingSpan("X", "Y" + i, "Z" + i);
            serviceCallEntry.end();
        }

        span.end();

        // then
        assertThat(reporter.getSpans()).hasSize(11);
    }
}
