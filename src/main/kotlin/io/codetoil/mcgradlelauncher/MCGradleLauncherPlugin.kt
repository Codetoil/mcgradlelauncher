/*
 * Copyright 2024 Codetoil
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
package io.codetoil.mcgradlelauncher

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.multimc.Launcher
import org.multimc.ParamBucket
import org.multimc.onesix.OneSixLauncher

abstract class LaunchMCTask : DefaultTask() {
    @get:Input
    abstract val params: Property<ParamBucket>

    @get:Input
    abstract val acceptableOutput: Property<Number>

    @TaskAction
    fun action() {
        val launcher: Launcher = OneSixLauncher()
        if (launcher.launch(params.get()) != acceptableOutput.get()) {
            throw GradleException("Game Exited with value other than 0")
        }
    }
}

class MCGradleLauncherPlugin : Plugin<Project> {
    override fun apply(project: Project) {

    }
}