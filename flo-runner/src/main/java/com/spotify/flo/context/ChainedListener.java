/*-
 * -\-\-
 * flo runner
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

package com.spotify.flo.context;

import static java.util.Objects.requireNonNull;

import com.spotify.flo.Task;
import com.spotify.flo.TaskId;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link InstrumentedContext.Listener} that chains calls between two other listener instances.
 */
class ChainedListener implements InstrumentedContext.Listener {

  private final InstrumentedContext.Listener first;
  private final InstrumentedContext.Listener second;
  private final Logging logging;

  ChainedListener(InstrumentedContext.Listener first,
                  InstrumentedContext.Listener second,
                  Logging logging) {
    this.first = requireNonNull(first);
    this.second = requireNonNull(second);
    this.logging = requireNonNull(logging);
  }

  @Override
  public void task(Task<?> task) {
    guardedCall(() -> first.task(task));
    guardedCall(() -> second.task(task));
  }

  @Override
  public void status(TaskId taskId, Phase phase) {
    guardedCall(() -> first.status(taskId, phase));
    guardedCall(() -> second.status(taskId, phase));
  }

  private void guardedCall(Runnable call) {
    try {
      call.run();
    } catch (Throwable t) {
      logging.exception(t);
    }
  }

  @Override
  public void close() throws IOException {
    final List<IOException> exceptions = Stream.of(first, second)
        .map(this::close)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());

    // TODO: Throw a composition of two exceptions when both first and second throw exceptions
    if (!exceptions.isEmpty()) {
      throw exceptions.get(0);
    }
  }

  private Optional<IOException> close(InstrumentedContext.Listener listener) {
    try {
      listener.close();
    } catch (IOException e) {
      logging.exception(e);
      return Optional.of(e);
    }
    return Optional.empty();
  }
}
