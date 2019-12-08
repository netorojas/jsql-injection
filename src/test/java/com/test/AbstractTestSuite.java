package com.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.h2.tools.Server;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.SpringApplication;

import com.jsql.model.accessible.DataAccess;
import com.jsql.model.bean.database.Column;
import com.jsql.model.bean.database.Database;
import com.jsql.model.bean.database.Table;
import com.jsql.model.exception.InjectionFailureException;
import com.jsql.model.exception.JSqlException;
import com.test.util.Retry;

import spring.Application; 

public abstract class AbstractTestSuite {
	
	static {
		// Use Timeout fix in Model
		PropertyConfigurator.configure("src/test/resources/logger/log4j.stdout.properties");
		jcifs.Config.registerSmbURLHandler();

        try {
            Server.createTcpServer().start();
        } catch (SQLException e) {
            e.printStackTrace();
        }
	}
	
    /**
     * Using default log4j.properties from root /
     */
    protected static final Logger LOGGER = Logger.getRootLogger();

    public static final String HOSTNAME = "localhost";
    
    private List<String> databaseToFind = new ArrayList<>();
    private List<String> tableToFind = new ArrayList<>();
    private List<String> columnToFind = new ArrayList<>();
    private List<String> valueToFind = new ArrayList<>();
    
    protected String jdbcURL;
    protected String jdbcUser;
    protected String jdbcPass;
    
    protected String jdbcQueryForDatabaseNames;
    protected String jdbcQueryForTableNames;
    protected String jdbcQueryForColumnNames;
    protected String jdbcQueryForValues;
    
    protected String jdbcColumnForDatabaseName;
    protected String jdbcColumnForTableName;
    protected String jdbcColumnForColumnName;
    
    protected String jsqlDatabaseName;
    protected String jsqlTableName;
    protected String jsqlColumnName;
    
    @Rule
    public Retry retry = new Retry(3);
    
    private static AtomicBoolean setUpIsDone = new AtomicBoolean(false);
    
    @BeforeClass
    public synchronized static void runSpringApplication() throws Exception {
        
        if (setUpIsDone.compareAndSet(false, true)) {
            LOGGER.info("@BeforeClass: setting up Hibernate and Spring, please wait...");
            Application.init(); 
            SpringApplication.run(Application.class, new String[] {});
        } else {
            LOGGER.info("@BeforeClass: Hibernate and Spring setup already done");
        }
    }

    public static void initialize() throws Exception {
        LOGGER.warn(
            "AbstractTestSuite and ConcreteTestSuite are for initialization purpose. "
            + "Run test suite or unit test instead."
        );
        throw new InjectionFailureException();
    }

    public void requestJdbc() {
        try (Connection conn = DriverManager.getConnection(this.jdbcURL, this.jdbcUser, this.jdbcPass)) {
            Statement stmt = conn.createStatement();
            ResultSet res = stmt.executeQuery(this.jdbcQueryForDatabaseNames);
            while (res.next()) {
                String dbName = res.getString(this.jdbcColumnForDatabaseName);
                this.databaseToFind.add(dbName);
            }
            res.close();
            stmt.close();
            
            stmt = conn.createStatement();
            res = stmt.executeQuery(this.jdbcQueryForTableNames);
            while (res.next()) {
                String tableName = res.getString(this.jdbcColumnForTableName);
                this.tableToFind.add(tableName);
            }
            res.close();
            stmt.close();
            
            stmt = conn.createStatement();
            res = stmt.executeQuery(this.jdbcQueryForColumnNames);
            while (res.next()) {
                String colName = res.getString(this.jdbcColumnForColumnName);
                this.columnToFind.add(colName);
            }
            res.close();
            stmt.close();

            stmt = conn.createStatement();
            res = stmt.executeQuery(this.jdbcQueryForValues);
            while (res.next()) {
                String value = res.getString(this.jsqlColumnName);
                this.valueToFind.add(value);
            }
            res.close();
            stmt.close();
        } catch (Exception e) {
            LOGGER.warn(e);
        }
    }

