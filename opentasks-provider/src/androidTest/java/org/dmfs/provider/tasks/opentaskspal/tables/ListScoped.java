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

package org.dmfs.provider.tasks.opentaskspal.tables;

import android.content.ContentProviderClient;
import android.support.annotation.NonNull;

import org.dmfs.android.contentpal.InsertOperation;
import org.dmfs.android.contentpal.Operation;
import org.dmfs.android.contentpal.Predicate;
import org.dmfs.android.contentpal.RowSnapshot;
import org.dmfs.android.contentpal.Table;
import org.dmfs.android.contentpal.UriParams;
import org.dmfs.android.contentpal.View;
import org.dmfs.provider.tasks.opentaskspal.operations.Task;
import org.dmfs.provider.tasks.opentaskspal.predicates.ListIdEq;
import org.dmfs.tasks.contract.TaskContract;


public final class ListScoped implements Table<TaskContract.Tasks>
{
    private final Table<TaskContract.Tasks> mDelegate;
    private final RowSnapshot<TaskContract.TaskLists> mTaskListRow;


    public ListScoped(@NonNull RowSnapshot<TaskContract.TaskLists> taskListRow, @NonNull Table<TaskContract.Tasks> delegate)
    {
        mDelegate = delegate;
        mTaskListRow = taskListRow;
    }


    @NonNull
    @Override
    public InsertOperation<TaskContract.Tasks> insertOperation(@NonNull UriParams uriParams)
    {
        return new Task(mTaskListRow, mDelegate.insertOperation(uriParams));
    }


    @NonNull
    @Override
    public Operation<TaskContract.Tasks> updateOperation(@NonNull UriParams uriParams, @NonNull Predicate predicate)
    {
        return mDelegate.updateOperation(uriParams, new ListIdEq(predicate, mTaskListRow));
    }


    @NonNull
    @Override
    public Operation<TaskContract.Tasks> deleteOperation(@NonNull UriParams uriParams, @NonNull Predicate predicate)
    {
        return mDelegate.deleteOperation(uriParams, new ListIdEq(predicate, mTaskListRow));
    }


    @NonNull
    @Override
    public View<TaskContract.Tasks> view(@NonNull ContentProviderClient client, @NonNull String... projection)
    {
        return new org.dmfs.provider.tasks.opentaskspal.views.ListScoped(mTaskListRow, mDelegate.view(client, projection));
    }
}