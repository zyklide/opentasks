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

package org.dmfs.android.contentpal.rowsnapshots;

import android.support.annotation.NonNull;

import org.dmfs.android.contentpal.RowDataSnapshot;
import org.dmfs.android.contentpal.RowSnapshot;
import org.dmfs.android.contentpal.SoftRowReference;
import org.dmfs.jems.Cached;
import org.dmfs.jems.OnDemand;


/**
 * @author Gabor Keszthelyi
 */
public abstract class LazyRowSnapshot<T> implements RowSnapshot<T>
{
    private final OnDemand<RowSnapshot<T>> mDelegate;


    public LazyRowSnapshot(OnDemand<RowSnapshot<T>> onDemandRowSnapshot)
    {
        mDelegate = new Cached<>(onDemandRowSnapshot);
    }


    @NonNull
    @Override
    public final SoftRowReference<T> reference()
    {
        return mDelegate.get().reference();
    }


    @NonNull
    @Override
    public final RowDataSnapshot<T> values()
    {
        return mDelegate.get().values();
    }
}
