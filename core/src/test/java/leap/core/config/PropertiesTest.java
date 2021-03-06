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
package leap.core.config;

import leap.core.AppConfig;
import leap.core.AppContext;
import leap.core.junit.AppTestBase;
import org.junit.Test;

public class PropertiesTest extends AppTestBase {

    @Test
    public void testProperties() {
        assertEquals("sys.val1", config.getProperty("sys.prop1"));
        assertEquals("imp.val1", config.getProperty("imp.prop1"));
        assertEquals("test.val1",config.getProperty("test.prop1"));
        assertEquals("test.val2",config.getProperty("test.prop2"));
        assertEquals("test.val3",config.getProperty("test.prop3"));
    }

    @Test
    public void testPropertiesPrefix() {
        AppConfig config = AppContext.config();
        assertEquals("test1.val1",config.getProperty("test1.prop1"));
        assertEquals("test2.val1",config.getProperty("test2_prop1"));
    }

    @Test
    public void testPropertiesFile() {
        assertEquals("a", config.getProperty("props.prop1"));
        assertEquals("b", config.getProperty("props.prop2"));
    }

}