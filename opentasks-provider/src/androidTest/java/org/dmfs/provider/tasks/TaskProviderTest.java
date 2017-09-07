/*
 * Copyright 2017 dmfs GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dmfs.provider.tasks;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.dmfs.android.contentpal.OperationsBatch;
import org.dmfs.android.contentpal.RowSnapshot;
import org.dmfs.android.contentpal.Table;
import org.dmfs.android.contentpal.batches.MultiBatch;
import org.dmfs.android.contentpal.operations.Put;
import org.dmfs.android.contentpal.predicates.AllOf;
import org.dmfs.android.contentpal.predicates.EqArg;
import org.dmfs.android.contentpal.predicates.IdEq;
import org.dmfs.android.contentpal.rowdata.Composite;
import org.dmfs.android.contentpal.rowdata.EmptyRowData;
import org.dmfs.android.contentpal.rowsnapshots.VirtualRowSnapshot;
import org.dmfs.android.contentpal.tables.AccountScoped;
import org.dmfs.android.contentpal.tables.BaseTable;
import org.dmfs.android.contentpal.tables.Synced;
import org.dmfs.android.contentpal.testing.RowExistsAfter;
import org.dmfs.android.contentpal.testing.RowInserted;
import org.dmfs.android.contentpal.testing.SingletonRow;
import org.dmfs.opentaskspal.predicates.ListIdEq;
import org.dmfs.opentaskspal.tables.InstanceTable;
import org.dmfs.opentaskspal.tables.ListScoped;
import org.dmfs.opentaskspal.tables.TaskListsTable;
import org.dmfs.opentaskspal.tables.TasksTable;
import org.dmfs.opentaskspal.tasklists.NameData;
import org.dmfs.opentaskspal.tasks.DueData;
import org.dmfs.opentaskspal.tasks.DurationData;
import org.dmfs.opentaskspal.tasks.StartData;
import org.dmfs.opentaskspal.tasks.TimeZoneData;
import org.dmfs.opentaskspal.tasks.TitleData;
import org.dmfs.rfc5545.Duration;
import org.dmfs.tasks.contract.TaskContract;
import org.dmfs.tasks.contract.TaskContract.Instances;
import org.dmfs.tasks.contract.TaskContract.TaskLists;
import org.dmfs.tasks.contract.TaskContract.Tasks;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.dmfs.android.contentpal.testing.ContentChangeCheck.resultsIn;
import static org.junit.Assert.assertThat;


/**
 * Tests for {@link TaskProvider}.
 *
 * @author Yannic Ahrens
 * @author Gabor Keszthelyi
 */
@RunWith(AndroidJUnit4.class)
public class TaskProviderTest
{
    private static final String ACC_NAME = TaskContract.LOCAL_ACCOUNT_NAME;
    private static final String ACC_TYPE = TaskContract.LOCAL_ACCOUNT_TYPE;

    private ContentResolver mResolver;
    private String mAuthority;
    private Context mContext;
    private ContentProviderClient mClient;


    @Before
    public void setUp() throws Exception
    {
        mContext = InstrumentationRegistry.getTargetContext();
        mResolver = mContext.getContentResolver();
        mAuthority = AuthorityUtil.taskAuthority(mContext);
        mClient = mContext.getContentResolver().acquireContentProviderClient(mAuthority);
    }


    @After
    public void tearDown() throws Exception
    {
        /*
        TODO When Test Orchestration is available, there will be no need for clear up, every test will run in separate instrumentation
        https://android-developers.googleblog.com/2017/07/android-testing-support-library-10-is.html
        https://developer.android.com/training/testing/junit-runner.html#using-android-test-orchestrator
        */

        // Delete the entries from Tasks and TaskLists tables:
        // TODO Reveiw this createSyncQuery usage here, do we have better support for this now in taskspal?
        mResolver.delete(createSyncQuery(Tasks.getContentUri(mAuthority).buildUpon(), true), null, null);
        mResolver.delete(createSyncQuery(TaskLists.getContentUri(mAuthority).buildUpon(), true), null, null);

        mClient.release();
    }


