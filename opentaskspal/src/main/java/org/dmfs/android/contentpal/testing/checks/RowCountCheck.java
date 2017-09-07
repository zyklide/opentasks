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

package org.dmfs.android.contentpal.testing.checks;

import android.content.ContentProviderClient;

import org.dmfs.android.contentpal.Predicate;
import org.dmfs.android.contentpal.Table;
import org.dmfs.android.contentpal.rowsets.QueryRowSet;
import org.dmfs.android.contentpal.testing.Check;
import org.dmfs.android.contentpal.testing.utils.PredicateDescription;
import org.dmfs.jems.IterableSize;
import org.dmfs.optional.Absent;
import org.dmfs.optional.Optional;
import org.dmfs.optional.Present;


/**
 * @author Gabor Keszthelyi
 */
public final class RowCountCheck implements Check<ContentProviderClient>
{
    private final Table<?> mTable;
    private final Predicate mPredicate;
    private final int mExpectedRowCount;


    public RowCountCheck(Table<?> table, Predicate predicate, int expectedRowCount)
    {
        mTable = table;
        mPredicate = predicate;
        mExpectedRowCount = expectedRowCount;
    }


    @Override
    public Optional<String> verify(ContentProviderClient client)
    {
        Long rowCount = new IterableSize(new QueryRowSet<>(mTable.view(client), mPredicate)).get();
        if (rowCount == mExpectedRowCount)
        {
            return Absent.absent();
        }
        else
        {
            return new Present<>(String.format("Not %s but %d row(s) found for %s", mExpectedRowCount, rowCount, new PredicateDescription(mPredicate)));
        }
    }

}
