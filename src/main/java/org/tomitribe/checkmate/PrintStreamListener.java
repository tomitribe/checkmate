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

public class PrintStreamListener implements Checks.Listener {

    private final PrintStream out;
    private final int column;

    public PrintStreamListener(final PrintStream out, final int column) {
        this.column = column;
        this.out = out;
    }

    @Override
    public Check check(final String item) {
        out.print(item);

        int i = item.length();
        while (++i < column) {
            if (i % 2 == 0) {
                out.print(".");
            } else {
                out.print(" ");
            }
        }

        if (i % 2 == 1) {
            out.print(" ");
        }

        return new Check() {
            @Override
            public void pass() {
                out.println("PASS");
            }

            @Override
            public void fail() {
                out.println("FAIL");
            }

            @Override
            public void warn() {
                out.println("WARN");
            }

            @Override
            public void skip() {
                out.println("SKIP");
            }

            @Override
            public void fail(final String reason) {
                out.println("FAIL  " + reason);
            }

            @Override
            public void warn(final String reason) {
                out.println("WARN  " + reason);
            }

            @Override
            public void error(final String reason) {
                out.println("ERROR  " + reason);
            }
        };
    }
}
