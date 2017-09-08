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
import android.content.OperationApplicationException;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;

import org.dmfs.android.contentpal.OperationsBatch;
import org.dmfs.android.contentpal.OperationsQueue;
import org.dmfs.android.contentpal.batches.SingletonBatch;
import org.dmfs.android.contentpal.queues.BasicOperationsQueue;
import org.dmfs.android.contentpal.testing.AssertChangeCheck;
import org.dmfs.android.contentpal.testing.VoidCheck;
import org.dmfs.android.contentpal.testing.utils.AssertCheckMatcher;
import org.hamcrest.Matcher;

import static junit.framework.Assert.fail;


/**
 * @author Gabor Keszthelyi
 */
public final class AssertingChangeCheck implements VoidCheck<OperationsBatch>
{
    private final AssertChangeCheck[] mChecks;
    private final OperationsQueue mOperationsQueue;


    public AssertingChangeCheck(ContentProviderClient client, AssertChangeCheck... checks)
    {
        this(new BasicOperationsQueue(client), checks);
    }


    public AssertingChangeCheck(OperationsQueue operationsQueue, AssertChangeCheck... checks)
    {
        mOperationsQueue = operationsQueue;
        mChecks = checks;
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT) // TODO
    @Override
    public void verify(OperationsBatch batch)
    {
///                    for (AssertChangeCheck changeCheck : mChecks)
//            {
//                mOperationsQueue.enqueue(new SingletonBatch(changeCheck.beforeAssertOperation()));
//            }
//            mOperationsQueue.flush();

        try
        {
            mOperationsQueue.enqueue(batch);
            mOperationsQueue.flush();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Exception during executing the OperationBatch", e);
        }

        try
        {
            for (AssertChangeCheck changeCheck : mChecks)
            {
                // TODO Multibatch...
                mOperationsQueue.enqueue(new SingletonBatch(changeCheck.afterAssertOperation()));
            }
            mOperationsQueue.flush();

        }
        catch (OperationApplicationException exception)
        {
            fail(exception.getMessage());
        }
        catch (RemoteException e)
        {
            throw new RuntimeException("Exception during executing the after assert OperationBatch", e);
        }
    }


    public static Matcher<OperationsBatch> resultsIn(ContentProviderClient client, AssertChangeCheck... checks)
    {
        return new AssertCheckMatcher<>(new AssertingChangeCheck(client, checks));
    }

}
