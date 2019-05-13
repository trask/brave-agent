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

import java.util.concurrent.TimeUnit;

import brave.Tracing;
import brave.propagation.CurrentTraceContext.Scope;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.brave.span.IncomingSpanImpl;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextPlus;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.engine.util.TwoPartCompletion;
import org.glowroot.xyzzy.instrumentation.api.AuxThreadContext;
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.Setter;
import org.glowroot.xyzzy.instrumentation.api.Span;
import org.glowroot.xyzzy.instrumentation.api.Timer;

public class AuxThreadContextImpl implements AuxThreadContext {

    private final Tracing tracing;

    private final ThreadContextThreadLocal threadContextThreadLocal;

    private final IncomingSpanImpl incomingSpan;

    public AuxThreadContextImpl(Tracing tracing, ThreadContextThreadLocal threadContextThreadLocal,
            IncomingSpanImpl incomingSpan) {
        this.tracing = tracing;
        this.threadContextThreadLocal = threadContextThreadLocal;
        this.incomingSpan = incomingSpan;
    }

    @Override
    public Span start() {
        return start(false);
    }

    @Override
    public Span startAndMarkAsyncTransactionComplete() {
        return start(true);
    }

    private Span start(boolean completeAsyncTransaction) {
        ThreadContextThreadLocal.Holder threadContextHolder = threadContextThreadLocal.getHolder();
        ThreadContextPlus threadContext = threadContextHolder.get();
        if (threadContext != null) {
            if (completeAsyncTransaction) {
                threadContext.setTransactionAsyncComplete();
            }
            return NopTransactionService.LOCAL_SPAN;
        }
        Scope auxScope = tracing.currentTraceContext().newScope(incomingSpan.getTraceContext());
        TwoPartCompletion auxThreadAsyncCompletion = new TwoPartCompletion();
        threadContext = new ThreadContextImpl(tracing, threadContextThreadLocal, incomingSpan, 0, 0,
                auxThreadAsyncCompletion);
        threadContextHolder.set(threadContext);
        if (completeAsyncTransaction) {
            threadContext.setTransactionAsyncComplete();
        }
        return new AuxThreadSpanImpl(auxScope, threadContextHolder, auxThreadAsyncCompletion,
                incomingSpan);
    }

    private static class AuxThreadSpanImpl implements Span {

        private final Scope scope;

        private final ThreadContextThreadLocal.Holder threadContextHolder;

        private final TwoPartCompletion auxThreadAsyncCompletion;
        private final IncomingSpanImpl incomingSpan;

        private AuxThreadSpanImpl(Scope scope, ThreadContextThreadLocal.Holder threadContextHolder,
                TwoPartCompletion auxThreadAsyncCompletion, IncomingSpanImpl incomingSpan) {
            this.scope = scope;
            this.threadContextHolder = threadContextHolder;
            this.auxThreadAsyncCompletion = auxThreadAsyncCompletion;
            this.incomingSpan = incomingSpan;
        }

        @Override
        public void end() {
            endInternal();
        }

        @Override
        public void endWithLocationStackTrace(long threshold, TimeUnit unit) {
            endInternal();
        }

        @Override
        public void endWithError(Throwable t) {
            endInternal();
        }

        @Override
        public void endWithError(@Nullable String message) {
            endInternal();
        }

        @Override
        public void endWithError(@Nullable String message, Throwable t) {
            endInternal();
        }

        @Override
        public void endWithInfo(Throwable t) {
            endInternal();
        }

        @Override
        public Timer extend() {
            throw new UnsupportedOperationException(
                    "extend() shouldn't be called on auxiliary thread span");
        }

        @Override
        public @Nullable Object getMessageSupplier() {
            return null;
        }

        @Override
        @Deprecated
        public <R> void propagateToResponse(R response, Setter<R> setter) {}

        @Override
        @Deprecated
        public <R> void extractFromResponse(R response, Getter<R> getter) {}

        private void endInternal() {
            scope.close();
            threadContextHolder.set(null);
            if (auxThreadAsyncCompletion.completePart2()) {
                incomingSpan.setAsyncComplete();
            }
        }
    }
}
