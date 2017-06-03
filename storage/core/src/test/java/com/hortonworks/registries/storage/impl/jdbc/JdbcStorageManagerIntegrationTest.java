/**
 * Copyright 2016 Hortonworks.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.hortonworks.registries.storage.impl.jdbc;

import com.google.common.cache.CacheBuilder;
import com.hortonworks.registries.common.test.IntegrationTest;
import com.hortonworks.registries.storage.AbstractStoreManagerTest;
import com.hortonworks.registries.storage.StorageManager;
import com.hortonworks.registries.storage.exception.AlreadyExistsException;
import com.hortonworks.registries.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.registries.storage.impl.jdbc.connection.ConnectionBuilder;
import com.hortonworks.registries.storage.impl.jdbc.provider.mysql.factory.MySqlExecutor;
import com.hortonworks.registries.storage.impl.jdbc.provider.mysql.query.MySqlQueryUtils;
import com.hortonworks.registries.storage.impl.jdbc.provider.sql.factory.QueryExecutor;
import com.hortonworks.registries.storage.impl.jdbc.provider.sql.statement.PreparedStatementBuilder;
import com.hortonworks.registries.storage.Storable;
import com.hortonworks.registries.storage.StorableTest;
import com.hortonworks.registries.storage.impl.jdbc.provider.sql.query.SqlQuery;
import com.hortonworks.registries.storage.search.OrderBy;
import com.hortonworks.registries.storage.search.SearchQuery;
import com.hortonworks.registries.storage.search.WhereClause;
import org.h2.tools.RunScript;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

@Category(IntegrationTest.class)
public abstract class JdbcStorageManagerIntegrationTest extends AbstractStoreManagerTest {
    protected static StorageManager jdbcStorageManager;
    protected static Database database;
    protected static ConnectionBuilder connectionBuilder;

    protected enum Database {MYSQL, H2, PHOENIX}

    // ===== Tests Setup ====
    // Class level initialization is done in the implementing subclasses

    @Before
    public void setUp() throws Exception {
        createTables();
    }

    @After
    public void tearDown() throws Exception {
        jdbcStorageManager.cleanup();
        dropTables();
    }

    @Override
    protected StorageManager getStorageManager() {
        return jdbcStorageManager;
    }


    // =============== TEST METHODS ===============

    @Test(expected = Exception.class)
    public void testAdd_UnequalExistingStorable_AlreadyExistsException() {
        for (StorableTest test : storableTests) {
            Storable storable1 = test.getStorableList().get(0);
            Storable storable2 = test.getStorableList().get(1);
            Assert.assertEquals(storable1.getStorableKey(), storable2.getStorableKey());
            Assert.assertNotEquals(storable1, storable2);
            getStorageManager().add(storable1);
            getStorageManager().add(storable2);     // should throw exception
        }
    }

    @Test
    public void testList_EmptyDb_EmptyCollection() {
        for (StorableTest test : storableTests) {
            Collection<Storable> found = getStorageManager().list(test.getStorableList().get(0).getStorableKey().getNameSpace());
            Assert.assertNotNull(found);
            Assert.assertTrue(found.isEmpty());
        }
    }

    @Test
    public void testSearchQueryApi() {
        for (StorableTest storableTest : storableTests) {
            storableTest.addAllToStorage();
            String nameSpace = storableTest.getNameSpace();
            SearchQuery searchQuery = SearchQuery
                    .searchFrom(nameSpace)
                    .where(WhereClause
                                   .begin()
                                   .contains("name", "info")
                                   .and()
                                   .gt("id", 1L)
                                   .combine())
                    .orderBy(OrderBy.asc("name"));
            Collection<Storable> storablesWithIdGt1 = getStorageManager().search(searchQuery);
            System.out.println("storablesWithIdGt1 = " + storablesWithIdGt1);
            for (Storable storable : storablesWithIdGt1) {
                Assert.assertTrue(storable.getId() > 1L);
            }

            Collection<Storable> allStorables = getStorageManager().list(searchQuery.getNameSpace());
            System.out.println("list = " + allStorables);

            Assert.assertEquals(allStorables.size()-1, storablesWithIdGt1.size());
        }
    }

    @Test
    public void testFindOrderBy() {
        super.testFindOrderBy();
    }
    
    @Test
    @Ignore
    public void testNextId_AutoincrementColumn_IdPlusOne() throws Exception {

        for (StorableTest test : storableTests) {
                doTestNextId_AutoincrementColumn_IdPlusOne(test);
        }
    }

    // ============= Inner classes that handle the initialization steps required for the Storable entity to be tested =================


    protected static Connection getConnection() {
        Connection connection = connectionBuilder.getConnection();
        log.debug("Opened connection {}", connection);
        return connection;
    }

    protected void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                log.debug("Closed connection {}", connection);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to close connection", e);
            }
        }
    }

    // ========= Private helper methods  ==========

    private void createTables() throws SQLException, IOException {
        runScript("mysql/create_tables.sql");
    }

    private void dropTables() throws SQLException, IOException {
        runScript("mysql/drop_tables.sql");
    }

    private void runScript(String fileName) throws SQLException, IOException {
        Connection connection = null;
        try {
            connection = getConnection();
            RunScript.execute(connection, load(fileName));
        } finally {
            // We need to close the connection because H2 DB running in memory only allows one connection at a time
            closeConnection(connection);
        }
    }

    private Reader load(String fileName) throws IOException {
        return new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(fileName));
    }

    public abstract JdbcStorageManager createJdbcStorageManager(QueryExecutor queryExecutor);

}
