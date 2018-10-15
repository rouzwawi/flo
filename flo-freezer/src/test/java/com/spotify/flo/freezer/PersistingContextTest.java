/*-
 * -\-\-
 * Flo Freezer
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

package com.spotify.flo.freezer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import org.apache.beam.sdk.options.ApplicationNameOptions;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PersistingContextTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void exceptionSerialization() throws Exception {
    final RuntimeException bar = new RuntimeException("bar");
    bar.addSuppressed(new IOException("baz", new InterruptedException("quux")));
    final Exception exception1 = new Exception("foo", bar);
    exception1.addSuppressed(new FoobarException());
    final Exception exception = exception1;

    final Path exceptionFile = temporaryFolder.getRoot().toPath().resolve("exception");
    PersistingContext.serialize(exception, exceptionFile);

    final Exception deserialized = PersistingContext.deserialize(exceptionFile);

    // Verify that stack trace can be accessed and printed and seems to be correct

    deserialized.printStackTrace();

    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    deserialized.printStackTrace(pw);
    pw.flush();
    final String stacktrace = sw.toString();
    assertThat(stacktrace, containsString("java.lang.Exception: foo"));
    assertThat(stacktrace, containsString("at com.spotify.flo.freezer.PersistingContextTest.exceptionSerialization"));
    assertThat(stacktrace, containsString("Suppressed: com.spotify.flo.freezer.PersistingContextTest$FoobarException"));
    assertThat(stacktrace, containsString("Caused by: java.lang.RuntimeException: bar"));
    assertThat(stacktrace, containsString("Suppressed: java.io.IOException: baz"));
    assertThat(stacktrace, containsString("Caused by: java.lang.InterruptedException: quux"));

    assertThat(deserialized.getStackTrace().length, is(not(0)));
    assertThat(deserialized.getStackTrace()[0].getClassName(), is("com.spotify.flo.freezer.PersistingContextTest"));
    assertThat(deserialized.getStackTrace()[0].getMethodName(), is("exceptionSerialization"));
  }

  private static class FoobarException extends Exception {

  }

  @Test
  public void shouldBeAbleToSerializePipelineOptions() {
    final String appName = "foobar-test";
    final String jobName = "foo";
    final long optionsId = 17;

    // Original
    final PipelineOptions opts = PipelineOptionsFactory.create();
    opts.as(ApplicationNameOptions.class).setAppName(appName);
    opts.setJobName(jobName);
    opts.setOptionsId(optionsId);

    // SerDe round trip
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PersistingContext.serialize(opts, baos);
    final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    PipelineOptions deserialized = PersistingContext.deserialize(bais);

    // Verify that the options survived SerDe
    assertThat(deserialized.getJobName(), is(jobName));
    assertThat(deserialized.getOptionsId(), is(optionsId));
    assertThat(deserialized.as(ApplicationNameOptions.class).getAppName(), is(appName));
  }
}