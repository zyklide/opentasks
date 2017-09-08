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

package org.dmfs.android.contentpal.testing.utils;

import org.dmfs.android.contentpal.testing.Check;
import org.dmfs.android.contentpal.testing.VoidCheck;
import org.dmfs.optional.Optional;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;


/**
 * {@link Matcher} adapter for {@link Check}.
 *
 * @author Gabor Keszthelyi
 */
public final class AssertCheckMatcher<T> extends TypeSafeDiagnosingMatcher<T>
{
    private final VoidCheck<T> mCheck;

    private Optional<String> mCachedFailMessage;


    public AssertCheckMatcher(VoidCheck<T> check)
    {
        mCheck = check;
    }


    @Override
    protected boolean matchesSafely(T item, Description mismatchDescription)
    {
        mCheck.verify(item);
        return true;
    }


    @Override
    public void describeTo(Description description)
    {
        throw new UnsupportedOperationException("Should not be called");
    }
}
