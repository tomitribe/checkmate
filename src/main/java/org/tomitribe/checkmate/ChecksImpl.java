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

import java.util.function.Function;
import java.util.function.Supplier;

import static org.tomitribe.checkmate.Checks.WhenFalse.FAIL;
import static org.tomitribe.checkmate.Checks.WhenFalse.WARN;

class ChecksImpl implements Checks {
    private final CheckLogger listener;

    public ChecksImpl(final CheckLogger listener) {
        this.listener = listener;
    }

    @Override
    public Check check(final String item) {
        return listener.log(item);
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
            return org.tomitribe.checkmate.ChecksImpl.this.check(item);
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
            return new ObjectChecksImpl(org.tomitribe.checkmate.ChecksImpl.this, null, description).new Ignore();
        }

        @Override
        public <T> ObjectChecks<T> object(final String description, final Supplier<T> supplier) {
            return new ObjectChecksImpl(org.tomitribe.checkmate.ChecksImpl.this, null, description).new Ignore();
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
                    return new ObjectChecksImpl.Skip();
                }
            } catch (Throwable t) {
                check1.error(t.getClass().getSimpleName() + ": " + t.getMessage());
                return new ObjectChecksImpl.Skip();
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

         class Skip implements ObjectChecks<T> {
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
                return new ObjectChecksImpl.Ignore();
            }

            public <R> ObjectChecks<R> map(final Function<? super T, ? extends R> mapper) {
                return new ObjectChecksImpl.Ignore();
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

         class Ignore<T> implements ObjectChecks<T> {
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
                return new ObjectChecksImpl.Ignore();
            }

            public <R> ObjectChecks<R> map(final Function<? super T, ? extends R> mapper) {
                return new ObjectChecksImpl.Ignore();
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
