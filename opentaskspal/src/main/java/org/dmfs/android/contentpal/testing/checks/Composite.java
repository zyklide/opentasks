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

import org.dmfs.android.contentpal.testing.Check;
import org.dmfs.optional.Absent;
import org.dmfs.optional.Optional;


/**
 * @author Gabor Keszthelyi
 */
public final class Composite<T> implements Check<T>
{
    private final Check<T>[] mChecks;


    public Composite(Check<T>... checks)
    {
        mChecks = checks;
    }


    @Override
    public Optional<String> verify(T target)
    {
        for (Check<T> check : mChecks)
        {
            Optional<String> failMsg = check.verify(target);
            if (failMsg.isPresent())
            {
                return failMsg;
            }
        }
        return Absent.absent();
    }
}
