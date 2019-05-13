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
package org.glowroot.brave.span;

import java.util.concurrent.TimeUnit;

import brave.Span;
import brave.propagation.TraceContext;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.util.TwoPartCompletion;
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.Setter;
import org.glowroot.xyzzy.instrumentation.api.ThreadContext.ServletRequestInfo;

import static com.google.common.base.Preconditions.checkNotNull;

public class IncomingSpanImpl implements org.glowroot.xyzzy.instrumentation.api.Span {

    private final Span span;

    private final MessageSupplier messageSupplier;
    private final ThreadContextThreadLocal.Holder threadContextHolder;

    private volatile @Nullable ServletRequestInfo servletRequestInfo;

    private volatile @MonotonicNonNull String user;

    private volatile @Nullable Throwable exception;

    private volatile @Nullable TwoPartCompletion asyncCompletion;

    public IncomingSpanImpl(Span span, MessageSupplier messageSupplier,
            ThreadContextThreadLocal.Holder threadContextHolder) {
        this.span = span;
        this.messageSupplier = messageSupplier;
        this.threadContextHolder = threadContextHolder;
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
        span.error(t);
        endInternal();
    }

    @Override
    public void endWithError(@Nullable String message) {
        endInternal();
    }

    @Override
    public void endWithError(@Nullable String message, Throwable t) {
        span.error(t);
        endInternal();
    }

    @Override
    public void endWithInfo(Throwable t) {
        // intentionally not setting exception since "info"
        endInternal();
    }

    @Override
    public org.glowroot.xyzzy.instrumentation.api.Timer extend() {
        throw new UnsupportedOperationException("extend() shouldn't be called on incoming span");
    }

    @Override
    public Object getMessageSupplier() {
        return messageSupplier;
    }

    @Override
    @Deprecated
    public <R> void propagateToResponse(R response, Setter<R> setter) {}

    @Override
    @Deprecated
    public <R> void extractFromResponse(R response, Getter<R> getter) {}

    public TraceContext getTraceContext() {
        return span.context();
    }

    public @Nullable ServletRequestInfo getServletRequestInfo() {
        return servletRequestInfo;
    }

    public void setServletRequestInfo(@Nullable ServletRequestInfo servletRequestInfo) {
        this.servletRequestInfo = servletRequestInfo;
    }

    public void setAsync() {
        asyncCompletion = new TwoPartCompletion();
    }

    public void setAsyncComplete() {
        checkNotNull(asyncCompletion);
        if (asyncCompletion.completePart1()) {
            span.finish();
        }
    }

    public void setUser(@Nullable String user) {
        this.user = user;
    }

    public void setError(Throwable t) {
        if (exception == null) {
            exception = t;
        }
    }

    private void endInternal() {
        threadContextHolder.set(null);
        if (asyncCompletion == null || asyncCompletion.completePart2()) {
            span.finish();
        }
    }
}
