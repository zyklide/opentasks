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

import android.content.ContentProviderClient;

import org.dmfs.android.contentpal.Predicate;
import org.dmfs.android.contentpal.Table;
import org.dmfs.android.contentpal.predicates.EqArg;


/**
 * @author Gabor Keszthelyi
 */
public class RowInserted implements ChangeCheck<ContentProviderClient>
{
    private final Table<?> mTable;
    private final Predicate mPredicate;


    public RowInserted(Table<?> table, Predicate predicate)
    {
        mTable = table;
        mPredicate = predicate;
    }


    public RowInserted(Table<?> table, String columnName, String value)
    {
        this(table, new EqArg(columnName, value));
    }


    @Override
    public Check<ContentProviderClient> beforeCheck()
    {
        return new RowCountCheck(mTable, mPredicate, 0);
    }


    @Override
    public Check<ContentProviderClient> afterCheck()
    {
        return new RowCountCheck(mTable, mPredicate, 1);
    }
}
