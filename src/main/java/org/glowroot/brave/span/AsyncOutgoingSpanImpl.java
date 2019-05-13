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
import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.AsyncSpan;
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.Setter;
import org.glowroot.xyzzy.instrumentation.api.Timer;

public class AsyncOutgoingSpanImpl implements AsyncSpan {

    private final Span span;

    public AsyncOutgoingSpanImpl(Span span) {
        this.span = span;
    }

    @Override
    public void end() {
        span.finish();
    }

    @Override
    public void endWithLocationStackTrace(long threshold, TimeUnit unit) {
        span.finish();
    }

    @Override
    public void endWithError(Throwable t) {
        span.error(t);
        span.finish();
    }

    @Override
    public void endWithError(String message) {
        span.finish();
    }

    @Override
    public void endWithError(@Nullable String message, Throwable t) {
        span.error(t);
        span.finish();
    }

    @Override
    public void endWithInfo(Throwable t) {
        span.finish();
    }

    @Override
    public Timer extend() {
        return NopTransactionService.TIMER;
    }

    @Override
    public @Nullable Object getMessageSupplier() {
        return null;
    }

    @Override
    public void stopSyncTimer() {}

    @Override
    public Timer extendSyncTimer() {
        return NopTransactionService.TIMER;
    }

    @Override
    @Deprecated
    public <R> void propagateToResponse(R response, Setter<R> setter) {}

    @Override
    @Deprecated
    public <R> void extractFromResponse(R response, Getter<R> getter) {}
}
