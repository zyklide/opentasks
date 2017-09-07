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

package org.dmfs.android.contentpal.testing.changechecks;

import android.content.ContentProviderClient;

import org.dmfs.android.contentpal.Predicate;
import org.dmfs.android.contentpal.Table;
import org.dmfs.android.contentpal.testing.ChangeCheck;
import org.dmfs.android.contentpal.testing.Check;
import org.dmfs.android.contentpal.testing.checks.Composite;
import org.dmfs.android.contentpal.testing.checks.RowCountCheck;


/**
 * @author Gabor Keszthelyi
 */
public class RowUpdated implements ChangeCheck<ContentProviderClient>
{
    private final Table<?> mTable;
    private final Predicate mPredicateBefore;
    private final Predicate mPredicateAfter;


    public RowUpdated(Table<?> table, Predicate predicateBefore, Predicate predicateAfter)
    {
        mTable = table;
        mPredicateBefore = predicateBefore;
        mPredicateAfter = predicateAfter;
    }


    @Override
    public Check<ContentProviderClient> beforeCheck()
    {
        return new Composite<>(
                new RowCountCheck(mTable, mPredicateBefore, 1),
                new RowCountCheck(mTable, mPredicateAfter, 0)
        );
    }


    @Override
    public Check<ContentProviderClient> afterCheck()
    {
        return new Composite<>(
                new RowCountCheck(mTable, mPredicateBefore, 0),
                new RowCountCheck(mTable, mPredicateAfter, 1)
        );
    }
}
