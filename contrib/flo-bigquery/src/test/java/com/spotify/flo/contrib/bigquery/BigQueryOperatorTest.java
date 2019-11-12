/*-
 * -\-\-
 * Flo BigQuery
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

package com.spotify.flo.contrib.bigquery;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.cloud.bigquery.CopyJobConfiguration;
import com.google.cloud.bigquery.ExtractJobConfiguration;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.LoadJobConfiguration;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableId;
import com.spotify.flo.FloTesting;
import com.spotify.flo.Task;
import com.spotify.flo.TestScope;
import com.spotify.flo.context.FloRunner;
import org.junit.Test;

public class BigQueryOperatorTest {

  @Test
  public void shouldRunQueryTestMode() throws Exception {
    final Task<String> task = Task.named("task")
        .ofType(String.class)
        .operator(BigQueryOperator.create())
        .process(bq -> bq.query("SELECT foo FROM input")
            .success(result -> "success!"));

    try (TestScope scope = FloTesting.scope()) {
      final String result = FloRunner.runTask(task).future()
          .get(30, SECONDS);
      assertThat(result, is("success!"));
    }
  }

  @Test
  public void shouldRunQueryJobInTestMode() throws Exception {
    final TableId table = TableId.of("foo", "bar", "baz");

    final Task<TableId> task = Task.named("task")
        .ofType(TableId.class)
        .output(BigQueryOutput.create(table))
        .operator(BigQueryOperator.create())
        .process((stagingTable, bq) -> bq.job(
            JobInfo.of(QueryJobConfiguration.newBuilder("SELECT foo FROM input")
                .setDestinationTable(stagingTable.tableId())
                .build()))
            .success(response -> stagingTable.publish()));

    try (TestScope scope = FloTesting.scope()) {

      final TableId result = FloRunner.runTask(task).future()
          .get(30, SECONDS);

      assertThat(result, is(table));
      assertThat(BigQueryMocking.mock().tablePublished(table), is(true));
      assertThat(BigQueryMocking.mock().tableExists(table), is(true));
    }
  }

  @Test
  public void shouldRunCopyJobInTestMode() throws Exception {
    final TableId srcTable = TableId.of("foo", "bar", "src");
    final TableId dstTable = TableId.of("foo", "bar", "dst");

    final Task<TableId> task = Task.named("task")
        .ofType(TableId.class)
        .operator(BigQueryOperator.create())
        .process(bq -> bq.job(
            JobInfo.of(CopyJobConfiguration.of(dstTable, srcTable)))
            .success(response -> dstTable));

    try (TestScope scope = FloTesting.scope()) {

      final TableId result = FloRunner.runTask(task).future()
          .get(30, SECONDS);

      assertThat(result, is(dstTable));
    }
  }

  @Test
  public void shouldRunLoadJobInTestMode() throws Exception {
    final TableId dstTable = TableId.of("foo", "bar", "baz");
    final String srcUri = "gs://foo/bar";

    final Task<TableId> task = Task.named("task")
        .ofType(TableId.class)
        .operator(BigQueryOperator.create())
        .process(bq -> bq.job(
            JobInfo.of(LoadJobConfiguration.of(dstTable, srcUri)))
            .success(response -> dstTable));

    try (TestScope scope = FloTesting.scope()) {

      final TableId result = FloRunner.runTask(task).future()
          .get(30, SECONDS);

      assertThat(result, is(dstTable));
    }
  }

  @Test
  public void shouldRunLoadJobInTestModeWithSuccess() throws Exception {
    final TableId dstTable = TableId.of("foo", "bar", "baz");
    final String srcUri = "gs://foo/bar";

    final Task<TableId> task = Task.named("task")
        .ofType(TableId.class)
        .operator(BigQueryOperator.create())
        .process(bq -> bq.job(
            JobInfo.of(LoadJobConfiguration.of(dstTable, srcUri)), response -> dstTable));

    try (TestScope scope = FloTesting.scope()) {

      final TableId result = FloRunner.runTask(task).future()
          .get(30, SECONDS);

      assertThat(result, is(dstTable));
    }
  }

  @Test
  public void shouldRunExtractJobInTestMode() throws Exception {
    final TableId srcTable = TableId.of("foo", "bar", "baz");

    final String destinationUri = "gs://foo/bar";

    final Task<String> task = Task.named("task")
        .ofType(String.class)
        .operator(BigQueryOperator.create())
        .process(bq -> bq.job(
            JobInfo.of(ExtractJobConfiguration.of(srcTable, destinationUri)))
            .success(response -> destinationUri));

    try (TestScope scope = FloTesting.scope()) {

      final String result = FloRunner.runTask(task).future()
          .get(30, SECONDS);

      assertThat(result, is(destinationUri));
    }
  }
}
