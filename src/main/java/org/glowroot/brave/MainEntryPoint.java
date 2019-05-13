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

import java.io.File;
import java.lang.instrument.Instrumentation;

import brave.Tracing;
import brave.propagation.ThreadLocalCurrentTraceContext;
import org.slf4j.Logger;
import zipkin2.reporter.Reporter;

import org.glowroot.xyzzy.engine.bytecode.api.ThreadContextThreadLocal;
import org.glowroot.xyzzy.engine.init.EngineModule;
import org.glowroot.xyzzy.engine.init.MainEntryPointUtil;

public class MainEntryPoint {

    private MainEntryPoint() {}

    public static void premain(Instrumentation instrumentation, File agentJarFile) {
        // DO NOT USE ANY GUAVA CLASSES before initLogging() because they trigger loading of jul
        // (and thus org.glowroot.xyzzy.engine.jul.Logger and thus glowroot's shaded slf4j)
        Logger startupLogger;
        try {
            startupLogger = MainEntryPointUtil.initLogging("org.glowroot.zipkin", instrumentation);
        } catch (Throwable t) {
            // log error but don't re-throw which would prevent monitored app from starting
            // also, don't use logger since it failed to initialize
            System.err.println("Agent failed to start: " + t.getMessage());
            t.printStackTrace();
            return;
        }
        try {
            start(instrumentation, agentJarFile);
        } catch (Throwable t) {
            // log error but don't re-throw which would prevent monitored app from starting
            startupLogger.error("Agent failed to start: {}", t.getMessage(), t);
        }
    }

    private static void start(Instrumentation instrumentation, File agentJarFile) throws Exception {

        // FIXME simple way to avoid conflict when multiple java processes being monitored
        File tmpDir = new File(agentJarFile.getParentFile(), "tmp");

        Tracing tracing = Tracing.newBuilder()
                .currentTraceContext(ThreadLocalCurrentTraceContext.create())
                .localServiceName("my-service-name")
                .spanReporter(Reporter.CONSOLE)
                .build();

        ThreadContextThreadLocal threadContextThreadLocal = new ThreadContextThreadLocal();

        AgentImpl agent = new AgentImpl(tracing, threadContextThreadLocal);

        EngineModule.createWithSomeDefaults(instrumentation, tmpDir, threadContextThreadLocal,
                agent, agentJarFile);
    }
}
