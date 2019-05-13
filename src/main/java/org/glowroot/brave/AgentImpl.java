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

import brave.Tracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext.Extractor;
import brave.propagation.TraceContextOrSamplingFlags;

import org.glowroot.brave.span.IncomingSpanImpl;
import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.spi.AgentSPI;
import org.glowroot.xyzzy.instrumentation.api.Getter;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.Span;
import org.glowroot.xyzzy.instrumentation.api.TimerName;

class AgentImpl implements AgentSPI {

    private final Tracing tracing;

    private final ThreadContextThreadLocal threadContextThreadLocal;

    AgentImpl(Tracing tracing, ThreadContextThreadLocal threadContextThreadLocal) {
        this.tracing = tracing;
        this.threadContextThreadLocal = threadContextThreadLocal;
    }

    // in addition to returning Span, this method needs to put the newly created thread context into
    // the threadContextHolder that is passed in
    @Override
    public <C> Span startIncomingSpan(String transactionType, String transactionName,
            Getter<C> getter, C carrier, MessageSupplier messageSupplier, TimerName timerName,
            ThreadContextThreadLocal.Holder threadContextHolder, int rootNestingGroupId,
            int rootSuppressionKeyId) {

        TraceContextOrSamplingFlags extracted = extract(getter, carrier);

        brave.Span span = extracted.context() != null
                ? tracing.tracer().joinSpan(extracted.context())
                : tracing.tracer().nextSpan(extracted);

        IncomingSpanImpl incomingSpan =
                new IncomingSpanImpl(span, messageSupplier, threadContextHolder);

        ThreadContextImpl threadContext = new ThreadContextImpl(tracing, threadContextThreadLocal,
                incomingSpan, rootNestingGroupId, rootSuppressionKeyId, null);
        threadContextHolder.set(threadContext);

        return incomingSpan;
    }

    private static <C> TraceContextOrSamplingFlags extract(Getter<C> getter, C carrier) {
        Extractor<C> extractor = Propagation.B3_STRING.extractor(new BraveGetter<C>(getter));
        return extractor.extract(carrier);
    }

    private static class BraveGetter<C> implements brave.propagation.Propagation.Getter<C, String> {

        private final Getter<C> getter;

        private BraveGetter(Getter<C> getter) {
            this.getter = getter;
        }

        @Override
        public String get(C carrier, String key) {
            return getter.get(carrier, key);
        }
    }
}