    @Test
    public void testSingleInsert() throws Exception
    {
        Table<TaskLists> taskListsTable = createTaskListsTable();
        RowSnapshot<TaskLists> taskList = new VirtualRowSnapshot<>(taskListsTable);
        RowSnapshot<Tasks> task = new VirtualRowSnapshot<>(new ListScoped(taskList, new TasksTable(mAuthority)));

        OperationsBatch batch = new MultiBatch(
                new Put<>(taskList, new NameData("list 1")),
                new Put<>(task, new TitleData("task title 1"))
        );

        assertThat(batch, resultsIn(mClient,

                new RowInserted(new TaskListsTable(mAuthority),
                        new AllOf(
                                new EqArg(TaskLists.LIST_NAME, "list 1"),
                                new EqArg(TaskLists.ACCOUNT_NAME, ACC_NAME),
                                new EqArg(TaskLists.ACCOUNT_TYPE, ACC_TYPE)
                        )),

                new RowInserted(new TasksTable(mAuthority), Tasks.TITLE, "task title 1"),

                new RowExistsAfter(
                        new BaseTable<>(Instances.getContentUri(mAuthority)),
                        new IdEq<>(
                                TaskContract.InstanceColumns.TASK_ID,
                                new SingletonRow<>(mClient, new TasksTable(mAuthority), Tasks.TITLE, "task title 1"),
                                Tasks._ID))
        ));
    }


    /**
     * Creates three new Tasks. One of them will refer to a different TasksList.
     */
    @Test
    public void testMultipleInserts()
    {
        Table<TaskLists> taskListsTable = createTaskListsTable();
        RowSnapshot<TaskLists> taskList1 = new VirtualRowSnapshot<>(taskListsTable);
        RowSnapshot<TaskLists> taskList2 = new VirtualRowSnapshot<>(taskListsTable);
        RowSnapshot<Tasks> task1 = new VirtualRowSnapshot<>(new ListScoped(taskList1, new TasksTable(mAuthority)));
        RowSnapshot<Tasks> task2 = new VirtualRowSnapshot<>(new ListScoped(taskList1, new TasksTable(mAuthority)));
        RowSnapshot<Tasks> task3 = new VirtualRowSnapshot<>(new ListScoped(taskList2, new TasksTable(mAuthority)));

        OperationsBatch batch = new MultiBatch(
                new Put<>(taskList1, new NameData("list1")),
                new Put<>(taskList2, new NameData("list2")),
                new Put<>(task1, new TitleData("task1")),
                new Put<>(task2, new TitleData("task2")),
                new Put<>(task3, new TitleData("task3"))
        );

        assertThat(batch, resultsIn(mClient,

                new RowInserted(new TaskListsTable(mAuthority),
                        new AllOf(
                                new EqArg(TaskLists.LIST_NAME, "list1"),
                                new EqArg(TaskLists.ACCOUNT_NAME, ACC_NAME),
                                new EqArg(TaskLists.ACCOUNT_TYPE, ACC_TYPE)
                        )),

                new RowInserted(new TaskListsTable(mAuthority),
                        new AllOf(
                                new EqArg(TaskLists.LIST_NAME, "list2"),
                                new EqArg(TaskLists.ACCOUNT_NAME, ACC_NAME),
                                new EqArg(TaskLists.ACCOUNT_TYPE, ACC_TYPE)
                        )),

                new RowExistsAfter(new TasksTable(mAuthority),
                        new AllOf(
                                new EqArg(Tasks.TITLE, "task1"),
                                new EqArg(Tasks.LIST_NAME, "list1"),
                                new ListIdEq(new SingletonRow<>(mClient, new TaskListsTable(mAuthority), TaskLists.LIST_NAME, "list1"))
                        )),

                new RowExistsAfter(new TasksTable(mAuthority),
                        new AllOf(
                                new EqArg(Tasks.TITLE, "task2"),
                                new EqArg(Tasks.LIST_NAME, "list1"),
                                new ListIdEq(new SingletonRow<>(mClient, new TaskListsTable(mAuthority), TaskLists.LIST_NAME, "list1"))
                        )),

                new RowExistsAfter(new TasksTable(mAuthority),
                        new AllOf(
                                new EqArg(Tasks.TITLE, "task3"),
                                new EqArg(Tasks.LIST_NAME, "list2"),
                                new ListIdEq(new SingletonRow<>(mClient, new TaskListsTable(mAuthority), TaskLists.LIST_NAME, "list2"))
                        )),

                new RowExistsAfter(new InstanceTable(mAuthority),
                        new IdEq<>(
                                TaskContract.InstanceColumns.TASK_ID,
                                new SingletonRow<>(mClient, new TasksTable(mAuthority), Tasks.TITLE, "task1"),
                                Tasks._ID)),

                new RowExistsAfter(new InstanceTable(mAuthority),
                        new IdEq<>(
                                TaskContract.InstanceColumns.TASK_ID,
                                new SingletonRow<>(mClient, new TasksTable(mAuthority), Tasks.TITLE, "task2"),
                                Tasks._ID)),

                new RowExistsAfter(new InstanceTable(mAuthority),
                        new IdEq<>(
                                TaskContract.InstanceColumns.TASK_ID,
                                new SingletonRow<>(mClient, new TasksTable(mAuthority), Tasks.TITLE, "task3"),
                                Tasks._ID))
        ));
    }


