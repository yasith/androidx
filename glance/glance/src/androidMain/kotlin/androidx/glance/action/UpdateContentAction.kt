/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.glance.action

import android.content.Context
import androidx.annotation.RestrictTo

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class UpdateContentAction(
    public val runnableClass: Class<out ActionRunnable>,
    public val parameters: ActionParameters
) : Action {
    companion object {

        public suspend fun run(context: Context, className: String, parameters: ActionParameters) {
            val workClass = Class.forName(className)

            if (!ActionRunnable::class.java.isAssignableFrom(workClass)) {
                error("Runnable class must implement ActionRunnable.")
            }

            val work = workClass.newInstance()
            (work as ActionRunnable).run(context, parameters)
        }
    }
}

/**
 * A runnable task executed in response to the user action, before the content is updated. The
 * implementing class must have a public zero argument constructor, this is used to instantiate
 * the class at runtime.
 */
public interface ActionRunnable {
    /**
     * Performs the work associated with this action. Called when the action is triggered.
     *
     * @param context the calling context
     * @param parameters the parameters associated with the action
     */
    suspend fun run(context: Context, parameters: ActionParameters = actionParametersOf())
}

/**
 * Creates an [Action] that executes a custom [ActionRunnable] and then updates the component
 * content.
 *
 * @param runnable the class of the runnable
 * @param parameters the parameters associated with the action
 */
public fun <T : ActionRunnable> actionUpdateContent(
    runnable: Class<T>,
    parameters: ActionParameters = actionParametersOf()
): Action = UpdateContentAction(runnable, parameters)

@Suppress("MissingNullability") /* Shouldn't need to specify @NonNull. b/199284086 */
/**
 * Creates an [Action] that executes a custom [ActionRunnable] and then updates the component
 * content.
 *
 * @param parameters the parameters associated with the action
 */
// TODO(b/201418282): Add the UI update path
public inline fun <reified T : ActionRunnable> actionUpdateContent(
    parameters: ActionParameters = actionParametersOf()
): Action = actionUpdateContent(T::class.java, parameters)
