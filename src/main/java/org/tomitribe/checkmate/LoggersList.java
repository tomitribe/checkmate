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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LoggersList implements CheckLogger {
    final List<CheckLogger> loggers = new ArrayList<>();

    public LoggersList(final List<CheckLogger> loggers) {
        this.loggers.addAll(loggers);
    }

    @Override
    public Check log(final String name) {
        final List<Check> list = loggers.stream()
                .map(logger -> logger.log(name))
                .collect(Collectors.toList());

        return new Check() {
            @Override
            public void error(final String reason) {
                list.forEach(check -> check.error(reason));
            }

            @Override
            public void pass() {
                list.forEach(Check::pass);
            }

            @Override
            public void fail() {
                list.forEach(Check::fail);
            }

            @Override
            public void warn() {
                list.forEach(Check::warn);
            }

            @Override
            public void skip() {
                list.forEach(Check::skip);
            }

            @Override
            public void fail(final String reason) {
                list.forEach(check -> check.fail(reason));
            }

            @Override
            public void warn(final String reason) {
                list.forEach(check -> check.warn(reason));
            }
        };
    }
}
