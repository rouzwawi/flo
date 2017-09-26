/*-
 * -\-\-
 * Flo Command Line Bindings
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

package com.spotify.flo.cli;

import static java.lang.System.out;
import static java.util.Arrays.asList;

import com.spotify.flo.Task;
import com.spotify.flo.TaskConstructor;
import com.spotify.flo.TaskContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

@Deprecated
public final class Cli {

  private final List<TaskConstructor<?>> factories;

  private Cli(List<TaskConstructor<?>> factories) {
    this.factories = factories;
  }

  public static Cli forFactories(TaskConstructor<?>... factories) {
    return new Cli(asList(factories));
  }

  public void run(String... args) throws IOException {
    final OptionParser parser = new OptionParser();
    final OptionSpec<Void> h = parser.acceptsAll(asList("h", "help"), "Print usage info").forHelp();

    final NonOptionArgumentSpec<String> nonOptions = parser.nonOptions();
    parser.allowsUnrecognizedOptions();

    final OptionSet parse = parser.parse(args);
    if (parse.nonOptionArguments().size() < 1 && parse.has(h)) {
      out.println("Flo 0.0.1");
      usage(parser);
    }
    if (parse.nonOptionArguments().size() < 1) {
      out.println("Too few arguments");
      usage(parser);
    }

    final String command = parse.valuesOf(nonOptions).get(0);
    switch (command) {
      case "list":
        list();
        break;

      case "create":
        final List<String> adjustedArgs = new ArrayList<>();
        adjustedArgs.addAll(parse.valuesOf(nonOptions));
        if (parse.has(h)) {
          adjustedArgs.add("--help");
        }
        create(adjustedArgs);
        break;

      default:
        out.println("'" + command + "' is not a command");
        usage(parser);
    }
  }

  private void list() {
    out.println("available tasks:\n");
    for (TaskConstructor<?> factory : factories) {
      out.println(factory.name());
    }
  }

  private void create(List<String> nonOptions) throws IOException {
    final String name = nonOptions.get(1);
    final List<String> subList = nonOptions.subList(2, nonOptions.size());
    final String[] args = subList.toArray(new String[subList.size()]);

    out.println("name = " + name);
    out.println("args = " + subList);
    out.println("creating tasks:\n");
    for (TaskConstructor<?> factory : factories) {
      if (factory.name().equalsIgnoreCase(name)) {
//        final OptionParser parser = factory.parser();
//        if (parser.parse(args).has("h")) {
//          parser.printHelpOn(out);
//          return;
//        }

        final TaskContext context = TaskContext.inmem();
        final Task<?> createdTask = factory.create(args);
        context.evaluate(createdTask).consume((value) -> {
          out.println("task.id() = " + createdTask.id());
          out.println("task value = " + value);
        });
        return;
      }
    }
  }

  private static void usage(OptionParser parser) throws IOException {
    out.println("  usage: flo <command> [<options>]\n");
    out.println("Commands:\n  list\n");
    parser.printHelpOn(out);
    System.exit(1);
  }
}
