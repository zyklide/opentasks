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

import org.dmfs.android.contentpal.OperationsQueue;
import org.dmfs.android.contentpal.Predicate;
import org.dmfs.android.contentpal.RowSnapshot;
import org.dmfs.android.contentpal.Table;
import org.dmfs.android.contentpal.batches.MultiBatch;
import org.dmfs.android.contentpal.operations.Put;
import org.dmfs.android.contentpal.predicates.AllOf;
import org.dmfs.android.contentpal.predicates.EqArg;
import org.dmfs.android.contentpal.predicates.IdEq;
import org.dmfs.android.contentpal.queues.BasicOperationsQueue;
import org.dmfs.android.contentpal.rowsets.AllRows;
import org.dmfs.android.contentpal.rowsets.QueryRowSet;
import org.dmfs.android.contentpal.rowsnapshots.VirtualRowSnapshot;
import org.dmfs.android.contentpal.tables.AccountScoped;
import org.dmfs.android.contentpal.tables.BaseTable;
import org.dmfs.android.contentpal.tables.Synced;
import org.dmfs.android.contentpal.testing.RowExistsAfter;
import org.dmfs.android.contentpal.testing.RowInserted;
import org.dmfs.android.contentpal.testing.SingletonRow;
import org.dmfs.provider.tasks.opentaskspal.tables.ListScoped;
import org.dmfs.provider.tasks.opentaskspal.tables.TaskListsTable;
import org.dmfs.provider.tasks.opentaskspal.tables.TasksTable;
import org.dmfs.provider.tasks.opentaskspal.tasklists.Named;
import org.dmfs.provider.tasks.opentaskspal.tasks.Titled;
import org.dmfs.rfc5545.Duration;
import org.dmfs.tasks.contract.TaskContract;
import org.dmfs.tasks.contract.TaskContract.Instances;
import org.dmfs.tasks.contract.TaskContract.TaskLists;
import org.dmfs.tasks.contract.TaskContract.Tasks;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import static org.dmfs.android.contentpal.testing.ContentChangeCheck.resultsIn;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


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


    @Before
    public void setUp() throws Exception
    {
        mContext = InstrumentationRegistry.getTargetContext();
        mResolver = mContext.getContentResolver();
        mAuthority = AuthorityUtil.taskAuthority(mContext);
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
        mResolver.delete(createSyncQuery(Tasks.getContentUri(mAuthority).buildUpon(), true), null, null);
        mResolver.delete(createSyncQuery(TaskLists.getContentUri(mAuthority).buildUpon(), true), null, null);
    }


    @Test
    public void testSingleInsert00() throws Exception
    {
        ContentProviderClient client = mContext.getContentResolver().acquireContentProviderClient(mAuthority);

        Table<TaskLists> taskListsTable = new Synced<>(new AccountScoped<>(new Account(ACC_NAME, ACC_TYPE), new TaskListsTable(mAuthority)));
        RowSnapshot<TaskLists> taskList = new VirtualRowSnapshot<>(taskListsTable);
        RowSnapshot<Tasks> task = new VirtualRowSnapshot<>(new ListScoped(taskList, new TasksTable(mAuthority)));

        MultiBatch batch = new MultiBatch(
                new Put<>(taskList, new Named("list 1")),
                new Put<>(task, new Titled("task title 1"))
        );

        assertThat(batch, resultsIn(client,

                new RowInserted(new TaskListsTable(mAuthority),
                        new AllOf(
                                new EqArg(TaskLists.LIST_NAME, "list 1"),
                                new EqArg(TaskLists.ACCOUNT_NAME, ACC_NAME),
                                new EqArg(TaskLists.ACCOUNT_TYPE, ACC_TYPE)
                        )),

                new RowInserted(new TasksTable(mAuthority), Tasks.TITLE, "task title 1"),

                new RowExistsAfter(new BaseTable<>(Instances.getContentUri(mAuthority)),
                        new IdEq<>(TaskContract.InstanceColumns.TASK_ID,
                                // TODO This has to be lazy or use the virtual snapshot somehow
                                new SingletonRow<>(client, new TasksTable(mAuthority), Tasks.TITLE, "task title 1").get(),
                                Tasks._ID))
        ));

        // TODO Check if it was added to Instances table? Is that white or black box?
        // TODO Related row check for the Instance table, same task id added, how to do that
    }


    @Test
    public void testSingleInsert() throws Exception
    {
        ContentProviderClient client = mResolver.acquireContentProviderClient(mAuthority);
        OperationsQueue operationsQueue = new BasicOperationsQueue(client);

        Table<TaskLists> taskListsTable = new Synced<>(new AccountScoped<>(new Account(ACC_NAME, ACC_TYPE), new TaskListsTable(mAuthority)));
        RowSnapshot<TaskLists> taskList = new VirtualRowSnapshot<>(taskListsTable);
        Table<TaskContract.Tasks> taskTable = new ListScoped(taskList, new TasksTable(mAuthority));
        RowSnapshot<Tasks> task = new VirtualRowSnapshot<>(taskTable);

        operationsQueue.enqueue(new MultiBatch(
                new Put<>(taskList, new Named("list 1")),
                new Put<>(task, new Titled("task title 1"))
        ));

        operationsQueue.flush();
        client.release();

        // Verification:

        // TODO improve verification, hamcrest iterable contains (num of elements), toString()s, Integer.valueOf(), etc
        // TODO ClosableIterator-s here, close them

        assertQuery(client, new TaskListsTable(mAuthority), new EqArg(TaskLists.LIST_NAME, "list 1"));

        assertQuery(client, new TasksTable(mAuthority), new EqArg(Tasks.TITLE, "task title 1"));

        Table<Tasks> verifyTasksTable = new TasksTable(mAuthority);
        RowSnapshot<Tasks> verifyTask = new AllRows<>(verifyTasksTable.view(client)).iterator().next();
        assertThat(verifyTask.values().charData(Tasks.TITLE).value().toString(), is("task title 1"));
        Integer verifyTaskId = Integer.valueOf(verifyTask.values().charData(Tasks._ID).value().toString());

        Table<Instances> verifyInstanceTable = new BaseTable<>(Instances.getContentUri(mAuthority));
        RowSnapshot<Instances> verifyInstanceRow = new AllRows<>(verifyInstanceTable.view(client)).iterator().next();
        assertThat(verifyInstanceRow.values().charData(TaskContract.InstanceColumns.TASK_ID).value().toString(), is(verifyTaskId.toString()));
    }


    private <T> void assertQuery(ContentProviderClient client, Table<T> table, Predicate predicate)
    {
        assertThat(new QueryRowSet<>(table.view(client), predicate), IsIterableWithSize.<RowSnapshot<T>>iterableWithSize(1));
    }


    /**
     * Creates three new Tasks. One of them will refer to a different TasksList.
     */
    @Test
    public void testMultipleInserts()
    {
        List<Long> listIds = createTaskLists(3);

        // Add two tasks with list id 1
        ContentValues values = new ContentValues();

        values.put(Tasks.LIST_ID, listIds.get(0));
        values.put(Tasks.TITLE, "A Task");
        mResolver.insert(Tasks.getContentUri(mAuthority), values);

        values.clear();
        values.put(Tasks.LIST_ID, listIds.get(0));
        values.put(Tasks.TITLE, "A second Task");
        mResolver.insert(Tasks.getContentUri(mAuthority), values);

        // Add a tasklist
        values.clear();
        // TODO What is this?:
        mResolver.insert(createSyncQuery(TaskLists.getContentUri(mAuthority).buildUpon(), true), values);

        // Add another task which refers to list #2
        values.clear();

        values.put(Tasks.LIST_ID, listIds.get(1));
        values.put(Tasks.TITLE, "A third Task");
        mResolver.insert(Tasks.getContentUri(mAuthority), values);

        // Check if Tasks contains three entries
        String[] projection = { Tasks._ID };
        Cursor cursor = mResolver.query(Tasks.getContentUri(mAuthority), projection, null, null, null);
        assertEquals(3, cursor.getCount());

        Set<String> ids = new HashSet<String>(); // save ids for later check
        while (cursor.moveToNext())
        {
            ids.add(cursor.getString(0));
        }

        // Check if instances also contains three entries (including the correct task_ids)
        projection = new String[] { Instances.TASK_ID };
        cursor = mResolver.query(Instances.getContentUri(mAuthority), projection, null, null, null);
        assertEquals(3, cursor.getCount());
        String taskId;

        while (cursor.moveToNext())
        { // if task_id matches => remove (ids has to be empty afterwards
            taskId = cursor.getString(0);
            if (ids.contains(taskId))
            {
                ids.remove(taskId);
            }
        }
        assertEquals(0, ids.size());
        cursor.close();
    }


    @Test
    public void testDelete()
    {
        // TODO Should not depend on the other test:
        testMultipleInserts(); // quick way to create a test database
        Cursor cursor = mResolver.query(Tasks.getContentUri(mAuthority), null, null, null, null);
        assertEquals(3, cursor.getCount());

        // Try to delete the task with the title "A second task"
        Uri syncUri = createSyncQuery(Tasks.getContentUri(mAuthority).buildUpon(), true);
        String where = Tasks.TITLE + "=?";
        String[] selectionArgs = { "A second Task" };
        mResolver.delete(syncUri, where, selectionArgs);

        String[] projection = { Tasks.TITLE };
        cursor = mResolver.query(Tasks.getContentUri(mAuthority), projection, null, null, null);
        assertEquals(2, cursor.getCount());
        while (cursor.moveToNext())
        {
            assertTrue(cursor.getString(0).compareTo("A second Task") != 0);
        }
        cursor.close();
    }


    @Test
    public void testInstanceWithGivenTime()
    {
        long listId = createTaskList();

        long start = System.currentTimeMillis();
        long due = System.currentTimeMillis() + 30000 / 1000 * 1000;
        createTaskWithTime(listId, start, due);

        // Check if a corresponding Instance exists
        String[] projection = { Instances.INSTANCE_START, Instances.INSTANCE_DUE, Instances.INSTANCE_DURATION };
        Cursor cursor = mResolver.query(Instances.getContentUri(mAuthority), projection, null, null, null);
        try
        {
            // there can be only one
            assertEquals(1, cursor.getCount());

            // Compare timestamps
            cursor.moveToNext();
            assertEquals(start, cursor.getLong(0));
            assertEquals(due, cursor.getLong(1));
            assertEquals((due - start), cursor.getLong(2));
        }
        finally
        {
            cursor.close();
        }
    }


    @Test
    public void testInstanceWithDuration()
    {
        long listId = createTaskList();

        long dtstart = System.currentTimeMillis() / 1000 * 1000;
        String durationStr = "PT1H";
        long duration = Duration.parse(durationStr).toMillis();
        createTaskWithTime(listId, dtstart, "PT1H");

        String[] projection = { Instances.INSTANCE_START, Instances.INSTANCE_DUE, Instances.INSTANCE_DURATION };
        Cursor cursor = mResolver.query(Instances.getContentUri(mAuthority), projection, null, null, null);
        try
        {
            // there can be only one
            assertEquals(1, cursor.getCount());

            // Compare timestamps
            cursor.moveToNext();
            assertEquals(dtstart, cursor.getLong(0));
            assertEquals(duration, cursor.getLong(2));
            assertEquals((dtstart + duration), cursor.getLong(1));
        }
        finally
        {
            cursor.close();
        }
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
            assertEquals(1, cursor.getCount());
            cursor.moveToNext();
            assertEquals((Long) newDue, (Long) cursor.getLong(0));
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
        assertEquals(1, cursor.getCount());
        try
        {
            cursor.moveToNext();
            String _syncId = cursor.getString(0);
            Long _id = cursor.getLong(1);
            assertEquals(originalSyncId, _syncId);
            assertEquals(originalId, _id);
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
            assertEquals(count, cursor.getCount());
        }
        finally
        {
            cursor.close();
        }
    }

}
