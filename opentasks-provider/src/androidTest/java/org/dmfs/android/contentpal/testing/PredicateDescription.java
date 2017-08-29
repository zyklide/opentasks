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

import android.text.TextUtils;

import org.dmfs.android.contentpal.Predicate;
import org.dmfs.jems.DelegatingCharSequence;


/**
 * @author Gabor Keszthelyi
 */
public final class PredicateDescription extends DelegatingCharSequence
{
    public PredicateDescription(Predicate predicate)
    {
        // TODO Do it lazily when appropriate classes are available from jems library
        super(String.format("[Predicate] selection: %s | args: %s", predicate.selection(), TextUtils.join(";", predicate.arguments())));
    }
}
