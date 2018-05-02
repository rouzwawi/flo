/*-
 * -\-\-
 * flo runner
 * --
 * Copyright (C) 2016 Spotify AB
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

import static java.util.stream.Collectors.joining;

import com.spotify.flo.TaskInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Utilities for printing the task tree
 */
final class PrintUtils {

  private PrintUtils() {
  }

  static List<String> tree(TaskInfo taskInfo) {
    final List<String> lines = new ArrayList<>();

    lines.add(taskInfo.id().toString());
    popSubTree(taskInfo.inputs(), lines, new Stack<>());

    return lines;
  }

  private static void popSubTree(List<TaskInfo> inputs, List<String> list, Stack<Boolean> indents) {
    for (int i = 0; i < inputs.size(); i++) {
      final TaskInfo taskInfo = inputs.get(i);
      final String indent = indents.stream()
          .map(b -> b ? "   " : "│  ")
          .collect(joining());
      final String branch = (i < inputs.size() - 1) ? "├" : "└";
      final String prefix = indent + branch + "▸ ";
      final String refArrow = taskInfo.isReference()
          ? " ⤴"
          : "";

      list.add(prefix + taskInfo.id() + refArrow);

      indents.push(i == inputs.size() - 1);
      popSubTree(taskInfo.inputs(), list, indents);
      indents.pop();
    }
  }
}
