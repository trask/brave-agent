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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import brave.Tracing;
import brave.propagation.ThreadLocalCurrentTraceContext;
import org.junit.After;
import org.junit.Before;

import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.impl.ImmutableTimerNameImpl;
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.Setter;
import org.glowroot.xyzzy.instrumentation.api.Span;
import org.glowroot.xyzzy.instrumentation.api.TimerName;
import org.glowroot.xyzzy.instrumentation.api.checker.Nullable;

public abstract class BaseTest {

    private static final TimerName DUMMY_TIMER_NAME = ImmutableTimerNameImpl.of("dummy", false);

    protected MockReporter reporter;
    protected ThreadContextThreadLocal threadContextThreadLocal;
    protected AgentImpl agent;
    protected ExecutorService executor;

    @Before
    public void beforeEach() {

        reporter = new MockReporter();

        Tracing tracing = Tracing.newBuilder()
                .currentTraceContext(ThreadLocalCurrentTraceContext.create())
                .localServiceName("my-service-name")
                .spanReporter(reporter)
                .build();

        threadContextThreadLocal = new ThreadContextThreadLocal();

        agent = new AgentImpl(tracing, threadContextThreadLocal);
        executor = Executors.newCachedThreadPool();
    }

    @After
    public void afterEach() {
        executor.shutdown();
    }

    protected Span startIncomingSpan(String transactionType, String transactionName,
            String message) {
        return agent.startIncomingSpan(transactionType, transactionName, NopGetter.INSTANCE,
                NopGetter.CARRIER, MessageSupplier.create(message), DUMMY_TIMER_NAME,
                threadContextThreadLocal.getHolder(), 0, 0);
    }

    protected Span startOutgoingSpan(String type, String text, String message) {
        return threadContextThreadLocal.getHolder().get().startOutgoingSpan(type, text,
                NopSetter.INSTANCE, NopSetter.CARRIER, MessageSupplier.create(message),
                DUMMY_TIMER_NAME);
    }

    private static class NopGetter implements Getter<Object> {

        private static final Getter<Object> INSTANCE = new NopGetter();

        private static final Object CARRIER = new Object();

        @Override
        public @Nullable String get(Object carrier, String key) {
            return null;
        }
    }

    private static class NopSetter implements Setter<Object> {

        private static final Setter<Object> INSTANCE = new NopSetter();

        private static final Object CARRIER = new Object();

        @Override
        public void put(Object carrier, String key, String value) {}
    }
}
