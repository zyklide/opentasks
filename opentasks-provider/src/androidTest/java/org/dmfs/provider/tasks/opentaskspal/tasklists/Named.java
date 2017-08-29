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

package org.dmfs.provider.tasks.opentaskspal.tasklists;

import android.content.ContentProviderOperation;
import android.support.annotation.NonNull;

import org.dmfs.tasks.contract.TaskContract;


public final class Named implements TaskListRowData
{
    private final CharSequence mName;


    public Named(@NonNull CharSequence name)
    {
        mName = name;
    }


    @NonNull
    @Override
    public ContentProviderOperation.Builder updatedBuilder(@NonNull ContentProviderOperation.Builder builder)
    {
        return builder.withValue(TaskContract.TaskLists.LIST_NAME, mName.toString());
    }
}