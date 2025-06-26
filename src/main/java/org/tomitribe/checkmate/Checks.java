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
import java.util.function.Function;
import java.util.function.Supplier;

import static org.tomitribe.checkmate.Checks.WhenFalse.FAIL;
import static org.tomitribe.checkmate.Checks.WhenFalse.WARN;

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

        <X extends Throwable> ObjectChecks<T>  orThrow(final Supplier<? extends X> exceptionSupplier) throws X;

        boolean result();
    }

    /**
     * How should a false status be handled?
     */
    public enum WhenFalse {
        FAIL,
        WARN
    }

    interface Listener {
        Check check(final String name);
    }

    class ChecksImpl implements Checks {
        private final Listener listener;

        public ChecksImpl(final PrintStream out, final int column) {
            this.listener = new PrintStreamListener(out, column);
        }

        @Override
        public Check check(final String item) {
            return listener.check(item);
        }

        @Override
        public boolean result() {
            return true;
        }

        @Override
        public Checks check(final String item, final Supplier<Boolean> check) {
            return check(item, check, FAIL);
        }

        //CHECKSTYLE:OFF
        @Override
        public Checks check(final String item, final Supplier<Boolean> check, final WhenFalse whenFalse) {

            final Check check1 = check(item);
            try {
                final Boolean status = check.get();
                if (Boolean.TRUE.equals(status)) {
                    check1.pass();
                    return this;
                } else {
                    if (whenFalse.equals(FAIL)) check1.fail();
                    else if (whenFalse.equals(WARN)) check1.warn();
                    else throw new UnsupportedOperationException("Unkown value: " + whenFalse);

                    return new Skip();
                }
            } catch (Throwable t) {
                check1.error(t.getClass().getSimpleName() + ": " + t.getMessage());
                return new Skip();
            }
        }
        //CHECKSTYLE:ON

        @Override
        public <X extends Throwable> Checks orThrow(final Supplier<? extends X> exceptionSupplier) throws X {
            return this;
        }

        @Override
        public <T> ObjectChecks<T> object(final String description, final T object) {
            return new ObjectChecksImpl<>(this, object, description);
        }

        @Override
        public <T> ObjectChecks<T> object(final String description, final Supplier<T> supplier) {
            return new ObjectChecksImpl<>(this, supplier.get(), description);
        }

        private class Skip implements Checks {
            @Override
            public Check check(final String item) {
                return ChecksImpl.this.check(item);
            }

            @Override
            public Checks check(final String item, final Supplier<Boolean> check) {
                check(item).skip();
                return this;
            }

            @Override
            public Checks check(final String item, final Supplier<Boolean> check, final WhenFalse onFalse) {
                check(item).skip();
                return this;
            }

            @Override
            public <X extends Throwable> Checks orThrow(final Supplier<? extends X> exceptionSupplier) throws X {
                throw exceptionSupplier.get();
            }

            @Override
            public boolean result() {
                return false;
            }

            @Override
            public <T> ObjectChecks<T> object(final String description, final T object) {
                return new ObjectChecksImpl(ChecksImpl.this, null, description).new Ignore();
            }

            @Override
            public <T> ObjectChecks<T> object(final String description, final Supplier<T> supplier) {
                return new ObjectChecksImpl(ChecksImpl.this, null, description).new Ignore();
            }
        }

        private static class ObjectChecksImpl<T> implements ObjectChecks<T> {
            private final Checks checks;
            private final T object;
            private final String prefix;

            public ObjectChecksImpl(final Checks checks, final T object, final String prefix) {
                this.checks = checks;
                this.object = object;
                this.prefix = prefix;
            }

            @Override
            public ObjectChecks<T> check(final String description, final Function<T, Boolean> check) {
                return check(description, check, FAIL);
            }

            //CHECKSTYLE:OFF
            @Override
            public ObjectChecks<T> check(final String description, final Function<T, Boolean> check, final WhenFalse whenFalse) {
                final Check check1 = checks.check(prefix + " " + description);
                try {
                    final Boolean status = check.apply(object);
                    if (Boolean.TRUE.equals(status)) {
                        check1.pass();
                        return this;
                    } else {
                        if (whenFalse.equals(FAIL)) check1.fail();
                        else if (whenFalse.equals(WARN)) check1.warn();
                        else throw new UnsupportedOperationException("Unkown value: " + whenFalse);
                        return new Skip();
                    }
                } catch (Throwable t) {
                    check1.error(t.getClass().getSimpleName() + ": " + t.getMessage());
                    return new Skip();
                }
            }
            //CHECKSTYLE:ON

            @Override
            public boolean result() {
                return true;
            }

            @Override
            public <R> ObjectChecks<R> map(final String description, final Function<? super T, ? extends R> mapper) {
                return new ObjectChecksImpl<R>(checks, mapper.apply(object), prefix + " " + description);
            }

            @Override
            public <R> ObjectChecks<R> map(final Function<? super T, ? extends R> mapper) {
                return new ObjectChecksImpl<R>(checks, mapper.apply(object), prefix);
            }

            @Override
            public ObjectChecks<T> or(final String description, final T object) {
                return this;
            }

            @Override
            public ObjectChecks<T> or(final String description, final Supplier<T> supplier) {
                return this;
            }

            @Override
            public <X extends Throwable> T getOrThrow(final Supplier<? extends X> exceptionSupplier) throws X {
                return object;
            }

            @Override
            public <X extends Throwable> ObjectChecks<T> orThrow(final Supplier<? extends X> exceptionSupplier) throws X {
                return this;
            }

            public class Skip implements ObjectChecks<T> {
                @Override
                public ObjectChecks<T> check(final String description, final Function<T, Boolean> check) {
                    checks.check(prefix + " " + description).skip();
                    return this;
                }

                @Override
                public ObjectChecks<T> check(final String description, final Function<T, Boolean> check, final WhenFalse whenFalse) {
                    checks.check(prefix + " " + description).skip();
                    return null;
                }

                @Override
                public <R> ObjectChecks<R> map(final String description, final Function<? super T, ? extends R> mapper) {
                    return new Ignore<>();
                }

                public <R> ObjectChecks<R> map(final Function<? super T, ? extends R> mapper) {
                    return new Ignore<>();
                }

                @Override
                public ObjectChecks<T> or(final String description, final T object) {
                    return new ObjectChecksImpl<>(checks, object, description);
                }

                @Override
                public ObjectChecks<T> or(final String description, final Supplier<T> supplier) {
                    return new ObjectChecksImpl<>(checks, supplier.get(), description);
                }

                @Override
                public boolean result() {
                    return false;
                }

                @Override
                public <X extends Throwable> T getOrThrow(final Supplier<? extends X> exceptionSupplier) throws X {
                    throw exceptionSupplier.get();
                }

                @Override
                public <X extends Throwable> ObjectChecks<T> orThrow(final Supplier<? extends X> exceptionSupplier) throws X {
                    throw exceptionSupplier.get();
                }
            }

            public class Ignore<T> implements ObjectChecks<T> {
                @Override
                public ObjectChecks<T> check(final String description, final Function<T, Boolean> check) {
                    return this;
                }

                @Override
                public ObjectChecks<T> check(final String description, final Function<T, Boolean> check, final WhenFalse whenFalse) {
                    return this;
                }

                @Override
                public <R> ObjectChecks<R> map(final String description, final Function<? super T, ? extends R> mapper) {
                    return new Ignore<>();
                }

                public <R> ObjectChecks<R> map(final Function<? super T, ? extends R> mapper) {
                    return new Ignore<>();
                }

                @Override
                public ObjectChecks<T> or(final String description, final T object) {
                    return new ObjectChecksImpl<>(checks, object, description);
                }

                @Override
                public ObjectChecks<T> or(final String description, final Supplier<T> supplier) {
                    return new ObjectChecksImpl<>(checks, supplier.get(), description);
                }

                @Override
                public boolean result() {
                    return false;
                }

                @Override
                public <X extends Throwable> T getOrThrow(final Supplier<? extends X> exceptionSupplier) throws X {
                    throw exceptionSupplier.get();
                }

                @Override
                public <X extends Throwable> ObjectChecks<T> orThrow(final Supplier<? extends X> exceptionSupplier) throws X {
                    throw exceptionSupplier.get();
                }
            }
        }

    }
}
