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
import org.dmfs.android.contentpal.RowSnapshot;
import org.dmfs.android.contentpal.Table;
import org.dmfs.android.contentpal.predicates.EqArg;
import org.dmfs.android.contentpal.rowsets.QueryRowSet;
import org.dmfs.android.contentpal.rowsnapshots.LazyRowSnapshot;
import org.dmfs.android.contentpal.tools.ClosableIterator;
import org.dmfs.jems.OnDemand;

import java.io.IOException;


/**
 * @author Gabor Keszthelyi
 */
public final class SingletonRow<T> extends LazyRowSnapshot<T>
{

    public SingletonRow(final ContentProviderClient client, final Table<T> table, final Predicate predicate)
    {
        super(new OnDemand<RowSnapshot<T>>()
        {
            @Override
            public RowSnapshot<T> get()
            {
                ClosableIterator<RowSnapshot<T>> rowSetIterator = new QueryRowSet<>(table.view(client), predicate).iterator();

                if (!rowSetIterator.hasNext())
                {
                    throw new RuntimeException(
                            String.format("No matching row found in table %s for %s", table, new PredicateDescription(predicate)));
                }

                RowSnapshot<T> rowSnapshot = rowSetIterator.next();
                if (rowSetIterator.hasNext())
                {
                    throw new RuntimeException(
                            String.format("More than one row found in table %s for %s", table, new PredicateDescription(predicate)));
                }

                try
                {
                    rowSetIterator.close();
                }
                catch (IOException e)
                {
                    throw new RuntimeException("Couldn't close RowSet iterator", e);
                }

                return rowSnapshot;
            }
        });
    }


    public SingletonRow(ContentProviderClient client, Table<T> table, String column, String value)
    {
        this(client, table, new EqArg(column, value));
    }

}
