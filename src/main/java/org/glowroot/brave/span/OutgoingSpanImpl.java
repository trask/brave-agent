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

import brave.ScopedSpan;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.glowroot.xyzzy.engine.impl.NopTransactionService;
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.Setter;
import org.glowroot.xyzzy.instrumentation.api.Span;
import org.glowroot.xyzzy.instrumentation.api.Timer;

public class OutgoingSpanImpl implements Span {

    private final ScopedSpan scopedSpan;

    public OutgoingSpanImpl(ScopedSpan scopedSpan) {
        this.scopedSpan = scopedSpan;
    }

    @Override
    public void end() {
        scopedSpan.finish();
    }

    @Override
    public void endWithLocationStackTrace(long threshold, TimeUnit unit) {
        scopedSpan.finish();
    }

    @Override
    public void endWithError(Throwable t) {
        scopedSpan.error(t);
        scopedSpan.finish();
    }

    @Override
    public void endWithError(String message) {
        scopedSpan.finish();
    }

    @Override
    public void endWithError(@Nullable String message, Throwable t) {
        scopedSpan.error(t);
        scopedSpan.finish();
    }

    @Override
    public void endWithInfo(Throwable t) {
        scopedSpan.finish();
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
    @Deprecated
    public <R> void propagateToResponse(R response, Setter<R> setter) {}

    @Override
    @Deprecated
    public <R> void extractFromResponse(R response, Getter<R> getter) {}
}
