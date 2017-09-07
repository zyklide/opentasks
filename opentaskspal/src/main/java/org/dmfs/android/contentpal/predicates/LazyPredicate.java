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

package org.dmfs.android.contentpal.predicates;

import android.support.annotation.NonNull;

import org.dmfs.android.contentpal.Predicate;
import org.dmfs.jems.Cached;
import org.dmfs.jems.OnDemand;


/**
 * @author Gabor Keszthelyi
 */
public abstract class LazyPredicate implements Predicate
{
    private final OnDemand<Predicate> mDelegate;


    public LazyPredicate(OnDemand<Predicate> onDemandPredicate)
    {
        mDelegate = new Cached<>(onDemandPredicate);
    }


    @NonNull
    @Override
    public final CharSequence selection()
    {
        return mDelegate.get().selection();
    }


    @NonNull
    @Override
    public Iterable<String> arguments()
    {
        return mDelegate.get().arguments();
    }
}
