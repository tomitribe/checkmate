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

/**
 * A fluent utility for executing validation checks with controlled failure handling,
 * structured object chaining, and customizable logging.
 *
 * <p>This interface allows you to build sequences of checks that can fail fast,
 * optionally warn, log results, and provide fallback or exception-based handling.
 *
 * <p>Typical use cases include configuration validation, startup environment checks,
 * and deployment assertions.
 */
public interface Checks {
    /**
     * Begin checking a specific object, associating it with a name for log output.
     *
     * @param description the label to use when printing check results
     * @param object the object to check
     * @param <T> the type of the object
     * @return a fluent ObjectChecks interface to validate the object
     */
    <T> ObjectChecks<T> object(final String description, final T object);

    /**
     * Like {@link #object(String, Object)} but lazily supplies the object.
     *
     * @param description the label for logging
     * @param supplier the lazy object supplier
     * @param <T> the type of the object
     * @return a fluent ObjectChecks interface to validate the object
     */
    <T> ObjectChecks<T> object(final String description, final Supplier<T> supplier);

    /**
     * Begin a check for the given item, returning a {@link Check} to declare the result.
     *
     * @param item the item being checked
     * @return a Check object to explicitly mark as pass/fail/error/etc
     */
    Check check(final String item);

    /**
     * Perform a simple boolean check on the item. If the supplier returns {@code false},
     * the failure is logged and subsequent chained checks (if any) are skipped.
     *
     * @param item the description of what is being checked
     * @param check a supplier returning {@code true} for success or {@code false} for failure
     * @return this instance for fluent chaining
     */
    Checks check(final String item, final Supplier<Boolean> check);

    /**
     * Perform a check that may be configured to log a warning instead of a failure.
     * If a warning is issued, the result still counts as false and subsequent chained checks are skipped.
     *
     * @param item the description of the check
     * @param check the logic to evaluate
     * @param onFalse what to do if the check fails—fail or warn
     * @return this instance for fluent chaining
     */
    Checks check(final String item, final Supplier<Boolean> check, final WhenFalse onFalse);

    /**
     * Throws the supplied exception if any previous check has failed or warned.
     *
     * @param exceptionSupplier the supplier to produce the exception
     * @param <X> the type of exception thrown
     * @return this instance for fluent chaining if no failure occurred
     * @throws X if any check returned false
     */
    <X extends Throwable> Checks orThrow(final Supplier<? extends X> exceptionSupplier) throws X;


    /**
     * Returns the outcome of the evaluation: {@code true} if all checks passed, {@code false} otherwise.
     *
     * @return whether the check series passed overall
     */
    boolean result();

    /**
     * Fluent API for performing multiple validations on a specific object.
     * Automatically short-circuits subsequent checks and mappings if any fail.
     *
     * @param <T> the current object type being checked
     */
    interface ObjectChecks<T> {

        /**
         * Validates the object with a predicate.
         *
         * @param description description of the check
         * @param check predicate to apply
         * @return this object for fluent chaining
         */
        ObjectChecks<T> check(final String description, final Function<T, Boolean> check);

        /**
         * Performs a check that can issue a warning instead of a failure.
         * Results still count as failed, and skip following steps.
         *
         * @param description the check label
         * @param check the predicate logic
         * @param whenFalse action to take if check fails (FAIL or WARN)
         * @return this object for fluent chaining
         */
        ObjectChecks<T> check(final String description, final Function<T, Boolean> check, final WhenFalse whenFalse);

        /**
         * Transforms the current object into another object for continued checks.
         * If any prior check failed, the mapping is skipped.
         *
         * @param description label of the new subobject
         * @param mapper function to transform the object
         * @param <R> the type of the new subobject
         * @return a new ObjectChecks for the mapped object
         */
        <R> ObjectChecks<R> map(final String description, Function<? super T, ? extends R> mapper);

        /**
         * Transforms the object without changing the label.
         *
         * @param mapper transformation logic
         * @param <R> resulting object type
         * @return a new ObjectChecks for the mapped object
         */
        <R> ObjectChecks<R> map(final Function<? super T, ? extends R> mapper);

        /**
         * If any previous checks failed, switch to evaluating a fallback object.
         *
         * @param description label for the fallback
         * @param object the fallback object
         * @return checks for the fallback object
         */
        ObjectChecks<T> or(final String description, final T object);

        /**
         * Like {@link #or(String, Object)} but lazily provides the fallback.
         *
         * @param description fallback label
         * @param supplier supplies the fallback object
         * @return checks for the fallback object
         */
        ObjectChecks<T> or(final String description, final Supplier<T> supplier);

        /**
         * Returns the current object if all checks passed, or throws the given exception.
         *
         * @param exceptionSupplier supplies the exception to throw
         * @param <X> exception type
         * @return the checked object
         * @throws X if any check failed
         */
        <X extends Throwable> T getOrThrow(final Supplier<? extends X> exceptionSupplier) throws X;

        /**
         * Throws the given exception if any check failed so far.
         *
         * @param exceptionSupplier supplies the exception
         * @param <X> exception type
         * @return this object for fluent chaining
         * @throws X if any check failed
         */
        <X extends Throwable> ObjectChecks<T> orThrow(final Supplier<? extends X> exceptionSupplier) throws X;

        /**
         * Returns {@code true} if all checks passed and no warnings/failures occurred.
         *
         * @return the result of the entire object check sequence
         */
        boolean result();
    }

    /**
     * Specifies how to interpret a check result that returns false.
     */
    enum WhenFalse {
        /**
         * Mark as a failure and skip remaining checks.
         */
        FAIL,

        /**
         * Mark as a warning, still count as a failure, skip remaining checks.
         */
        WARN
    }

    /**
     * Factory for building a {@link Checks} instance with configurable loggers.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builds {@link Checks} instances with optional logging capabilities.
     */
    class Builder {
        final List<CheckLogger> loggers = new ArrayList<>();

        public Builder() {
        }

        /**
         * Adds a logger to receive check results.
         * Multiple loggers may be added in order.
         *
         * @param logger the logger to add
         * @return this builder for fluent chaining
         */
        public Builder logger(final CheckLogger logger) {
            loggers.add(logger);
            return this;
        }

        /**
         * Adds a simple print-based logger to output results to a stream.
         *
         * @param out the destination stream
         * @param width the column width to pad messages for alignment
         * @return this builder for fluent chaining
         */
        public Builder print(final PrintStream out, final int width) {
            loggers.add(new PrintStreamListener(out, width));
            return this;
        }

        /**
         * Constructs the {@link Checks} instance.
         *
         * @return a ready-to-use Checks object
         */
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