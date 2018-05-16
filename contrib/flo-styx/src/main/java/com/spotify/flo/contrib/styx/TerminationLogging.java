/*-
 * -\-\-
 * flo-styx
 * --
 * Copyright (C) 2016 - 2018 Spotify AB
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

package com.spotify.flo.contrib.styx;

import com.spotify.flo.context.TerminationHook;
import com.typesafe.config.Config;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminationLogging implements TerminationHook {

  private static final Logger LOG = LoggerFactory.getLogger(TerminationLogging.class);

  private static final String STYX_TERMINATION_LOG = "styx.termination.log";
  private static final String STYX_COMPONENT_ID = "styx.component.id";
  private static final String STYX_WORKFLOW_ID = "styx.workflow.id";
  private static final String STYX_PARAMETER = "styx.parameter";
  private static final String STYX_EXECUTION_ID = "styx.execution.id";

  private final Config config;

  TerminationLogging(Config config) {
    this.config = Objects.requireNonNull(config);
  }

  @Override
  public void accept(Integer exitCode) {
    final String content = String.format("{\"component_id\": \"%s\","
                                         + "\"workflow_id\": \"%s\","
                                         + "\"parameter\": \"%s\","
                                         + "\"execution_id\": \"%s\","
                                         + "\"event\": \"exited\","
                                         + "\"exit_code\": %d}",
        config.getString(STYX_COMPONENT_ID),
        config.getString(STYX_WORKFLOW_ID),
        config.getString(STYX_PARAMETER),
        config.getString(STYX_EXECUTION_ID),
        exitCode);
    final Path path = Paths.get(config.getString(STYX_TERMINATION_LOG));

    try {
      Files.write(path, content.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      LOG.error("Could not write termination log to {}.", path, e);
      throw new RuntimeException(e);
    }
  }
}
