/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package leap.core.transaction;

import leap.core.annotation.Inject;
import leap.core.junit.AppTestBase;
import leap.lang.Try;
import org.junit.Test;
import tested.transaction.TransactionalBean;

public class TransactionTest extends AppTestBase {

    private @Inject TransactionalBean bean;

    @Test
    public void testInstrument() throws Exception {
        System.out.println("");

        bean.doSuccess();
        bean.doSuccessTryFinally();
        bean.doSuccessWithReturnValue();

        Try.catchAll(() -> bean.doFailure());
        Try.catchAll(() -> bean.doFailureTryFinally());
        Try.catchAll(() -> bean.doFailureNested());
        Try.catchAll(() -> bean.doFailureThrowable());
    }

}
