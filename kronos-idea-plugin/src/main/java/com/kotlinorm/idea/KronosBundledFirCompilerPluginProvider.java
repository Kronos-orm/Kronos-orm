/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.idea;

import com.intellij.openapi.project.Project;
import com.kotlinorm.compiler.fir.KronosProjectionIdeBridge;
import java.nio.file.Path;
import org.jetbrains.kotlin.idea.fir.extensions.KotlinBundledFirCompilerPluginProvider;

public final class KronosBundledFirCompilerPluginProvider
        implements KotlinBundledFirCompilerPluginProvider {
    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public Path provideBundledPluginJar(Project project, Path userSuppliedPluginJar) {
        return KronosIdeaSafe.INSTANCE.guard(
                "bundled FIR compiler plugin lookup",
                (Path) null,
                () -> {
                    KronosProjectionIdeBridge.INSTANCE.markIdeActive();
                    return KronosIdePluginPaths.INSTANCE.compilerPluginJar();
                }
        );
    }
}