    @Test
    public void listDatabases() throws JSqlException {
        Set<Object> set1 = new HashSet<>();
        Set<Object> set2 = new HashSet<>();
        
        try {
            List<Database> dbs = DataAccess.listDatabases();
            List<String> databasesFound = new ArrayList<>();
            for (Database d: dbs) {
                databasesFound.add(d.toString());
            }

            set1.addAll(databasesFound);
            set2.addAll(this.databaseToFind);

            LOGGER.info("ListDatabases: found "+ set1 +" to find "+ set2);

            Assert.assertTrue(!set1.isEmpty() && !set2.isEmpty() && set1.containsAll(set2));
            
        } catch (AssertionError e) {
            Set<Object> tmp = new TreeSet<>();
            for (Object x: set1) {
                if (!set2.contains(x)) {
                    tmp.add(x);
                }
            }
            for (Object x: set2) {
                if (!set1.contains(x)) {
                    tmp.add(x);
                }
            }
            throw new AssertionError("Error listDatabases: "+ tmp +"\n"+ e);
        }
    }

    @Test
    public void listTables() throws JSqlException {
        Set<Object> set1 = new HashSet<>();
        Set<Object> set2 = new HashSet<>();

        try {
            List<Table> ts = DataAccess.listTables(new Database(this.jsqlDatabaseName, "0"));
            List<String> tablesFound = new ArrayList<>();
            for (Table t: ts) {
                tablesFound.add(t.toString());
            }

            set1.addAll(tablesFound);
            set2.addAll(this.tableToFind);

            LOGGER.info("listTables: found "+ set1 +" to find "+ set2);
            Assert.assertTrue(!set1.isEmpty() && !set2.isEmpty() && set1.equals(set2));
            
        } catch (AssertionError e) {
            Set<Object> tmp = new TreeSet<>();
            for (Object x: set1) {
                if (!set2.contains(x)) {
                    tmp.add(x);
                }
            }
            for (Object x: set2) {
                if (!set1.contains(x)) {
                    tmp.add(x);
                }
            }
            throw new AssertionError("Error listTables: "+ tmp +"\n"+ e);
        }
    }

    @Test
    public void listColumns() throws JSqlException {
        Set<Object> set1 = new HashSet<>();
        Set<Object> set2 = new HashSet<>();

        try {
            List<Column> cs = DataAccess.listColumns(
                new Table(this.jsqlTableName, "0",
                    new Database(this.jsqlDatabaseName, "0")
                )
            );
            List<String> columnsFound = new ArrayList<>();
            for (Column c: cs) {
                columnsFound.add(c.toString());
            }

            set1.addAll(columnsFound);
            set2.addAll(this.columnToFind);

            LOGGER.info("listColumns: found "+ set1 +" to find "+ set2);
            Assert.assertTrue(!set1.isEmpty() && !set2.isEmpty() && set1.equals(set2));
            
        } catch (AssertionError e) {
            Set<Object> tmp = new TreeSet<>();
            for (Object x: set1) {
                if (!set2.contains(x)) {
                    tmp.add(x);
                }
            }
            for (Object x: set2) {
                if (!set1.contains(x)) {
                    tmp.add(x);
                }
            }
            throw new AssertionError("Error listColumns: "+ tmp +"\n"+ e);
        }
    }

    @Test
    public void listValues() throws JSqlException {
        Set<Object> set1 = new TreeSet<>();
        Set<Object> set2 = new TreeSet<>();

        try {
            String[][] vs = DataAccess.listValues(Arrays.asList(
                new Column(this.jsqlColumnName,
                    new Table(this.jsqlTableName, "0",
                        new Database(this.jsqlDatabaseName, "0")
                    )
                )
            ));
            List<String> valuesFound = new ArrayList<>();
            for (String[] v: vs) {
                valuesFound.add(v[2].replaceAll("\r\n", "\n"));
            }

            set1.addAll(valuesFound);
            set2.addAll(this.valueToFind);

            LOGGER.info(
                "listValues: found "+
                set1.toString()
                    .replaceAll("\n", "[n]")
                    .replaceAll("\r", "[r]") +
                " to find "+
                set2.toString()
                    .replaceAll("\n", "[n]")
                    .replaceAll("\r", "[r]") 
            );
            Assert.assertTrue(!set1.isEmpty() && !set2.isEmpty() && set1.equals(set2));
            
        } catch (AssertionError e) {
            Set<Object> tmp = new TreeSet<>();
            for (Object x: set1) {
                if (!set2.contains(x)) {
                    tmp.add(x);
                }
            }
            for (Object x: set2) {
                if (!set1.contains(x)) {
                    tmp.add(x);
                }
            }
            throw new AssertionError("Error listValues: "+ tmp +"\n"+ e);
        }
    }
    
}
