/*
 * Copyright 2016 Bloomberg L.P.
 *
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
package com.facebook.presto.accumulo.integration.tests;

import com.facebook.presto.accumulo.AccumuloClient;
import com.facebook.presto.accumulo.AccumuloConnectorId;
import com.facebook.presto.accumulo.conf.AccumuloConfig;
import com.facebook.presto.accumulo.conf.AccumuloTableProperties;
import com.facebook.presto.accumulo.metadata.AccumuloTable;
import com.facebook.presto.accumulo.model.AccumuloColumnHandle;
import com.facebook.presto.accumulo.model.Row;
import com.facebook.presto.accumulo.model.RowSchema;
import com.facebook.presto.accumulo.serializers.AccumuloRowSerializer;
import com.facebook.presto.spi.ColumnMetadata;
import com.facebook.presto.spi.ConnectorTableMetadata;
import com.facebook.presto.spi.SchemaTableName;
import com.facebook.presto.spi.type.BigintType;
import com.facebook.presto.spi.type.BooleanType;
import com.facebook.presto.spi.type.DateType;
import com.facebook.presto.spi.type.DoubleType;
import com.facebook.presto.spi.type.StandardTypes;
import com.facebook.presto.spi.type.TimeType;
import com.facebook.presto.spi.type.TimestampType;
import com.facebook.presto.spi.type.Type;
import com.facebook.presto.spi.type.TypeSignature;
import com.facebook.presto.spi.type.VarbinaryType;
import com.facebook.presto.spi.type.VarcharType;
import com.facebook.presto.type.ArrayType;
import com.facebook.presto.type.TypeRegistry;
import io.airlift.log.Logger;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.hadoop.io.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

public class QueryDriver
{
    private static final Logger LOG = Logger.get(QueryDriver.class);
    public static final String JDBC_DRIVER = "com.facebook.presto.jdbc.PrestoDriver";
    public static final String SCHEME = "jdbc:presto://";
    public static final String CATALOG = "accumulo";

    private boolean initialized = false;
    private boolean orderMatters = false;
    private int port = 8080;
    private String host = "localhost";
    private String schema;
    private String query;
    private String tableName;
    private String rowIdName = "recordkey";
    private List<Row> inputs = new ArrayList<>();
    private List<Row> expectedOutputs = new ArrayList<>();
    private SortedSet<Text> splits = new TreeSet<>();
    private RowSchema inputSchema;
    private RowSchema outputSchema;
    private AccumuloRowSerializer serializer;
    private AccumuloClient client;

    static {
        try {
            Class.forName(JDBC_DRIVER);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public QueryDriver(AccumuloConfig config)
            throws AccumuloException, AccumuloSecurityException
    {
        this(config, AccumuloRowSerializer.getDefault());
    }

    public QueryDriver(AccumuloConfig config, AccumuloRowSerializer serializer)
            throws AccumuloException, AccumuloSecurityException
    {
        this.serializer = serializer;
        client = new AccumuloClient(new AccumuloConnectorId(CATALOG), config);
    }

    public QueryDriver withHost(String host)
    {
        this.host = host;
        return this;
    }

    public QueryDriver withPort(int port)
    {
        this.port = port;
        return this;
    }

    public QueryDriver withSchema(String schema)
    {
        this.schema = schema;
        return this;
    }

    public QueryDriver withQuery(String query)
    {
        this.query = query;
        return this;
    }

    public QueryDriver withInput(Row... rows)
    {
        for (Row r : rows) {
            this.inputs.add(r);
        }
        return this;
    }

    public QueryDriver withInput(List<Row> rows)
    {
        this.inputs.clear();
        this.inputs.addAll(rows);
        return this;
    }

    public QueryDriver withOutput(Row... rows)
    {
        for (Row r : rows) {
            this.expectedOutputs.add(r);
        }
        return this;
    }

    public QueryDriver withOutput(List<Row> rows)
    {
        this.expectedOutputs.clear();
        this.expectedOutputs.addAll(rows);
        return this;
    }

    public QueryDriver withTable(String tableName)
    {
        this.tableName = tableName;
        return this;
    }

    public QueryDriver withInputSchema(RowSchema schema)
    {
        this.inputSchema = schema;
        return this;
    }

    public QueryDriver withInputFile(File file)
            throws IOException
    {
        if (this.inputSchema == null) {
            throw new RuntimeException("Input schema must be set prior to using this method");
        }

        this.withInput(loadRowsFromFile(inputSchema, file));

        return this;
    }

    public QueryDriver withRowIdColumnName(String name)
            throws IOException
    {
        this.rowIdName = name;
        return this;
    }

    public QueryDriver withSplits(String... splits)
    {
        for (String s : splits) {
            this.splits.add(new Text(s));
        }
        return this;
    }

    public QueryDriver withSplits(List<String> splits)
    {
        this.splits.clear();
        for (String s : splits) {
            this.splits.add(new Text(s));
        }
        return this;
    }

    public QueryDriver withOutputSchema(RowSchema schema)
    {
        this.outputSchema = schema;
        return this;
    }

    public QueryDriver withOutputFile(File file)
            throws IOException
    {
        if (this.outputSchema == null) {
            throw new RuntimeException("Input schema must be set prior to using this method");
        }

        this.expectedOutputs.clear();
        this.withOutput(loadRowsFromFile(outputSchema, file));

        return this;
    }

    public static List<Row> loadRowsFromFile(RowSchema rSchema, File file)
            throws IOException
    {
        List<Row> list = new ArrayList<>();
        int eLength = rSchema.getLength();

        // auto-detect gzip compression based on filename
        InputStream dataStream = file.getName().endsWith(".gz")
                ? new GZIPInputStream(new FileInputStream(file)) : new FileInputStream(file);

        try (BufferedReader rdr = new BufferedReader(new InputStreamReader(dataStream))) {
            String line;
            while ((line = rdr.readLine()) != null) {
                String[] tokens = line.split("\\|");

                if (tokens.length != eLength) {
                    throw new RuntimeException(
                            String.format("Record in file has %d tokens, expected %d, %s",
                                    tokens.length, eLength, line));
                }

                Row r = Row.newRow();
                list.add(r);
                for (int i = 0; i < tokens.length; ++i) {
                    switch (rSchema.getColumn(i).getType().getDisplayName()) {
                        case StandardTypes.BIGINT:
                            r.addField(Long.parseLong(tokens[i]), BigintType.BIGINT);
                            break;
                        case StandardTypes.BOOLEAN:
                            r.addField(Boolean.parseBoolean(tokens[i]), BooleanType.BOOLEAN);
                            break;
                        case StandardTypes.DATE:
                            r.addField(new Date(Long.parseLong(tokens[i]) * 1000), DateType.DATE);
                            break;
                        case StandardTypes.DOUBLE:
                            r.addField(Double.parseDouble(tokens[i]), DoubleType.DOUBLE);
                            break;
                        case StandardTypes.TIME:
                            r.addField(new Time(Long.parseLong(tokens[i]) * 1000), TimeType.TIME);
                            break;
                        case StandardTypes.TIMESTAMP:
                            r.addField(new Timestamp(Long.parseLong(tokens[i]) * 1000),
                                    TimestampType.TIMESTAMP);
                            break;
                        case StandardTypes.VARBINARY:
                            r.addField(tokens[i].getBytes(), VarbinaryType.VARBINARY);
                            break;
                        case StandardTypes.VARCHAR:
                            r.addField(tokens[i], VarcharType.VARCHAR);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        return list;
    }

    public QueryDriver orderMatters()
    {
        orderMatters = true;
        return this;
    }

    public List<Row> run()
            throws Exception
    {
        try {
            if (!initialized) {
                initialize();
            }
            return execQuery();
        }
        finally {
            inputs.clear();
            expectedOutputs.clear();
        }
    }

    public void runTest()
            throws Exception
    {
        List<Row> eOutputs = new ArrayList<Row>(expectedOutputs);
        if (!compareOutput(run(), eOutputs, orderMatters)) {
            throw new Exception("Output does not match, see log");
        }
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public String getSchema()
    {
        return schema;
    }

    public String getQuery()
    {
        return query;
    }

    public String getAccumuloTable()
    {
        return tableName;
    }

    public void initialize()
            throws Exception
    {
        createTable();
        pushInput();
        initialized = true;
    }

    public void cleanup()
            throws Exception
    {
        if (client.getTable(new SchemaTableName(getSchema(), getAccumuloTable())) == null) {
            return;
        }

        String rowIdName = "recordkey";

        for (AccumuloColumnHandle ach : inputSchema.getColumns()) {
            if (ach.getFamily() == null) {
                rowIdName = ach.getName();
            }
        }

        AccumuloTable table =
                new AccumuloTable(getSchema(), getAccumuloTable(), inputSchema.getColumns(),
                        rowIdName, false, this.serializer.getClass().getName(), null);
        client.dropTable(table);

        this.withHost("localhost").withPort(8080).withInputSchema(null).withSchema(null)
                .withQuery(null).withTable(null);

        inputs.clear();
        expectedOutputs.clear();
        orderMatters = false;
        initialized = false;
        rowIdName = "recordkey";
    }

    /**
     * Compares output of the actual data to the configured expected output
     *
     * @param actual The list of results from the query
     * @param expected The list of expected results
     * @return True if the output matches, false otherwise
     */
    public static boolean compareOutput(List<Row> actual, List<Row> expected, boolean orderMatters)
    {
        if (orderMatters) {
            return validateWithOrder(actual, expected);
        }
        else {
            return validateWithoutOrder(actual, expected);
        }
    }

    protected String getDbUrl()
    {
        return String.format("%s%s:%d/%s/%s", SCHEME, getHost(), getPort(), CATALOG, getSchema());
    }

    protected void createTable()
            throws AccumuloException, AccumuloSecurityException, TableExistsException,
            TableNotFoundException
    {
        StringBuilder indexCols = new StringBuilder();
        StringBuilder colMapping = new StringBuilder();
        List<ColumnMetadata> cms = new ArrayList<>();
        String rowIdName = "recordkey";
        for (AccumuloColumnHandle col : inputSchema.getColumns()) {
            cms.add(col.getColumnMetadata());
            colMapping.append(col.getName()).append(":").append(col.getFamily()).append(":")
                    .append(col.getQualifier()).append(",");
            if (col.isIndexed()) {
                indexCols.append(col.getName()).append(",");
            }
            if (col.getFamily() == null) {
                rowIdName = col.getName();
            }
        }

        if (indexCols.length() > 0) {
            indexCols.deleteCharAt(indexCols.length() - 1);
        }

        colMapping.deleteCharAt(colMapping.length() - 1);

        Map<String, Object> props = new HashMap<>();
        props.put(AccumuloTableProperties.COLUMN_MAPPING, colMapping.toString());
        props.put(AccumuloTableProperties.EXTERNAL, false);
        props.put(AccumuloTableProperties.INDEX_COLUMNS, indexCols.toString());
        props.put(AccumuloTableProperties.ROW_ID, rowIdName);
        props.put(AccumuloTableProperties.SERIALIZER, this.serializer.getClass().getName());

        ConnectorTableMetadata ctm = new ConnectorTableMetadata(
                new SchemaTableName(getSchema(), getAccumuloTable()), cms, props);

        client.createTable(ctm);
    }

    protected void pushInput()
            throws Exception
    {
        LOG.info("Connecting to database...");
        Properties props = new Properties();
        props.setProperty("user", "root");
        Connection conn = DriverManager.getConnection(this.getDbUrl(), props);

        String insertInto =
                "INSERT INTO " + (this.getSchema().equals("default") ? this.getAccumuloTable()
                        : this.getSchema() + '.' + this.getAccumuloTable()) + " VALUES ";
        StringBuilder bldr = new StringBuilder(insertInto);

        int i = 0;
        for (Row row : inputs) {
            bldr.append(row).append(',');

            if (++i % 1000 == 0) {
                bldr.deleteCharAt(bldr.length() - 1);
                Statement stmt = conn.createStatement();
                LOG.info("Executing " + (bldr.length() < 1024 ? bldr : bldr.substring(0, 1024)));
                int rows = stmt.executeUpdate(bldr.toString());
                LOG.info("Inserted " + rows + " rows");
                bldr = new StringBuilder(insertInto);
                i = 0;
            }
        }

        if (i > 0) {
            bldr.deleteCharAt(bldr.length() - 1);
            Statement stmt = conn.createStatement();
            LOG.info("Executing " + (bldr.length() < 1024 ? bldr : bldr.substring(0, 1024)));
            int rows = stmt.executeUpdate(bldr.toString());
            LOG.info("Inserted " + rows + " rows");
        }
    }

    protected List<Row> execQuery()
            throws SQLException
    {
        LOG.info("Connecting to database...");

        Properties props = new Properties();
        props.setProperty("user", "root");
        Connection conn = DriverManager.getConnection(this.getDbUrl(), props);

        LOG.info("Creating statement...");
        Statement stmt = conn.createStatement();

        LOG.info("Executing query...");
        long start = System.currentTimeMillis();
        ResultSet rs = stmt.executeQuery(this.getQuery());

        long numrows = 0;
        List<Row> outputRows = new ArrayList<>();
        while (rs.next()) {
            ++numrows;
            Row orow = new Row();
            outputRows.add(orow);
            for (int j = 1; j <= rs.getMetaData().getColumnCount(); ++j) {
                Type type = getType(rs, rs.getMetaData(), j);
                if (type == null) {
                    orow.addField(null, null);
                    continue;
                }
                if (com.facebook.presto.accumulo.Types.isArrayType(type)) {
                    Array array = rs.getArray(j);
                    Type elementType = getType(array.getBaseTypeName());
                    Object[] elements = (Object[]) array.getArray();
                    orow.addField(AccumuloRowSerializer.getBlockFromArray(elementType,
                            Arrays.asList(elements)), new ArrayType(elementType));
                }
                else if (com.facebook.presto.accumulo.Types.isMapType(type)) {
                    Map<?, ?> map = (Map<?, ?>) rs.getObject(j);
                    orow.addField(
                            rs.wasNull() ? null : AccumuloRowSerializer.getBlockFromMap(type, map),
                            type);
                }
                else {
                    switch (type.getDisplayName()) {
                        case StandardTypes.BIGINT: {
                            Long v = rs.getLong(j);
                            orow.addField(rs.wasNull() ? null : v, BigintType.BIGINT);
                            break;
                        }
                        case StandardTypes.BOOLEAN: {
                            Boolean v = rs.getBoolean(j);
                            orow.addField(rs.wasNull() ? null : v, BooleanType.BOOLEAN);
                            break;
                        }
                        case StandardTypes.DATE: {
                            Date v = rs.getDate(j);
                            orow.addField(rs.wasNull() ? null : v, DateType.DATE);
                            break;
                        }
                        case StandardTypes.DOUBLE: {
                            Double v = rs.getDouble(j);
                            orow.addField(rs.wasNull() ? null : v, DoubleType.DOUBLE);
                            break;
                        }
                        case StandardTypes.TIME: {
                            Time v = rs.getTime(j);
                            orow.addField(rs.wasNull() ? null : v, TimeType.TIME);
                            break;
                        }
                        case StandardTypes.TIMESTAMP: {
                            Timestamp v = rs.getTimestamp(j);
                            orow.addField(rs.wasNull() ? null : v, TimestampType.TIMESTAMP);
                            break;
                        }
                        case StandardTypes.VARBINARY: {
                            byte[] v = rs.getBytes(j);
                            orow.addField(rs.wasNull() ? null : v, VarbinaryType.VARBINARY);
                            break;
                        }
                        case StandardTypes.VARCHAR: {
                            String v = rs.getString(j);
                            orow.addField(rs.wasNull() ? null : v, VarcharType.VARCHAR);
                            break;
                        }
                        default:
                            throw new RuntimeException(
                                    "Unknown SQL type " + rs.getMetaData().getColumnType(j));
                    }
                }
            }
        }

        rs.close();
        stmt.close();
        conn.close();

        long end = System.currentTimeMillis();
        LOG.info(String.format("Done.  Received %d rows in %d ms", numrows, end - start));
        return outputRows;
    }

    TypeRegistry typeManager = new TypeRegistry();

    private Type getType(ResultSet rs, ResultSetMetaData rsmd, int column)
            throws SQLException
    {
        switch (rsmd.getColumnType(column)) {
            case Types.ARRAY:
                Array ar = rs.getArray(column);
                if (ar != null) {
                    Type t = typeManager.getType(TypeSignature
                            .parseTypeSignature(rs.getArray(column).getBaseTypeName()));
                    return new ArrayType(t);
                }
                else {
                    return null;
                }
            case Types.BIGINT:
                return BigintType.BIGINT;
            case Types.BOOLEAN:
                return BooleanType.BOOLEAN;
            case Types.DATE:
                return DateType.DATE;
            case Types.DOUBLE:
                return DoubleType.DOUBLE;
            case Types.JAVA_OBJECT:
                return typeManager
                        .getType(TypeSignature.parseTypeSignature(rsmd.getColumnTypeName(column)));
            case Types.TIME:
                return TimeType.TIME;
            case Types.TIMESTAMP:
                return TimestampType.TIMESTAMP;
            case Types.LONGVARBINARY:
                return VarbinaryType.VARBINARY;
            case Types.LONGNVARCHAR:
                return VarcharType.VARCHAR;
            default:
                throw new RuntimeException("Unknown SQL type " + rsmd.getColumnType(column));
        }
    }

    private Type getType(String name)
            throws SQLException
    {
        return typeManager.getType(TypeSignature.parseTypeSignature(name));
    }

    protected static boolean validateWithoutOrder(final List<Row> actualOutputs,
            final List<Row> expectedOutputs)
    {
        Set<Integer> verifiedExpecteds = new HashSet<Integer>();
        Set<Integer> unverifiedOutputs = new HashSet<Integer>();
        for (int i = 0; i < actualOutputs.size(); i++) {
            Row output = actualOutputs.get(i);
            boolean found = false;
            for (int j = 0; j < expectedOutputs.size(); j++) {
                if (verifiedExpecteds.contains(j)) {
                    continue;
                }
                Row expected = expectedOutputs.get(j);
                if (expected.equals(output)) {
                    found = true;
                    verifiedExpecteds.add(j);
                    LOG.info(String.format("Matched expected output %s no %d at " + "position %d",
                            output, j, i));
                    break;
                }
            }
            if (!found) {
                unverifiedOutputs.add(i);
            }
        }

        boolean noErrors = true;
        for (int j = 0; j < expectedOutputs.size(); j++) {
            if (!verifiedExpecteds.contains(j)) {
                LOG.error("Missing expected output %s", expectedOutputs.get(j));
                noErrors = false;
            }
        }

        for (int i = 0; i < actualOutputs.size(); i++) {
            if (unverifiedOutputs.contains(i)) {
                LOG.error("Received unexpected output %s", actualOutputs.get(i));
                noErrors = false;
            }
        }

        return noErrors;
    }

    protected static boolean validateWithOrder(final List<Row> actualOutputs,
            final List<Row> expectedOutputs)
    {
        boolean noErrors = true;
        int i = 0;
        for (i = 0; i < Math.min(actualOutputs.size(), expectedOutputs.size()); i++) {
            Row output = actualOutputs.get(i);
            Row expected = expectedOutputs.get(i);
            if (expected.equals(output)) {
                LOG.info(String.format("Matched expected output %s at " + "position %d", expected,
                        i));
            }
            else {
                LOG.error("Missing expected output %s at position %d, got %s.", expected, i,
                        output);
                noErrors = false;
            }
        }

        for (int j = i; j < actualOutputs.size(); j++) {
            LOG.error("Received unexpected output %s at position %d.", actualOutputs.get(j), j);
            noErrors = false;
        }

        for (int j = i; j < expectedOutputs.size(); j++) {
            LOG.error("Missing expected output %s at position %d.", expectedOutputs.get(j), j);
            noErrors = false;
        }
        return noErrors;
    }
}