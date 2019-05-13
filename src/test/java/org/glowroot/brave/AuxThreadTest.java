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

import java.util.concurrent.Future;

import org.junit.Test;

import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextPlus;
import org.glowroot.xyzzy.instrumentation.api.AuxThreadContext;
import org.glowroot.xyzzy.instrumentation.api.Span;

import static org.assertj.core.api.Assertions.assertThat;

// the tests simulate the method calls that the engine makes, and then validate that the reporter
// collected the expected spans
public class AuxThreadTest extends BaseTest {

    @Test
    public void testWithOneAuxThread() throws Exception {
        // when
        Span incomingSpan = startIncomingSpan("A", "B", "C");

        ThreadContextPlus threadContext = threadContextThreadLocal.getHolder().get();
        final AuxThreadContext auxThreadContext = threadContext.createAuxThreadContext();

        Future<?> future = executor.submit(new Runnable() {
            @Override
            public void run() {
                Span auxSpan = auxThreadContext.start();
                Span outgoingSpan = startOutgoingSpan("X", "Y", "Z");
                outgoingSpan.end();
                auxSpan.end();
            }
        });
        future.get();

        incomingSpan.end();

        // then
        assertThat(reporter.getSpans()).hasSize(2);
    }
}
