/*-
 * -\-\-
 * Flo Workflow Definition
 * --
 * Copyright (C) 2016 - 2017 Spotify AB
 * --
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
 * -/-/-
 */

package com.spotify.flo;

import java.util.Optional;

/**
 * This is to be extended when building a {@link TaskContext} that is intended to work only with a
 * task whose returned value is of type {@link S}.
 */
public abstract class TaskContextStrict<T, S> implements TaskContext<T> {

  @Override
  final public void onSuccess(Task<?> task, Object z) {
   onSuccessStrict(task, (S) z);
  }

  /**
   * Will be called just after a task that is using this task context has successfully evaluated.
   *
   * @param task The task that evaluated
   * @param z    The return value of the evaluated task
   */
  public void onSuccessStrict(Task<?> task, S z) {
  }

  /**
   * Perform a lookup of the value this task would have produced if it ran. Override to be able
   * to short-circuit a task with a previously calculated value.
   *
   * @param task a task with a {@link TaskContextStrict} to lookup a value for
   * @return the value for this {@link TaskContextStrict} (e.g. from a previous run)
   */
  public Optional<S> lookup(Task<S> task) {
    return Optional.empty();
  }
}
