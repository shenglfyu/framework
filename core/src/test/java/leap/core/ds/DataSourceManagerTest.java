/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package leap.core.ds;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Test;

import leap.core.junit.AppTestBase;
import leap.lang.Exceptions;
import leap.lang.exception.ObjectNotFoundException;

public class DataSourceManagerTest extends AppTestBase {
	
	private DataSourceManager dsm;
	
	@Override
    protected void setUp() throws Exception {
		dsm = factory.getBean(DataSourceManager.class);
	}

	@Test
	public void testDefaultConfiguration() {
		assertNull(dsm.tryGetDefaultDataSource());

		try {
	        dsm.getDefaultDataSource();
	        fail("Should throw exception");
        } catch (ObjectNotFoundException e) {

        }
		
		assertTrue(dsm.getAllDataSources().isEmpty());
	}
	
	@Test
	public void testCreateDataSource() {
		DataSourceConfig p = 
				DataSourceConfig.createBuilder()
								    .setDriverClassName("org.h2.Driver")
								    .setUrl("jdbc:h2:mem:test")
								    .build();
		
		DataSource ds = null;
		try {
			ds = dsm.createDataSource(p);
			assertNotNull(ds);
			
	        try(Connection connection = ds.getConnection()){
	        	
	        }
        } catch (SQLException e) {
        	throw Exceptions.wrap(e);
        }
		
		dsm.destroyDataSource(ds);
	}

}
