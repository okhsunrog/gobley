/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gobley.gradle.android

import gobley.gradle.InternalGobleyGradleApi
import gobley.gradle.tasks.InjectJniLibsTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File

typealias OnVariantAction = (
    agpVariantName: String,
    cargoVariantName: String,
    onMainTask: (TaskProvider<InjectJniLibsTask>) -> Unit,
    onTestTask: ((TaskProvider<InjectJniLibsTask>) -> Unit)?
) -> Unit

@InternalGobleyGradleApi
interface GobleyAndroidExtensionDelegate {
    val androidSdkRoot: File
    val androidMinSdk: Int
    val androidNdkRoot: File?
    val androidNdkVersion: String?
    val abiFilters: Set<String>

    /**
     * Wires generated Kotlin source code into the Android build.
     * Safely attaches the mapped directory to both the main application
     * and its isolated test components (Unit/UI tests) without exposing AGP classes.
     */
    fun <T : Task> addGeneratedBindingsDirectory(
        project: Project,
        taskProvider: TaskProvider<T>,
        directoryMapping: (T) -> DirectoryProperty
    )

    /**
     * Registers an AGP `onVariants` callback that wires a generated source
     * directory into every variant. Unlike [addGeneratedBindingsDirectory] this
     * is safe to call during the plugin's configuration phase: AGP 9 forbids
     * registering `onVariants` from `afterEvaluate`. The producing task is
     * resolved lazily by [taskName] inside the callback, so it only needs to
     * exist by the time AGP fires the variant callbacks (after `afterEvaluate`).
     * [shouldAdd] is evaluated inside the callback so callers can defer the
     * decision until the build is fully configured.
     */
    fun addGeneratedJavaSourcesForEachVariant(
        project: Project,
        taskName: String,
        shouldAdd: () -> Boolean,
        outputDir: (Task) -> DirectoryProperty,
    )

    /**
     * Provides a safe hook into Android's build variants (e.g., debug, release).
     * Used primarily to map and inject compiled native Rust binaries (JNI libs)
     * into the corresponding main and test APKs.
     */
    fun onVariants(
        project: Project,
        action: OnVariantAction,
    )

    fun addProguardFiles(
        project: Project,
        proguardFileProvider: Provider<RegularFile>,
        generationTask: TaskProvider<*>,
    )
}
