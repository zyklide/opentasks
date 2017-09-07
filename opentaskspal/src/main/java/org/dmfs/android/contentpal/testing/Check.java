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


/**
 * Represents a check/matcher/condition/verification that can be checked against a target object of type <code>T</code>.
 *
 * @author Gabor Keszthelyi
 */
public interface Check<T>
{
    /**
     * Verifies if the condition represented by this {@link Check} is met for the given target.
     *
     * @param target
     *         the object to check
     *
     * @return absent if the target verifies successfully, a present fail message describing the mismatch otherwise
     */
    Optional<String> verify(T target);
}