    @Test
    public void testInsertTaskWithStartAndDue()
    {
        Table<TaskLists> taskListsTable = createTaskListsTable();
        RowSnapshot<TaskLists> taskList = new VirtualRowSnapshot<>(taskListsTable);
        RowSnapshot<Tasks> task = new VirtualRowSnapshot<>(new ListScoped(taskList, new TasksTable(mAuthority)));

        long start = System.currentTimeMillis();
        long due = start + TimeUnit.DAYS.toMillis(1);
        TimeZone timeZone = TimeZone.getDefault();

        OperationsBatch batch = new MultiBatch(
                new Put<>(taskList, new EmptyRowData<TaskLists>()),
                new Put<>(task,
                        new Composite<>(
                                new StartData(start),
                                new DueData(due),
                                new TimeZoneData(timeZone)
                        ))
        );

        assertThat(batch, resultsIn(mClient,

                new RowInserted(new TasksTable(mAuthority),
                        new AllOf(
                                new EqArg(Tasks.DTSTART, start),
                                new EqArg(Tasks.DUE, due),
                                new EqArg(Tasks.TZ, timeZone.getID())
                        )
                ),

                new RowInserted(new InstanceTable(mAuthority),
                        new AllOf(
                                new EqArg(Instances.INSTANCE_START, start),
                                new EqArg(Instances.INSTANCE_DUE, due),
                                new EqArg(Instances.INSTANCE_DURATION, due - start),
                                new EqArg(Tasks.TZ, timeZone.getID())
                        )
                )
        ));
    }


    @Test
    public void testInsertWithStartAndDuration()
    {
        Table<TaskLists> taskListsTable = createTaskListsTable();
        RowSnapshot<TaskLists> taskList = new VirtualRowSnapshot<>(taskListsTable);
        RowSnapshot<Tasks> task = new VirtualRowSnapshot<>(new ListScoped(taskList, new TasksTable(mAuthority)));

        long start = System.currentTimeMillis();
        String durationStr = "PT1H";
        long durationMillis = Duration.parse(durationStr).toMillis();
        TimeZone timeZone = TimeZone.getDefault();

        OperationsBatch batch = new MultiBatch(
                new Put<>(taskList, new EmptyRowData<TaskLists>()),
                new Put<>(task,
                        new Composite<>(
                                new StartData(start),
                                new DurationData(durationStr),
                                new TimeZoneData(timeZone)
                        ))
        );

        assertThat(batch, resultsIn(mClient,

                new RowInserted(new TasksTable(mAuthority),
                        new AllOf(
                                new EqArg(Tasks.DTSTART, start),
                                new EqArg(Tasks.DURATION, durationStr),
                                new EqArg(Tasks.TZ, timeZone.getID())
                        )
                ),

                new RowInserted(new InstanceTable(mAuthority),
                        new AllOf(
                                new EqArg(Instances.INSTANCE_START, start),
                                new EqArg(Instances.INSTANCE_DUE, start + durationMillis),
                                new EqArg(Instances.INSTANCE_DURATION, durationMillis),
                                new EqArg(Tasks.TZ, timeZone.getID())
                        )
                )
        ));
    }


