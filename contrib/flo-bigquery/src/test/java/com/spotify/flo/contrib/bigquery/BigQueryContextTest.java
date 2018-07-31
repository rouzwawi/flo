/*-
 * -\-\-
 * flo-bigquery
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.cloud.WaitForOption;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryError;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.DatasetInfo;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.JobStatus;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.common.base.Throwables;
import com.spotify.flo.Task;
import com.spotify.flo.context.FloRunner;
import com.spotify.flo.freezer.PersistingContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BigQueryContextTest {

  @Mock
  private BigQuery bigQuery;

  @Mock
  private Dataset dataset;

  @Mock
  private Job job;

  private static final String PROJECT = "project";
  private static final String LOCATION = "EU";
  private static final DatasetId DATASET_ID = DatasetId.of(PROJECT, "dataset");
  private static final TableId TABLE_ID = TableId.of(PROJECT, DATASET_ID.getDataset(), "table");

  private FloBigQueryClient floBigQueryClient;

  @Before
  public void setup() {
    when(dataset.getLocation()).thenReturn(LOCATION);
    when(bigQuery.create(any(DatasetInfo.class))).thenReturn(dataset);
    when(bigQuery.create(any(JobInfo.class))).thenReturn(mock(Job.class));

    floBigQueryClient = spy(new DefaultBigQueryClient(bigQuery));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotCreateDataset() {
    final BigQueryContext bigQueryContext = BigQueryContext.create(() -> floBigQueryClient, TABLE_ID);

    bigQueryContext.provide(null);
  }

  @Test
  public void shouldCreateStagingDatasetIfDoesNotExist() {
    when(bigQuery.getDataset(DATASET_ID)).thenReturn(dataset);

    final BigQueryContext bigQueryContext = BigQueryContext.create(() -> floBigQueryClient, TABLE_ID);

    bigQueryContext.provide(null);

    verify(bigQuery).create(any(DatasetInfo.class));
  }

  @Test
  public void shouldProvideStagingTableId() {
    when(bigQuery.getDataset(any(DatasetId.class))).thenReturn(dataset);

    final BigQueryContext bigQueryContext = BigQueryContext.create(() -> floBigQueryClient, TABLE_ID);

    final StagingTableId stagingTableId = bigQueryContext.provide(null);

    assertThat(stagingTableId.tableId(), is(not(TABLE_ID)));
  }

  @Test
  public void shouldReturnTableIdOnJobSuccess() throws InterruptedException, TimeoutException {
    when(bigQuery.getDataset(any(DatasetId.class))).thenReturn(dataset);
    when(bigQuery.create(any(JobInfo.class))).thenReturn(job);
    when(job.waitFor(any(WaitForOption.class))).thenReturn(job);
    when(job.getStatus()).thenReturn(mock(JobStatus.class));

    final BigQueryContext bigQueryContext = BigQueryContext.create(() -> floBigQueryClient, TABLE_ID);

    final StagingTableId stagingTableId = bigQueryContext.provide(null);

    final TableId tableId = stagingTableId.publish();

    assertThat(tableId, is(TABLE_ID));
  }

  @Test
  public void shouldReturnTableIdWhenExists() {
    when(bigQuery.getDataset(DATASET_ID)).thenReturn(mock(Dataset.class));
    when(bigQuery.getTable(TABLE_ID)).thenReturn(mock(Table.class));

    final BigQueryContext bigQueryContext = BigQueryContext.create(() -> floBigQueryClient, TABLE_ID);

    final TableId tableId = bigQueryContext.lookup(null).get();

    assertThat(tableId, is(TABLE_ID));
  }

  @Test(expected = RuntimeException.class)
  public void shouldFailWhenJobTerminatesWithError() throws InterruptedException, TimeoutException {
    when(bigQuery.getDataset(DATASET_ID)).thenReturn(mock(Dataset.class));

    when(bigQuery.create(any(JobInfo.class))).thenReturn(job);
    when(job.waitFor(any(WaitForOption.class))).thenReturn(job);
    when(job.getStatus()).thenReturn(mock(JobStatus.class));
    when(job.getStatus().getError()).thenReturn(new BigQueryError("", "", "job error"));

    BigQueryContext.create(() -> floBigQueryClient, TABLE_ID).provide(null).publish();
  }

  @Test(expected = RuntimeException.class)
  public void shouldFailWhenJobDisappears() throws InterruptedException, TimeoutException {
    when(bigQuery.getDataset(DATASET_ID)).thenReturn(mock(Dataset.class));

    when(bigQuery.create(any(JobInfo.class))).thenReturn(job);
    when(job.waitFor(any(WaitForOption.class))).thenReturn(null);

    BigQueryContext.create(() -> floBigQueryClient, TABLE_ID).provide(null).publish();
  }

  @Test(expected = BigQueryException.class)
  public void shouldFailWhenJobTerminatesExceptionally()
      throws InterruptedException, TimeoutException {
    when(bigQuery.getDataset(DATASET_ID)).thenReturn(mock(Dataset.class));

    when(bigQuery.create(any(JobInfo.class))).thenReturn(job);
    doThrow(new BigQueryException(mock(IOException.class))).when(job)
        .waitFor(any(WaitForOption.class));

    BigQueryContext.create(() -> floBigQueryClient, TABLE_ID).provide(null).publish();
  }

  @Test
  public void shouldBeSerializable() {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final BigQueryContext context = BigQueryContext.create("foo", "bar", "baz");
    PersistingContext.serialize(context, baos);
    final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    final BigQueryContext deserializedContext = PersistingContext.deserialize(bais);
    assertThat(deserializedContext, is(notNullValue()));
  }

  @Test
  public void shouldBeRunnable() throws Exception {
    final String nonExistentProject = UUID.randomUUID().toString();
    final Task<TableId> task = Task.named("task").ofType(TableId.class)
        .context(BigQueryContext.create(nonExistentProject, "foo", "bar"))
        .process(StagingTableId::tableId);

    final Future<TableId> future = FloRunner.runTask(task).future();
    try {
      future.get(30, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
      final Throwable rootCause = Throwables.getRootCause(e);
      if (rootCause instanceof GoogleJsonResponseException) {
        // Seems we managed to make a request, so the lookup context was successfully invoked. We're done here.
      } else if (rootCause instanceof IllegalArgumentException
          && rootCause.getMessage().startsWith("A project ID is required")) {
        // Seems we got as far as to instantiate the BigQuery client. We're done here.
      } else if (rootCause instanceof IllegalArgumentException &&
          rootCause.getMessage().startsWith("Dataset does not exist.")) {
        // Seems we managed to make a request, so the lookup context was successfully invoked. We're done here.
      } else if (rootCause instanceof BigQueryException &&
          rootCause.getMessage().equals("The project " + nonExistentProject + " has not enabled BigQuery.")) {
        // Seems we managed to make a request, so the lookup context was successfully invoked. We're done here.
      } else {
        // Not sure what error we got here, might be a serialization problem. Be conservative and fail.
        throw new AssertionError("Unknown error, might be serialization problem that needs fixing?", e);
      }
    }
  }
}
