/*
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
package org.tomitribe.checkmate;

public class ChecksFailedException extends IllegalArgumentException {
    public ChecksFailedException() {
        this("One or more checks failed");
    }

    public ChecksFailedException(final String message) {
        super(message);
    }

    public ChecksFailedException(final String s, final Throwable t) {
        super(String.format("%s%n%s: %s",
                        s,
                        t.getClass().getSimpleName(),
                        t.getMessage()),
                t);
    }
}
