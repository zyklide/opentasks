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

import org.dmfs.optional.Optional;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;


/**
 * {@link Matcher} adapter for {@link Check}.
 *
 * @author Gabor Keszthelyi
 */
public final class CheckMatcher<T> extends TypeSafeDiagnosingMatcher<T>
{
    private final Check<T> mCheck;

    private Optional<String> mCachedFailMessage;


    public CheckMatcher(Check<T> check)
    {
        mCheck = check;
    }


    @Override
    protected boolean matchesSafely(T item, Description mismatchDescription)
    {
        /*
         * Note: matchesSafely() is called twice when failing. Caching is not for performance but to
         * guarantee correct working for {@link Check}s that may have different results on multiple runs.
         */
        if (mCachedFailMessage == null)
        {
            mCachedFailMessage = mCheck.verify(item);
        }
        if (mCachedFailMessage.isPresent())
        {
            mismatchDescription.appendText(mCachedFailMessage.value());
        }
        return !mCachedFailMessage.isPresent();
    }


    @Override
    public void describeTo(Description description)
    {

    }
}
