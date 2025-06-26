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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Checks {

    Check check(final String item);

    Checks check(final String item, final Supplier<Boolean> check);

    Checks check(final String item, final Supplier<Boolean> check, final WhenFalse onFalse);

    <X extends Throwable> Checks orThrow(final Supplier<? extends X> exceptionSupplier) throws X;

    <T> ObjectChecks<T> object(final String description, final T object);

    <T> ObjectChecks<T> object(final String description, final Supplier<T> supplier);

    boolean result();

    interface ObjectChecks<T> {

        ObjectChecks<T> check(final String description, final Function<T, Boolean> check);

        /**
         * A check can issue a warning instead of a failure.  The result still returns false
         * and subsequent checks are still not called.  If you need to call a warning and
         * continue doing checks, they need to be separated as follows:
         *
         * keystoreChecks = checks.object("keystoreFile", Paths.get(""));
         *
         * keystoreChecks.check("is defined", path -> false, WARN);
         * keystoreChecks.check("is readable", function)
         *                 .check("is writable", function);
         */
        ObjectChecks<T> check(final String description, final Function<T, Boolean> check, final WhenFalse whenFalse);

        <R> ObjectChecks<R> map(final String description, Function<? super T, ? extends R> mapper);

        <R> ObjectChecks<R> map(final Function<? super T, ? extends R> mapper);

        /**
         * If any checks have returned false, evaluate this object instead
         */
        ObjectChecks<T> or(final String description, final T object);

        /**
         * If any checks have returned false, evaluate the supplied object instead
         */
        ObjectChecks<T> or(final String description, final Supplier<T> supplier);

        /**
         * Returns the object being evaluated in this series of checks if all checks
         * have returned true.  If any check returned false, the supplier will be called
         * and the resulting exception will be thrown.
         */
        <X extends Throwable> T getOrThrow(final Supplier<? extends X> exceptionSupplier) throws X;

        <X extends Throwable> ObjectChecks<T> orThrow(final Supplier<? extends X> exceptionSupplier) throws X;

        boolean result();
    }

    /**
     * How should a false status be handled?
     */
    public enum WhenFalse {
        FAIL,
        WARN
    }

    static Builder builder() {
        return new Builder();
    }

    class Builder {
        final List<CheckLogger> loggers = new ArrayList<>();

        public Builder() {
        }

        /**
         * May be called multiple times to add several loggers.
         * Loggers will be executed in the order they are added.
         */
        public Builder logger(final CheckLogger logger) {
            loggers.add(logger);
            return this;
        }

        public Builder print(final PrintStream out, final int width) {
            loggers.add(new PrintStreamListener(out, width));
            return this;
        }

        public Checks build() {
            if (loggers.isEmpty()) {
                return new ChecksImpl(new NoOpLogger());
            }

            if (loggers.size() == 1) {
                return new ChecksImpl(loggers.get(0));
            }

            return new ChecksImpl(new LoggersList(loggers));
        }
    }
}
