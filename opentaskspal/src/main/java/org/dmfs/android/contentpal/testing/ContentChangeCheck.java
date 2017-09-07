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
import android.content.Context;

import org.dmfs.android.contentpal.OperationsBatch;
import org.dmfs.android.contentpal.OperationsQueue;
import org.dmfs.android.contentpal.queues.BasicOperationsQueue;
import org.dmfs.optional.Absent;
import org.dmfs.optional.Optional;
import org.dmfs.optional.Present;
import org.hamcrest.Matcher;


/**
 * @author Gabor Keszthelyi
 */
public final class ContentChangeCheck implements Check<OperationsBatch>
{
    private final ContentProviderClient mClient;
    private final ChangeCheck<ContentProviderClient>[] mChecks;

    private Optional<String> mFailMsg = new Absent<>();


    public ContentChangeCheck(ContentProviderClient client, ChangeCheck... checks)
    {
        mClient = client;
        mChecks = checks;
    }


    public static Matcher<OperationsBatch> resultsIn(Context context, String authority, ChangeCheck<ContentProviderClient>... checks)
    {
        return new CheckMatcher<>(new ContentChangeCheck(context.getContentResolver().acquireContentProviderClient(authority), checks));
    }


    public static Matcher<OperationsBatch> resultsIn(ContentProviderClient client, ChangeCheck<ContentProviderClient>... checks)
    {
        return new CheckMatcher<>(new ContentChangeCheck(client, checks));
    }


    @Override
    public Optional<String> verify(OperationsBatch batch)
    {
        for (ChangeCheck<ContentProviderClient> changeCheck : mChecks)
        {
            Optional<String> failMsg = changeCheck.beforeCheck().verify(mClient);
            if (failMsg.isPresent())
            {
                return new Present<>("Before check failed with: " + failMsg.value());
            }
        }

        OperationsQueue operationsQueue = new BasicOperationsQueue(mClient);
        try
        {
            operationsQueue.enqueue(batch);
            operationsQueue.flush();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Exception during executing the OperationBatch", e);
        }

        for (ChangeCheck<ContentProviderClient> changeCheck : mChecks)
        {
            Optional<String> failMsg = changeCheck.afterCheck().verify(mClient);
            if (failMsg.isPresent())
            {
                return new Present<>("After check failed with: " + failMsg.value());
            }
        }

        mClient.release();

        return Absent.absent();
    }
}
