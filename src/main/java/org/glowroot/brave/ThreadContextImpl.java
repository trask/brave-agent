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

import brave.ScopedSpan;
import brave.Tracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContext.Injector;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.brave.span.AsyncOutgoingSpanImpl;
import org.glowroot.brave.span.AsyncQuerySpanImpl;
import org.glowroot.brave.span.IncomingSpanImpl;
import org.glowroot.brave.span.OutgoingSpanImpl;
import org.glowroot.brave.span.QuerySpanImpl;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextPlus;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.engine.util.TwoPartCompletion;
import org.glowroot.xyzzy.instrumentation.api.AsyncQuerySpan;
import org.glowroot.xyzzy.instrumentation.api.AsyncSpan;
import org.glowroot.xyzzy.instrumentation.api.AuxThreadContext;
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.QueryMessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.QuerySpan;
import org.glowroot.xyzzy.instrumentation.api.Setter;
import org.glowroot.xyzzy.instrumentation.api.Span;
import org.glowroot.xyzzy.instrumentation.api.Timer;
import org.glowroot.xyzzy.instrumentation.api.TimerName;

public class ThreadContextImpl implements ThreadContextPlus {

    private final Tracing tracing;

    private final ThreadContextThreadLocal threadContextThreadLocal;

    private final IncomingSpanImpl incomingSpan;

    private int currentNestingGroupId;
    private int currentSuppressionKeyId;

    private final @Nullable TwoPartCompletion auxThreadAsyncCompletion;

    public ThreadContextImpl(Tracing tracing, ThreadContextThreadLocal threadContextThreadLocal,
            IncomingSpanImpl incomingSpan, int rootNestingGroupId, int rootSuppressionKeyId,
            @Nullable TwoPartCompletion auxThreadAsyncCompletion) {

        this.tracing = tracing;
        this.threadContextThreadLocal = threadContextThreadLocal;
        this.incomingSpan = incomingSpan;
        currentNestingGroupId = rootNestingGroupId;
        currentSuppressionKeyId = rootSuppressionKeyId;
        this.auxThreadAsyncCompletion = auxThreadAsyncCompletion;
    }

    @Override
    public boolean isInTransaction() {
        return true;
    }

    @Override
    public <C> Span startIncomingSpan(String transactionType, String transactionName,
            Getter<C> getter, C carrier, MessageSupplier messageSupplier, TimerName timerName,
            AlreadyInTransactionBehavior alreadyInTransactionBehavior) {
        return NopTransactionService.LOCAL_SPAN;
    }

    @Override
    public Span startLocalSpan(MessageSupplier messageSupplier, TimerName timerName) {
        return NopTransactionService.LOCAL_SPAN;
    }

    @Override
    public QuerySpan startQuerySpan(String queryType, String queryText,
            QueryMessageSupplier queryMessageSupplier, TimerName timerName) {
        return new QuerySpanImpl(tracing.tracer().startScopedSpan(queryText));
    }

    @Override
    public QuerySpan startQuerySpan(String queryType, String queryText, long queryExecutionCount,
            QueryMessageSupplier queryMessageSupplier, TimerName timerName) {
        return new QuerySpanImpl(tracing.tracer().startScopedSpan(queryText));
    }

    @Override
    public AsyncQuerySpan startAsyncQuerySpan(String queryType, String queryText,
            QueryMessageSupplier queryMessageSupplier, TimerName timerName) {
        return new AsyncQuerySpanImpl(tracing.tracer().nextSpan()
                .name(queryText)
                .start());
    }

    @Override
    public <C> Span startOutgoingSpan(String type, String text, Setter<C> setter, C carrier,
            MessageSupplier messageSupplier, TimerName timerName) {
        ScopedSpan scopedSpan = tracing.tracer().startScopedSpan(text);
        inject(scopedSpan.context(), setter, carrier);
        return new OutgoingSpanImpl(scopedSpan);
    }

    @Override
    public <C> AsyncSpan startAsyncOutgoingSpan(String type, String text, Setter<C> setter,
            C carrier, MessageSupplier messageSupplier, TimerName timerName) {
        brave.Span span = tracing.tracer().nextSpan()
                .name(text)
                .start();
        inject(span.context(), setter, carrier);
        return new AsyncOutgoingSpanImpl(span);
    }

    @Override
    public Timer startTimer(TimerName timerName) {
        return NopTransactionService.TIMER;
    }

    @Override
    public AuxThreadContext createAuxThreadContext() {
        return new AuxThreadContextImpl(tracing, threadContextThreadLocal, incomingSpan);
    }

    @Override
    public void setTransactionAsync() {
        incomingSpan.setAsync();
    }

    @Override
    public void setTransactionAsyncComplete() {
        // this is so that if setTransactionAsyncComplete is called from within an auxiliary thread
        // span, that the transaction won't be completed until that (active) auxiliary thread span
        // is completed (see AuxThreadSpanImpl.endInternal())
        if (auxThreadAsyncCompletion == null || auxThreadAsyncCompletion.completePart1()) {
            incomingSpan.setAsyncComplete();
        }
    }

    @Override
    public void setTransactionType(String transactionType, int priority) {}

    @Override
    public void setTransactionName(String transactionName, int priority) {}

    @Override
    public void setTransactionUser(String user, int priority) {
        incomingSpan.setUser(user);
    }

    @Override
    public void addTransactionAttribute(String name, String value) {}

    @Override
    public void setTransactionSlowThreshold(long threshold, TimeUnit unit, int priority) {}

    @Override
    public void setTransactionError(Throwable t) {
        incomingSpan.setError(t);
    }

    @Override
    public void setTransactionError(@Nullable String message) {}

    @Override
    public void setTransactionError(@Nullable String message, @Nullable Throwable t) {
        incomingSpan.setError(t);
    }

    @Override
    public void addErrorSpan(Throwable t) {}

    @Override
    public void addErrorSpan(String message) {}

    @Override
    public void addErrorSpan(String message, Throwable t) {}

    @Override
    public void trackResourceAcquired(Object resource, boolean withLocationStackTrace) {}

    @Override
    public void trackResourceReleased(Object resource) {}

    @Override
    public @Nullable ServletRequestInfo getServletRequestInfo() {
        return incomingSpan.getServletRequestInfo();
    }

    @Override
    public void setServletRequestInfo(@Nullable ServletRequestInfo servletRequestInfo) {
        incomingSpan.setServletRequestInfo(servletRequestInfo);
    }

    @Override
    public int getCurrentNestingGroupId() {
        return currentNestingGroupId;
    }

    @Override
    public void setCurrentNestingGroupId(int nestingGroupId) {
        this.currentNestingGroupId = nestingGroupId;
    }

    @Override
    public int getCurrentSuppressionKeyId() {
        return currentSuppressionKeyId;
    }

    @Override
    public void setCurrentSuppressionKeyId(int suppressionKeyId) {
        this.currentSuppressionKeyId = suppressionKeyId;
    }

    private static <C> void inject(TraceContext context, Setter<C> setter, C carrier) {
        Injector<C> injector = Propagation.B3_STRING.injector(new BraveSetter<C>(setter));
        injector.inject(context, carrier);
    }

    private static class BraveSetter<C> implements brave.propagation.Propagation.Setter<C, String> {

        private final Setter<C> setter;

        private BraveSetter(Setter<C> setter) {
            this.setter = setter;
        }

        @Override
        public void put(C carrier, String key, String value) {
            setter.put(carrier, key, value);
        }
    }
}
