/*
 * Copyright 2015 the original author or authors.
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
package leap.db.cp;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import leap.core.junit.AppTestBase;
import leap.db.mock.MockConnection;
import leap.db.mock.MockDataSource;
import leap.db.mock.MockStatement;

public abstract class ConnPoolTestBase extends AppTestBase {
	
	PooledDataSource poolds;
	MockDataSource	 mockds;
	
	@Override
    protected void setUp() throws Exception {
		initDefaultDataSource();
    }
	
	@Override
    protected void tearDown() throws Exception {
		if(null != poolds && !poolds.isClose()) {
			poolds.close();
		}
	}

	protected void initDefaultDataSource() {
		if(null != poolds && !poolds.isClose()) {
			poolds.close();
		}
		mockds = new MockDataSource();
		poolds = new PooledDataSource(mockds);
	}
	
	ProxyConnection getConnection() throws SQLException {
		return (ProxyConnection)poolds.getConnection();
	}
	
	protected PooledDataSource createDefaultDataSource() {
		return new PooledDataSource(new MockDataSource());
	}
	
	protected static PooledConnection proxy(Connection conn) {
		return (PooledConnection)conn;
	}
	
	protected static ProxyStatement proxy(Statement statement) {
		return (ProxyStatement)statement;
	}
	
	protected static ProxyPreparedStatement proxy(PreparedStatement ps) {
		return (ProxyPreparedStatement)ps;
	}
	
	protected static ProxyCallableStatement proxy(CallableStatement cs) {
		return (ProxyCallableStatement)cs;
	}
	
	protected static MockConnection real(Connection conn) {
		return (MockConnection)((ProxyConnection)conn).getReal();
	}
	
	protected static MockStatement real(Statement stmt) {
		return (MockStatement)proxy(stmt).getReal();
	}
	
}