    /**
     * Having a table with a single task. Update task and check if instance updates accordingly.
     */
    @Test
    public void testInstanceUpdate()
    {
        long listId = createTaskList();

        createTaskWithTime(listId, System.currentTimeMillis(), System.currentTimeMillis() + 100); // Task-table with a single task

        assertRowCount(Tasks.getContentUri(mAuthority), 1);

        // Update task with new DUE
        ContentValues values = new ContentValues();
        Long newDue = System.currentTimeMillis();
        values.put(Tasks.DUE, newDue);
        values.put(Tasks.TZ, TimeZone.getDefault().getID());
        mResolver.update(Tasks.getContentUri(mAuthority), values, null, null);

        assertRowCount(Tasks.getContentUri(mAuthority), 1);

        // Check instance
        String[] projection = { Instances.DUE };
        Cursor cursor = mResolver.query(Instances.getContentUri(mAuthority), projection, null, null, null);
        try
        {
            Assert.assertEquals(1, cursor.getCount());
            cursor.moveToNext();
            Assert.assertEquals((Long) newDue, (Long) cursor.getLong(0));
        }
        finally
        {
            cursor.close();
        }
    }


    /**
     * Having a table with a single task. Delete task and check if instance is deleted accordingly.
     */
    @Test
    public void testInstanceDelete()
    {
        long listId = createTaskList();

        createTaskWithTime(listId, System.currentTimeMillis(), System.currentTimeMillis() + 100); // Task-table with a single task

        assertRowCount(Tasks.getContentUri(mAuthority), 1);
        assertRowCount(Instances.getContentUri(mAuthority), 1);

        // Delete the task
        mResolver.delete(createSyncQuery(Tasks.getContentUri(mAuthority).buildUpon(), true), null, null);

        assertRowCount(Tasks.getContentUri(mAuthority), 0);
        assertRowCount(Instances.getContentUri(mAuthority), 0);
    }


