/*
 * Copyright 2022-2026 kronos-orm
 * Licensed under the Apache License, Version 2.0.
 */
package com.kotlinorm.ide.coverage;

import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.security.ProtectionDomain;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import kotlinx.kover.jvmagent.KoverJvmAgentPremain;

/** Gives Kover frame computation the resources hidden by IDEA's compiler-plugin loader. */
public final class KoverResourceAgent {
    private static final Map<ClassLoader, WeakReference<ClassLoader>> RESOURCES = new WeakHashMap<>();

    private KoverResourceAgent() {}

    public static void premain(String arguments, Instrumentation instrumentation) throws Exception {
        Map<ClassFileTransformer, ClassFileTransformer> transformers = new IdentityHashMap<>();
        Instrumentation view = (Instrumentation) Proxy.newProxyInstance(
                Instrumentation.class.getClassLoader(), new Class<?>[] {Instrumentation.class},
                (proxy, method, values) -> {
                    if (method.getName().equals("addTransformer")) {
                        ClassFileTransformer original = (ClassFileTransformer) values[0];
                        ClassFileTransformer wrapped = new ClassFileTransformer() {
                            @Override
                            public byte[] transform(Module module, ClassLoader loader, String name,
                                    Class<?> redefined, ProtectionDomain domain, byte[] bytes)
                                    throws IllegalClassFormatException {
                                ClassLoader resources = name != null && name.startsWith("com/kotlinorm/compiler/")
                                        ? resources(loader) : loader;
                                return original.transform(module, resources, name, redefined, domain, bytes);
                            }
                        };
                        synchronized (transformers) {
                            transformers.put(original, wrapped);
                        }
                        values[0] = wrapped;
                    } else if (method.getName().equals("removeTransformer")) {
                        synchronized (transformers) {
                            values[0] = transformers.getOrDefault(values[0], (ClassFileTransformer) values[0]);
                        }
                    }
                    try {
                        return method.invoke(instrumentation, values);
                    } catch (InvocationTargetException failure) {
                        throw failure.getTargetException();
                    }
                });
        KoverJvmAgentPremain.premain(arguments, view);
    }

    private static ClassLoader resources(ClassLoader loader) {
        if (loader == null) return null;
        synchronized (RESOURCES) {
            WeakReference<ClassLoader> reference = RESOURCES.get(loader);
            ClassLoader cached = reference == null ? null : reference.get();
            if (cached != null) return cached;
            // Class loading still delegates to the unmodified IDE loader. Only resource
            // lookup gains its parent chain, which UrlClassLoader omits by default.
            ClassLoader view = new ClassLoader(loader) {
                @Override
                public InputStream getResourceAsStream(String name) {
                    for (ClassLoader current = getParent(); current != null; current = current.getParent()) {
                        InputStream stream = current.getResourceAsStream(name);
                        if (stream != null) return stream;
                    }
                    return ClassLoader.getSystemResourceAsStream(name);
                }
            };
            RESOURCES.put(loader, new WeakReference<>(view));
            return view;
        }
    }
}
