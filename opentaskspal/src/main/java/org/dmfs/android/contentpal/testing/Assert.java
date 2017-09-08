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

package org.dmfs.android.contentpal.testing;

import android.content.ContentProviderOperation;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.dmfs.android.contentpal.Operation;
import org.dmfs.android.contentpal.RowData;
import org.dmfs.android.contentpal.SoftRowReference;
import org.dmfs.android.contentpal.TransactionContext;
import org.dmfs.android.contentpal.rowdata.EmptyRowData;
import org.dmfs.optional.Absent;
import org.dmfs.optional.Optional;


/**
 * @author Gabor Keszthelyi
 */
public final class Assert<T> implements Operation<T>
{
    private final Uri mTableUri;
    private final RowData<T> mRowData;


    public Assert(Uri tableUri, RowData<T> rowData)
    {
        mTableUri = tableUri;
        mRowData = rowData;
    }


    public Assert(Uri tableUri)
    {
        this(tableUri, new EmptyRowData<T>());
    }


    @NonNull
    @Override
    public Optional<SoftRowReference<T>> reference()
    {
        return Absent.absent();
    }


    @NonNull
    @Override
    public ContentProviderOperation.Builder contentOperationBuilder(@NonNull TransactionContext transactionContext) throws UnsupportedOperationException
    {
        return mRowData.updatedBuilder(ContentProviderOperation.newAssertQuery(mTableUri));
    }
}