    /**
     * LIST_IDs are required on creation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInsertWithOutListId()
    {
        ContentValues values = new ContentValues();
        values.put(Tasks.TITLE, "A task");
        mResolver.insert(Tasks.getContentUri(mAuthority), values);
    }


    /**
     * LIST_IDs have to refer to an existing TaskList.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInsertWithInvalidId()
    {
        ContentValues values = new ContentValues();
        values.put(Tasks.LIST_ID, 5);
        mResolver.insert(Tasks.getContentUri(mAuthority), values);
    }


    private Synced<TaskLists> createTaskListsTable()
    {
        return new Synced<>(new AccountScoped<>(new Account(ACC_NAME, ACC_TYPE), new TaskListsTable(mAuthority)));
    }


    private void createTaskWithTime(long taskListId, long dtstart, long due)
    {
        ContentValues values = new ContentValues();
        values.put(Tasks.LIST_ID, taskListId);
        values.put(Tasks.DTSTART, dtstart);
        values.put(Tasks.DUE, due);
        values.put(Tasks.TZ, TimeZone.getDefault().getID());
        mResolver.insert(Tasks.getContentUri(mAuthority), values);
    }


    private void createTaskWithTime(long taskListId, long dtstart, String duration)
    {
        ContentValues values = new ContentValues();
        values.put(Tasks.LIST_ID, taskListId);
        values.put(Tasks.DTSTART, dtstart);
        values.put(Tasks.DURATION, duration);
        values.put(Tasks.TZ, TimeZone.getDefault().getID());
        mResolver.insert(Tasks.getContentUri(mAuthority), values);
    }


    private void checkExceptions(Cursor cursor, String originalSyncId, Long originalId)
    {
        Assert.assertEquals(1, cursor.getCount());
        try
        {
            cursor.moveToNext();
            String _syncId = cursor.getString(0);
            Long _id = cursor.getLong(1);
            Assert.assertEquals(originalSyncId, _syncId);
            Assert.assertEquals(originalId, _id);
        }
        finally
        {
            cursor.close();
        }
    }


    @Test
    public void testExceptionalInstances()
    {
        long taskListId = createTaskList();

        String originalSyncId1 = "47";
        String originalSyncId2 = "0xdeadbeef";

        ContentValues values = new ContentValues();
        values.put(Tasks.TITLE, "A Task");
        values.put(Tasks.LIST_ID, taskListId);
        values.put(Tasks._SYNC_ID, originalSyncId1);

        Long originalId1 = ContentUris.parseId(mResolver.insert(Tasks.getContentUri(mAuthority), values));

        values.clear();
        values.put(Tasks.TITLE, "Another Task");
        values.put(Tasks.LIST_ID, taskListId);
        values.put(Tasks._SYNC_ID, originalSyncId2);

        Long originalId2 = ContentUris.parseId(mResolver.insert(Tasks.getContentUri(mAuthority), values));

        // add exceptions
        values.clear();
        values.put(Tasks.TITLE, "Exception to first task");
        values.put(Tasks.LIST_ID, taskListId);
        values.put(Tasks.ORIGINAL_INSTANCE_SYNC_ID, originalSyncId1);

        Uri uri1 = mResolver.insert(Tasks.getContentUri(mAuthority), values);

        values.clear();
        values.put(Tasks.TITLE, "Exception to other task");
        values.put(Tasks.LIST_ID, taskListId);
        values.put(Tasks.ORIGINAL_INSTANCE_ID, originalId2);

        Uri uri2 = mResolver.insert(Tasks.getContentUri(mAuthority), values);

        // query and check if the values have been set correctly

        String[] projection = { Tasks.ORIGINAL_INSTANCE_SYNC_ID, Tasks.ORIGINAL_INSTANCE_ID };

        Cursor cursor = mResolver.query(uri1, projection, null, null, null);
        checkExceptions(cursor, originalSyncId1, originalId1);

        cursor = mResolver.query(uri2, projection, null, null, null);
        checkExceptions(cursor, originalSyncId2, originalId2);
    }


    private Long createTaskList()
    {
        return createTaskLists(1).get(0);
    }


    private List<Long> createTaskLists(int numberOfLists)
    {
        List<Long> listIds = new ArrayList<>();
        for (int i = 0; i < numberOfLists; i++)
        {
            ContentValues values = new ContentValues();
            values.put(TaskLists.LIST_NAME, "LocalList_" + (i + 1));
            values.put(TaskLists.VISIBLE, 1);
            values.put(TaskLists.SYNC_ENABLED, 1);
            values.put(TaskLists.OWNER, "");
            Uri inserted = mResolver.insert(
                    TaskLists.getContentUri(mAuthority).buildUpon()
                            .appendQueryParameter(TaskContract.CALLER_IS_SYNCADAPTER, "1")
                            .appendQueryParameter(TaskContract.ACCOUNT_NAME, ACC_NAME)
                            .appendQueryParameter(TaskContract.ACCOUNT_TYPE, ACC_TYPE)
                            .build(),
                    values);
            listIds.add(ContentUris.parseId(inserted));
        }
        return listIds;
    }


    private Uri createSyncQuery(Uri.Builder builder, boolean isSync)
    {
        builder.appendQueryParameter(TaskContract.CALLER_IS_SYNCADAPTER, String.valueOf(isSync));
        builder.appendQueryParameter(TaskLists.ACCOUNT_NAME, ACC_NAME);
        builder.appendQueryParameter(TaskLists.ACCOUNT_TYPE, ACC_TYPE);
        return builder.build();
    }


    private void assertRowCount(Uri uri, int count)
    {
        Cursor cursor = mResolver.query(uri, null, null, null, null);
        try
        {
            Assert.assertEquals(count, cursor.getCount());
        }
        finally
        {
            cursor.close();
        }
    }

}
