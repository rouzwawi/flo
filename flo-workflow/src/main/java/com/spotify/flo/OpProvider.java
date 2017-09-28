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

/**
 * Provider interface for operation objects that will be injected into tasks.
 *
 * <p>An operator is an object that is aware of the lifecycle of a task and can perform
 * operations before and after the task evaluates. A common use case for operators is to be able
 * to integrate 3rd party libraries into Flo in a way that makes them easily accessible to tasks.
 */
public interface OpProvider<T> {

  /**
   * Creates a new operator instance of type {@link T}. The given {@link EvalContext} will be
   * the context which was used to evaluate the task which this operator is being injected into.
   * The task itself can be accessed through {@link EvalContext#currentTask()}.
   *
   * @param evalContext The evaluation context in which the current task is being evaluated
   * @return An instance of the provided operator type
   */
  T provide(EvalContext evalContext);

  /**
   * Will be called just before a task that is using this operator starts evaluating.
   *
   * @param task The task being evaluated
   */
  default void preRun(Task<?> task) {
  }

  /**
   * Will be called just after a task that is using this operator has successfully evaluated.
   *
   * @param task The task that evaluated
   * @param z    The return value of the evaluated task
   */
  default void onSuccess(Task<?> task, Object z) {
  }

  /**
   * Will be called just after a task that is using this operator has failed evaluating.
   *
   * @param task      The task that evaluated
   * @param throwable The throwable that was thrown, causing the failure
   */
  default void onFail(Task<?> task, Throwable throwable) {
  }
}
