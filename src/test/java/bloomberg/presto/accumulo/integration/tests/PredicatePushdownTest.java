package bloomberg.presto.accumulo.integration.tests;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.facebook.presto.spi.type.BigintType;
import com.facebook.presto.spi.type.BooleanType;
import com.facebook.presto.spi.type.DateType;
import com.facebook.presto.spi.type.DoubleType;
import com.facebook.presto.spi.type.TimeType;
import com.facebook.presto.spi.type.TimestampType;
import com.facebook.presto.spi.type.Type;
import com.facebook.presto.spi.type.VarbinaryType;
import com.facebook.presto.spi.type.VarcharType;
import com.facebook.presto.type.ArrayType;
import com.facebook.presto.type.MapType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import bloomberg.presto.accumulo.AccumuloConfig;
import bloomberg.presto.accumulo.model.Row;
import bloomberg.presto.accumulo.model.RowSchema;
import bloomberg.presto.accumulo.serializers.AccumuloRowSerializer;

public class PredicatePushdownTest {

    public static final QueryDriver HARNESS;

    static {
        try {
            AccumuloConfig config = new AccumuloConfig();
            config.setInstance("default");
            config.setZooKeepers("localhost:2181");
            config.setUsername("root");
            config.setPassword("secret");
            HARNESS = new QueryDriver(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void cleanup() throws Exception {
        HARNESS.cleanup();
    }

    @Test
    public void testSelectBigIntWhereNull() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("age", "metadata", "age", BigintType.BIGINT)
                .addColumn("weight", "metadata", "weight", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT)
                .addField(new Long(60), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(null, BigintType.BIGINT)
                .addField(new Long(60), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT)
                .addField(new Long(60), BigintType.BIGINT);

        // no constraints on predicate pushdown
        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE age IS NULL")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectBigIntWhereNotNull() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("age", "metadata", "age", BigintType.BIGINT)
                .addColumn("weight", "metadata", "weight", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT)
                .addField(new Long(60), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(null, BigintType.BIGINT)
                .addField(new Long(60), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT)
                .addField(new Long(60), BigintType.BIGINT);

        // no constraints on predicate pushdown
        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE age IS NOT NULL")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r3).runTest();
    }

    @Test
    public void testSelectBigIntWhereNullOrEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("age", "metadata", "age", BigintType.BIGINT)
                .addColumn("weight", "metadata", "weight", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT)
                .addField(new Long(60), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(null, BigintType.BIGINT)
                .addField(new Long(60), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT)
                .addField(new Long(60), BigintType.BIGINT);

        // no constraints on predicate pushdown
        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE age = 0 OR age is NULL")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r2).runTest();
    }

    @Test
    public void testSelectBigIntLess() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Long(15), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE age < 15")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r1)
                .runTest();
    }

    @Test
    public void testSelectBigIntLessOrEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Long(15), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE age <= 15")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r2).runTest();
    }

    @Test
    public void testSelectBigIntEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Long(15), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE age = 15")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE age = 15 AND age IS NOT NULL")
                .withInputSchema(schema).withInput(r1).withInput(r2)
                .withInput(r3).withOutput(r2).runTest();

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE age = 15 OR age IS NULL")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectBigIntGreater() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Long(15), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT);
        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE age > 15")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r3)
                .runTest();
    }

    @Test
    public void testSelectBigIntGreaterOrEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Long(15), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE age >= 15")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r2, r3).runTest();
    }

    @Test
    public void testSelectBigIntLessAndGreater() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Long(15), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE age < 30 AND age > 0")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectBigIntLessEqualAndGreater() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Long(15), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(new Long(-15), BigintType.BIGINT);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(new Long(45), BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE age <= 30 AND age > 0")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5)
                .withOutput(r2, r3).runTest();
    }

    @Test
    public void testSelectBigIntLessAndGreaterEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Long(15), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(new Long(-15), BigintType.BIGINT);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(new Long(45), BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE age < 30 AND age >= 0")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5)
                .withOutput(r1, r2).runTest();
    }

    @Test
    public void testSelectBigIntLessEqualAndGreaterEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Long(15), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(new Long(-15), BigintType.BIGINT);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(new Long(45), BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE age <= 30 AND age >= 0")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5)
                .withOutput(r1, r2, r3).runTest();
    }

    @Test
    public void testSelectBigIntInRange() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Long(15), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(new Long(-15), BigintType.BIGINT);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(new Long(45), BigintType.BIGINT);
        Row r6 = Row.newInstance().addField("row6", VarcharType.VARCHAR)
                .addField(new Long(60), BigintType.BIGINT);
        Row r7 = Row.newInstance().addField("row7", VarcharType.VARCHAR)
                .addField(new Long(90), BigintType.BIGINT);

        // no constraints on predicate pushdown
        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE age IN (0, 15, -15, 45)")
                .withInputSchema(schema).withSplits("row4", "row6")
                .withInput(r1, r2, r3, r4, r5, r6, r7)
                .withOutput(r1, r2, r4, r5).runTest();
    }

    @Test
    public void testSelectBigIntWithOr() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Long(15), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(new Long(-15), BigintType.BIGINT);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(new Long(45), BigintType.BIGINT);
        Row r6 = Row.newInstance().addField("row6", VarcharType.VARCHAR)
                .addField(new Long(60), BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE (age <= 30 AND age >= 0) OR (age > 45 AND age < 70)")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5, r6)
                .withOutput(r1, r2, r3, r6).runTest();
    }

    @Test
    public void testSelectBigIntWithComplexWhereing() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("age", "metadata", "age", BigintType.BIGINT)
                .addColumn("fav_num", "metadata", "fav_num", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT)
                .addField(new Long(0), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Long(15), BigintType.BIGINT)
                .addField(new Long(15), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT)
                .addField(new Long(30), BigintType.BIGINT);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(new Long(-15), BigintType.BIGINT)
                .addField(new Long(-15), BigintType.BIGINT);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(new Long(45), BigintType.BIGINT)
                .addField(new Long(45), BigintType.BIGINT);
        Row r6 = Row.newInstance().addField("row6", VarcharType.VARCHAR)
                .addField(new Long(60), BigintType.BIGINT)
                .addField(new Long(60), BigintType.BIGINT);
        Row r7 = Row.newInstance().addField("row7", VarcharType.VARCHAR)
                .addField(new Long(90), BigintType.BIGINT)
                .addField(new Long(90), BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE (("
                        + "age <= 30 AND age >= 0) OR ("
                        + "age > 45 AND age < 70) OR (age > 80)) AND (("
                        + "fav_num <= 30 AND fav_num > 15) OR ("
                        + "fav_num >= 45 AND fav_num < 60) OR (fav_num = 90))")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5, r6, r7)
                .withOutput(r3, r7).runTest();
    }

    @Test
    public void testSelectBooleanWhereNull() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("male", "metadata", "male", BooleanType.BOOLEAN)
                .addColumn("age", "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(null, BooleanType.BOOLEAN)
                .addField(10L, BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(false, BooleanType.BOOLEAN)
                .addField(10L, BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE male IS NULL")
                .withInputSchema(schema).withInput(r1, r2).withOutput(r1)
                .runTest();
    }

    @Test
    public void testSelectBooleanWhereNotNull() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("male", "metadata", "male", BooleanType.BOOLEAN)
                .addColumn("age", "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(null, BooleanType.BOOLEAN)
                .addField(10L, BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(false, BooleanType.BOOLEAN)
                .addField(10L, BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE male IS NOT NULL")
                .withInputSchema(schema).withInput(r1, r2).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectBooleanWhereNullOrEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("male", "metadata", "male", BooleanType.BOOLEAN)
                .addColumn("age", "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(null, BooleanType.BOOLEAN)
                .addField(10L, BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(false, BooleanType.BOOLEAN)
                .addField(10L, BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(true, BooleanType.BOOLEAN)
                .addField(10L, BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE male = false OR male IS NULL")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r2).runTest();
    }

    @Test
    public void testSelectBooleanIsTrue() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("male",
                "metadata", "male", BooleanType.BOOLEAN);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Boolean(true), BooleanType.BOOLEAN);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Boolean(false), BooleanType.BOOLEAN);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE male = true")
                .withInputSchema(schema).withInput(r1, r2).withOutput(r1)
                .runTest();
    }

    @Test
    public void testSelectBooleanIsFalse() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("male",
                "metadata", "male", BooleanType.BOOLEAN);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(true, BooleanType.BOOLEAN);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(false, BooleanType.BOOLEAN);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE male = false")
                .withInputSchema(schema).withInput(r1, r2).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectBooleanIsTrueAndFalse() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("male", "metadata", "male", BooleanType.BOOLEAN)
                .addColumn("human", "metadata", "human", BooleanType.BOOLEAN);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(true, BooleanType.BOOLEAN)
                .addField(true, BooleanType.BOOLEAN);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(false, BooleanType.BOOLEAN)
                .addField(true, BooleanType.BOOLEAN);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(true, BooleanType.BOOLEAN)
                .addField(false, BooleanType.BOOLEAN);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE male = false AND human = true")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectBooleanInRange() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("male",
                "metadata", "male", BooleanType.BOOLEAN);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(true, BooleanType.BOOLEAN);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(false, BooleanType.BOOLEAN);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE male IN (false, true)")
                .withInputSchema(schema).withInput(r1, r2).withOutput(r1, r2)
                .runTest();
    }

    @Test
    public void testSelectDateWhereNull() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("start_date", "metadata", "start_date",
                        DateType.DATE)
                .addColumn("age", "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(c(2015, 12, 1), DateType.DATE)
                .addField(10L, BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(null, DateType.DATE).addField(10L, BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(c(2015, 12, 31), DateType.DATE)
                .addField(10L, BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE start_date IS NULL")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectDateWhereNotNull() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("start_date", "metadata", "start_date",
                        DateType.DATE)
                .addColumn("age", "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(c(2015, 12, 1), DateType.DATE)
                .addField(10L, BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(null, DateType.DATE).addField(10L, BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(c(2015, 12, 31), DateType.DATE)
                .addField(10L, BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date IS NOT NULL")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r3).runTest();
    }

    @Test
    public void testSelectDateWhereNullOrEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("start_date", "metadata", "start_date",
                        DateType.DATE)
                .addColumn("age", "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(c(2015, 12, 1), DateType.DATE)
                .addField(10L, BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(null, DateType.DATE).addField(10L, BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(c(2015, 12, 31), DateType.DATE)
                .addField(10L, BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date = DATE '2015-12-01' OR start_date IS NULL")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r2).runTest();
    }

    @Test
    public void testSelectDateLess() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", DateType.DATE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(c(2015, 12, 1), DateType.DATE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(c(2015, 12, 15), DateType.DATE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(c(2015, 12, 31), DateType.DATE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date < DATE '2015-12-15'")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r1)
                .runTest();
    }

    @Test
    public void testSelectDateLessOrEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", DateType.DATE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(c(2015, 12, 1), DateType.DATE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(c(2015, 12, 15), DateType.DATE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(c(2015, 12, 31), DateType.DATE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date <= DATE '2015-12-15'")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r2).runTest();
    }

    @Test
    public void testSelectDateEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", DateType.DATE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(c(2015, 12, 1), DateType.DATE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(c(2015, 12, 15), DateType.DATE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(c(2015, 12, 31), DateType.DATE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date = DATE '2015-12-15'")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectDateGreater() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", DateType.DATE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(c(2015, 12, 1), DateType.DATE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(c(2015, 12, 15), DateType.DATE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(c(2015, 12, 31), DateType.DATE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date > DATE '2015-12-15'")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r3)
                .runTest();
    }

    @Test
    public void testSelectDateGreaterOrEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", DateType.DATE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(c(2015, 12, 1), DateType.DATE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(c(2015, 12, 15), DateType.DATE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(c(2015, 12, 31), DateType.DATE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date >= DATE '2015-12-15'")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r2, r3).runTest();
    }

    @Test
    public void testSelectDateLessAndGreater() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", DateType.DATE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(c(2015, 12, 1), DateType.DATE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(c(2015, 12, 15), DateType.DATE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(c(2015, 12, 31), DateType.DATE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE "
                        + "start_date > DATE '2015-12-01' AND "
                        + "start_date < DATE '2015-12-31'")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectDateLessEqualAndGreater() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", DateType.DATE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(c(2015, 12, 1), DateType.DATE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(c(2015, 12, 15), DateType.DATE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(c(2015, 12, 31), DateType.DATE);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(c(2015, 11, 15), DateType.DATE);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(c(2016, 1, 15), DateType.DATE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE "
                        + "start_date > DATE '2015-12-01' AND "
                        + "start_date <= DATE '2015-12-31'")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5)
                .withOutput(r2, r3).runTest();
    }

    @Test
    public void testSelectDateLessAndGreaterEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", DateType.DATE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(c(2015, 12, 1), DateType.DATE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(c(2015, 12, 15), DateType.DATE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(c(2015, 12, 31), DateType.DATE);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(c(2015, 11, 15), DateType.DATE);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(c(2016, 1, 15), DateType.DATE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE "
                        + "start_date >= DATE '2015-12-01' AND "
                        + "start_date < DATE '2015-12-31'")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5)
                .withOutput(r1, r2).runTest();
    }

    @Test
    public void testSelectDateLessEqualAndGreaterEqual() throws Exception {

        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", DateType.DATE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(c(2015, 12, 1), DateType.DATE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(c(2015, 12, 15), DateType.DATE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(c(2015, 12, 31), DateType.DATE);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(c(2015, 11, 15), DateType.DATE);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(c(2016, 1, 15), DateType.DATE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE "
                        + "start_date >= DATE '2015-12-01' AND "
                        + "start_date <= DATE '2015-12-31'")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5)
                .withOutput(r1, r2, r3).runTest();
    }

    @Test
    public void testSelectDateWithOr() throws Exception {

        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", DateType.DATE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(c(2015, 12, 1), DateType.DATE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(c(2015, 12, 15), DateType.DATE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(c(2015, 12, 31), DateType.DATE);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(c(2015, 11, 15), DateType.DATE);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(c(2016, 1, 15), DateType.DATE);
        Row r6 = Row.newInstance().addField("row6", VarcharType.VARCHAR)
                .addField(c(2016, 1, 31), DateType.DATE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE ("
                        + "(start_date > DATE '2015-12-01' AND "
                        + "start_date <= DATE '2015-12-31')) OR ("
                        + "(start_date >= DATE '2016-01-15' AND "
                        + "start_date < DATE '2016-01-31'))")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5, r6)
                .withOutput(r2, r3, r5).runTest();
    }

    @Test
    public void testSelectDateInRange() throws Exception {

        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", DateType.DATE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(c(2015, 12, 1), DateType.DATE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(c(2015, 12, 15), DateType.DATE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(c(2015, 12, 31), DateType.DATE);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(c(2015, 11, 15), DateType.DATE);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(c(2016, 1, 15), DateType.DATE);
        Row r6 = Row.newInstance().addField("row6", VarcharType.VARCHAR)
                .addField(c(2016, 1, 31), DateType.DATE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE start_date IN ("
                        + "DATE '2015-12-01', " + "DATE '2015-12-31', "
                        + "DATE '2016-01-15', " + "DATE '2015-11-15')")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5, r6)
                .withOutput(r1, r3, r5, r4).runTest();
    }

    @Test
    public void testSelectDoubleWhereNull() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("age", "metadata", "age", DoubleType.DOUBLE)
                .addColumn("weight", "metadata", "weight", DoubleType.DOUBLE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Double(0), DoubleType.DOUBLE)
                .addField(new Double(60), DoubleType.DOUBLE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(null, DoubleType.DOUBLE)
                .addField(new Double(60), DoubleType.DOUBLE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Double(30), DoubleType.DOUBLE)
                .addField(new Double(60), DoubleType.DOUBLE);

        // no constraints on predicate pushdown
        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE age IS NULL")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectDoubleWhereNotNull() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("age", "metadata", "age", DoubleType.DOUBLE)
                .addColumn("weight", "metadata", "weight", DoubleType.DOUBLE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Double(0), DoubleType.DOUBLE)
                .addField(new Double(60), DoubleType.DOUBLE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(null, DoubleType.DOUBLE)
                .addField(new Double(60), DoubleType.DOUBLE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Double(30), DoubleType.DOUBLE)
                .addField(new Double(60), DoubleType.DOUBLE);

        // no constraints on predicate pushdown
        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE age IS NOT NULL")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r3).runTest();
    }

    @Test
    public void testSelectDoubleWhereNullOrEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("age", "metadata", "age", DoubleType.DOUBLE)
                .addColumn("weight", "metadata", "weight", DoubleType.DOUBLE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Double(0), DoubleType.DOUBLE)
                .addField(new Double(60), DoubleType.DOUBLE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(null, DoubleType.DOUBLE)
                .addField(new Double(60), DoubleType.DOUBLE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Double(30), DoubleType.DOUBLE)
                .addField(new Double(60), DoubleType.DOUBLE);

        // no constraints on predicate pushdown
        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE age = 0.0 OR age is NULL")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r2).runTest();
    }

    @Test
    public void testSelectDoubleLess() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", DoubleType.DOUBLE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Double(0), DoubleType.DOUBLE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Double(15), DoubleType.DOUBLE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Double(30), DoubleType.DOUBLE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE age < 15")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r1)
                .runTest();
    }

    @Test
    public void testSelectDoubleLessOrEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", DoubleType.DOUBLE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Double(0), DoubleType.DOUBLE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Double(15), DoubleType.DOUBLE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Double(30), DoubleType.DOUBLE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE age <= 15")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r2).runTest();
    }

    @Test
    public void testSelectDoubleEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", DoubleType.DOUBLE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Double(0), DoubleType.DOUBLE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Double(15), DoubleType.DOUBLE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Double(30), DoubleType.DOUBLE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE age = 15")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE age = 15 AND age IS NOT NULL")
                .withInputSchema(schema).withInput(r1).withInput(r2)
                .withInput(r3).withOutput(r2).runTest();

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE age = 15 OR age IS NULL")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectDoubleGreater() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", DoubleType.DOUBLE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Double(0), DoubleType.DOUBLE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Double(15), DoubleType.DOUBLE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Double(30), DoubleType.DOUBLE);
        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE age > 15")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r3)
                .runTest();
    }

    @Test
    public void testSelectDoubleGreaterOrEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", DoubleType.DOUBLE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Double(0), DoubleType.DOUBLE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Double(15), DoubleType.DOUBLE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Double(30), DoubleType.DOUBLE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE age >= 15")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r2, r3).runTest();
    }

    @Test
    public void testSelectDoubleLessAndGreater() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", DoubleType.DOUBLE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Double(0), DoubleType.DOUBLE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Double(15), DoubleType.DOUBLE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Double(30), DoubleType.DOUBLE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE age < 30 AND age > 0")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectDoubleLessEqualAndGreater() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", DoubleType.DOUBLE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Double(0), DoubleType.DOUBLE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Double(15), DoubleType.DOUBLE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Double(30), DoubleType.DOUBLE);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(new Double(-15), DoubleType.DOUBLE);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(new Double(45), DoubleType.DOUBLE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE age <= 30 AND age > 0")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5)
                .withOutput(r2, r3).runTest();
    }

    @Test
    public void testSelectDoubleLessAndGreaterEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", DoubleType.DOUBLE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Double(0), DoubleType.DOUBLE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Double(15), DoubleType.DOUBLE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Double(30), DoubleType.DOUBLE);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(new Double(-15), DoubleType.DOUBLE);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(new Double(45), DoubleType.DOUBLE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE age < 30 AND age >= 0")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5)
                .withOutput(r1, r2).runTest();
    }

    @Test
    public void testSelectDoubleLessEqualAndGreaterEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", DoubleType.DOUBLE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Double(0), DoubleType.DOUBLE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Double(15), DoubleType.DOUBLE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Double(30), DoubleType.DOUBLE);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(new Double(-15), DoubleType.DOUBLE);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(new Double(45), DoubleType.DOUBLE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE age <= 30 AND age >= 0")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5)
                .withOutput(r1, r2, r3).runTest();
    }

    @Test
    public void testSelectDoubleInRange() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", DoubleType.DOUBLE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Double(0), DoubleType.DOUBLE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Double(15), DoubleType.DOUBLE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Double(30), DoubleType.DOUBLE);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(new Double(-15), DoubleType.DOUBLE);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(new Double(45), DoubleType.DOUBLE);
        Row r6 = Row.newInstance().addField("row6", VarcharType.VARCHAR)
                .addField(new Double(60), DoubleType.DOUBLE);
        Row r7 = Row.newInstance().addField("row7", VarcharType.VARCHAR)
                .addField(new Double(90), DoubleType.DOUBLE);

        // no constraints on predicate pushdown
        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE age IN (0, 15, -15, 45)")
                .withInputSchema(schema).withSplits("row4", "row6")
                .withInput(r1, r2, r3, r4, r5, r6, r7)
                .withOutput(r1, r2, r4, r5).runTest();
    }

    @Test
    public void testSelectDoubleWithOr() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", DoubleType.DOUBLE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Double(0), DoubleType.DOUBLE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Double(15), DoubleType.DOUBLE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Double(30), DoubleType.DOUBLE);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(new Double(-15), DoubleType.DOUBLE);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(new Double(45), DoubleType.DOUBLE);
        Row r6 = Row.newInstance().addField("row6", VarcharType.VARCHAR)
                .addField(new Double(60), DoubleType.DOUBLE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE (age <= 30 AND age >= 0) OR (age > 45 AND age < 70)")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5, r6)
                .withOutput(r1, r2, r3, r6).runTest();
    }

    @Test
    public void testSelectDoubleWithComplexWhereing() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("age", "metadata", "age", DoubleType.DOUBLE)
                .addColumn("fav_num", "metadata", "fav_num", DoubleType.DOUBLE);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Double(0), DoubleType.DOUBLE)
                .addField(new Double(0), DoubleType.DOUBLE);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Double(15), DoubleType.DOUBLE)
                .addField(new Double(15), DoubleType.DOUBLE);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Double(30), DoubleType.DOUBLE)
                .addField(new Double(30), DoubleType.DOUBLE);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(new Double(-15), DoubleType.DOUBLE)
                .addField(new Double(-15), DoubleType.DOUBLE);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(new Double(45), DoubleType.DOUBLE)
                .addField(new Double(45), DoubleType.DOUBLE);
        Row r6 = Row.newInstance().addField("row6", VarcharType.VARCHAR)
                .addField(new Double(60), DoubleType.DOUBLE)
                .addField(new Double(60), DoubleType.DOUBLE);
        Row r7 = Row.newInstance().addField("row7", VarcharType.VARCHAR)
                .addField(new Double(90), DoubleType.DOUBLE)
                .addField(new Double(90), DoubleType.DOUBLE);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE (("
                        + "age <= 30 AND age >= 0) OR ("
                        + "age > 45 AND age < 70) OR (age > 80)) AND (("
                        + "fav_num <= 30 AND fav_num > 15) OR ("
                        + "fav_num >= 45 AND fav_num < 60) OR (fav_num = 90))")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5, r6, r7)
                .withOutput(r3, r7).runTest();
    }

    @Test
    public void testSelectTimeWhereNull() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("start_date", "metadata", "start_date",
                        TimeType.TIME)
                .addColumn("age", "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(t(0, 30, 0), TimeType.TIME)
                .addField(10L, BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(null, TimeType.TIME).addField(10L, BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(t(20, 15, 30), TimeType.TIME)
                .addField(10L, BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE start_date IS NULL")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectTimeWhereNotNull() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("start_date", "metadata", "start_date",
                        TimeType.TIME)
                .addColumn("age", "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(t(0, 30, 0), TimeType.TIME)
                .addField(10L, BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(null, TimeType.TIME).addField(10L, BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(t(20, 15, 30), TimeType.TIME)
                .addField(10L, BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date IS NOT NULL")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r3).runTest();
    }

    @Test
    public void testSelectTimeWhereNullOrEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("start_date", "metadata", "start_date",
                        TimeType.TIME)
                .addColumn("age", "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(t(0, 30, 0), TimeType.TIME)
                .addField(10L, BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(null, TimeType.TIME).addField(10L, BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(t(20, 15, 30), TimeType.TIME)
                .addField(10L, BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date = TIME '00:30:00' OR start_date IS NULL")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r2).runTest();
    }

    @Test
    public void testSelectTimeLess() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", TimeType.TIME);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(t(0, 30, 0), TimeType.TIME);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(t(12, 0, 30), TimeType.TIME);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(t(20, 15, 30), TimeType.TIME);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date < TIME '12:00:30'")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r1)
                .runTest();
    }

    @Test
    public void testSelectTimeLessOrEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", TimeType.TIME);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(t(0, 30, 0), TimeType.TIME);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(t(12, 0, 30), TimeType.TIME);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(t(20, 15, 30), TimeType.TIME);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date <= TIME '12:00:30'")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r2).runTest();
    }

    @Test
    public void testSelectTimeEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", TimeType.TIME);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(t(0, 30, 0), TimeType.TIME);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(t(12, 0, 30), TimeType.TIME);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(t(20, 15, 30), TimeType.TIME);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date = TIME '12:00:30'")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectTimeGreater() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", TimeType.TIME);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(t(0, 30, 0), TimeType.TIME);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(t(12, 0, 30), TimeType.TIME);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(t(20, 15, 30), TimeType.TIME);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date > TIME '12:00:30'")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r3)
                .runTest();
    }

    @Test
    public void testSelectTimeGreaterOrEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", TimeType.TIME);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(t(0, 30, 0), TimeType.TIME);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(t(12, 0, 30), TimeType.TIME);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(t(20, 15, 30), TimeType.TIME);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date >= TIME '12:00:30'")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r2, r3).runTest();
    }

    @Test
    public void testSelectTimeLessAndGreater() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", TimeType.TIME);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(t(0, 30, 0), TimeType.TIME);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(t(12, 0, 30), TimeType.TIME);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(t(20, 15, 30), TimeType.TIME);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE "
                        + "start_date > TIME '00:30:00' AND "
                        + "start_date < TIME '20:15:30'")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectTimeLessEqualAndGreater() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", TimeType.TIME);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(t(0, 30, 0), TimeType.TIME);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(t(12, 0, 30), TimeType.TIME);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(t(20, 15, 30), TimeType.TIME);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(t(0, 0, 0), TimeType.TIME);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(t(22, 0, 15), TimeType.TIME);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE "
                        + "start_date > TIME '00:30:00' AND "
                        + "start_date <= TIME '20:15:30'")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5)
                .withOutput(r2, r3).runTest();
    }

    @Test
    public void testSelectTimeLessAndGreaterEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", TimeType.TIME);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(t(0, 30, 0), TimeType.TIME);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(t(12, 0, 30), TimeType.TIME);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(t(20, 15, 30), TimeType.TIME);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(t(0, 0, 0), TimeType.TIME);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(t(22, 0, 15), TimeType.TIME);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE "
                        + "start_date >= TIME '00:30:00' AND "
                        + "start_date < TIME '20:15:30'")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5)
                .withOutput(r1, r2).runTest();
    }

    @Test
    public void testSelectTimeLessEqualAndGreaterEqual() throws Exception {

        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", TimeType.TIME);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(t(0, 30, 0), TimeType.TIME);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(t(12, 0, 30), TimeType.TIME);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(t(20, 15, 30), TimeType.TIME);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(t(0, 0, 0), TimeType.TIME);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(t(22, 0, 15), TimeType.TIME);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE "
                        + "start_date >= TIME '00:30:00' AND "
                        + "start_date <= TIME '20:15:30'")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5)
                .withOutput(r1, r2, r3).runTest();
    }

    @Test
    public void testSelectTimeWithOr() throws Exception {

        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", TimeType.TIME);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(t(0, 30, 0), TimeType.TIME);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(t(12, 0, 30), TimeType.TIME);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(t(20, 15, 30), TimeType.TIME);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(t(0, 0, 0), TimeType.TIME);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(t(22, 0, 15), TimeType.TIME);
        Row r6 = Row.newInstance().addField("row6", VarcharType.VARCHAR)
                .addField(ts(2016, 01, 31, 23, 0, 0), TimeType.TIME);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE ("
                        + "(start_date > TIME '00:30:00' AND "
                        + "start_date <= TIME '20:15:30')) OR ("
                        + "(start_date >= TIME '22:00:15' AND "
                        + "start_date < TIME '23:00:00'))")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5, r6)
                .withOutput(r2, r3, r5).runTest();
    }

    @Test
    public void testSelectTimeInRange() throws Exception {

        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date", TimeType.TIME);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(t(0, 30, 0), TimeType.TIME);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(t(12, 0, 30), TimeType.TIME);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(t(20, 15, 30), TimeType.TIME);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(t(0, 0, 0), TimeType.TIME);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(t(22, 0, 15), TimeType.TIME);
        Row r6 = Row.newInstance().addField("row6", VarcharType.VARCHAR)
                .addField(ts(2016, 01, 31, 23, 0, 0), TimeType.TIME);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE start_date IN ("
                        + "TIME '00:30:00', " + "TIME '20:15:30', "
                        + "TIME '22:00:15', " + "TIME '00:00:00')")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5, r6)
                .withOutput(r1, r3, r5, r4).runTest();
    }

    @Test
    public void testSelectTimestampWhereNull() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("start_date", "metadata", "start_date",
                        TimestampType.TIMESTAMP)
                .addColumn("age", "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(t(0, 30, 0), TimestampType.TIMESTAMP)
                .addField(10L, BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(null, TimestampType.TIMESTAMP)
                .addField(10L, BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 31, 20, 15, 30), TimestampType.TIMESTAMP)
                .addField(10L, BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE start_date IS NULL")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectTimestampWhereNotNull() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("start_date", "metadata", "start_date",
                        TimestampType.TIMESTAMP)
                .addColumn("age", "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 1, 0, 30, 0), TimestampType.TIMESTAMP)
                .addField(10L, BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(null, TimestampType.TIMESTAMP)
                .addField(10L, BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 31, 20, 15, 30), TimestampType.TIMESTAMP)
                .addField(10L, BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date IS NOT NULL")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r3).runTest();
    }

    @Test
    public void testSelectTimestampWhereNullOrEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("start_date", "metadata", "start_date",
                        TimestampType.TIMESTAMP)
                .addColumn("age", "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 1, 0, 30, 0), TimestampType.TIMESTAMP)
                .addField(10L, BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(null, TimestampType.TIMESTAMP)
                .addField(10L, BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 31, 20, 15, 30), TimestampType.TIMESTAMP)
                .addField(10L, BigintType.BIGINT);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date = TIMESTAMP '2015-12-01 00:30:00' OR start_date IS NULL")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r2).runTest();
    }

    @Test
    public void testSelectTimestampLess() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date",
                TimestampType.TIMESTAMP);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 1, 0, 30, 0), TimestampType.TIMESTAMP);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 15, 12, 0, 30), TimestampType.TIMESTAMP);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 31, 20, 15, 30),
                        TimestampType.TIMESTAMP);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date < TIMESTAMP '2015-12-15 12:00:30'")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r1)
                .runTest();
    }

    @Test
    public void testSelectTimestampLessOrEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date",
                TimestampType.TIMESTAMP);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 1, 0, 30, 0), TimestampType.TIMESTAMP);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 15, 12, 0, 30), TimestampType.TIMESTAMP);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 31, 20, 15, 30),
                        TimestampType.TIMESTAMP);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date <= TIMESTAMP '2015-12-15 12:00:30'")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r2).runTest();
    }

    @Test
    public void testSelectTimestampEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date",
                TimestampType.TIMESTAMP);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 1, 0, 30, 0), TimestampType.TIMESTAMP);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 15, 12, 0, 30), TimestampType.TIMESTAMP);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 31, 20, 15, 30),
                        TimestampType.TIMESTAMP);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date = TIMESTAMP '2015-12-15 12:00:30'")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectTimestampGreater() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date",
                TimestampType.TIMESTAMP);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 1, 0, 30, 0), TimestampType.TIMESTAMP);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 15, 12, 0, 30), TimestampType.TIMESTAMP);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 31, 20, 15, 30),
                        TimestampType.TIMESTAMP);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date > TIMESTAMP '2015-12-15 12:00:30'")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r3)
                .runTest();
    }

    @Test
    public void testSelectTimestampGreaterOrEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date",
                TimestampType.TIMESTAMP);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 1, 0, 30, 0), TimestampType.TIMESTAMP);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 15, 12, 0, 30), TimestampType.TIMESTAMP);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 31, 20, 15, 30),
                        TimestampType.TIMESTAMP);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE start_date >= TIMESTAMP '2015-12-15 12:00:30'")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r2, r3).runTest();
    }

    @Test
    public void testSelectTimestampLessAndGreater() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date",
                TimestampType.TIMESTAMP);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 1, 0, 30, 0), TimestampType.TIMESTAMP);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 15, 12, 0, 30), TimestampType.TIMESTAMP);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 31, 20, 15, 30),
                        TimestampType.TIMESTAMP);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE "
                        + "start_date > TIMESTAMP '2015-12-01 00:30:00' AND "
                        + "start_date < TIMESTAMP '2015-12-31 20:15:30'")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectTimestampLessEqualAndGreater() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date",
                TimestampType.TIMESTAMP);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 1, 0, 30, 0), TimestampType.TIMESTAMP);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 15, 12, 0, 30), TimestampType.TIMESTAMP);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 31, 20, 15, 30),
                        TimestampType.TIMESTAMP);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(ts(2015, 11, 15, 0, 0, 0), TimestampType.TIMESTAMP);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(ts(2016, 01, 15, 22, 0, 15), TimestampType.TIMESTAMP);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE "
                        + "start_date > TIMESTAMP '2015-12-01 00:30:00' AND "
                        + "start_date <= TIMESTAMP '2015-12-31 20:15:30'")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5)
                .withOutput(r2, r3).runTest();
    }

    @Test
    public void testSelectTimestampLessAndGreaterEqual() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date",
                TimestampType.TIMESTAMP);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 1, 0, 30, 0), TimestampType.TIMESTAMP);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 15, 12, 0, 30), TimestampType.TIMESTAMP);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 31, 20, 15, 30),
                        TimestampType.TIMESTAMP);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(ts(2015, 11, 15, 0, 0, 0), TimestampType.TIMESTAMP);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(ts(2016, 01, 15, 22, 0, 15), TimestampType.TIMESTAMP);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE "
                        + "start_date >= TIMESTAMP '2015-12-01 00:30:00' AND "
                        + "start_date < TIMESTAMP '2015-12-31 20:15:30'")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5)
                .withOutput(r1, r2).runTest();
    }

    @Test
    public void testSelectTimestampLessEqualAndGreaterEqual() throws Exception {

        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date",
                TimestampType.TIMESTAMP);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 1, 0, 30, 0), TimestampType.TIMESTAMP);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 15, 12, 0, 30), TimestampType.TIMESTAMP);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 31, 20, 15, 30),
                        TimestampType.TIMESTAMP);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(ts(2015, 11, 15, 0, 0, 0), TimestampType.TIMESTAMP);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(ts(2016, 01, 15, 22, 0, 15), TimestampType.TIMESTAMP);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE "
                        + "start_date >= TIMESTAMP '2015-12-01 00:30:00' AND "
                        + "start_date <= TIMESTAMP '2015-12-31 20:15:30'")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5)
                .withOutput(r1, r2, r3).runTest();
    }

    @Test
    public void testSelectTimestampWithOr() throws Exception {

        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date",
                TimestampType.TIMESTAMP);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 1, 0, 30, 0), TimestampType.TIMESTAMP);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 15, 12, 0, 30), TimestampType.TIMESTAMP);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 31, 20, 15, 30),
                        TimestampType.TIMESTAMP);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(ts(2015, 11, 15, 0, 0, 0), TimestampType.TIMESTAMP);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(ts(2016, 01, 15, 22, 0, 15), TimestampType.TIMESTAMP);
        Row r6 = Row.newInstance().addField("row6", VarcharType.VARCHAR)
                .addField(ts(2016, 01, 31, 23, 0, 0), TimestampType.TIMESTAMP);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE ("
                        + "(start_date > TIMESTAMP '2015-12-01 00:30:00' AND "
                        + "start_date <= TIMESTAMP '2015-12-31 20:15:30')) OR ("
                        + "(start_date >= TIMESTAMP '2016-01-15 22:00:15' AND "
                        + "start_date < TIMESTAMP '2016-01-31 23:00:00'))")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5, r6)
                .withOutput(r2, r3, r5).runTest();
    }

    @Test
    public void testSelectTimestampInRange() throws Exception {

        RowSchema schema = RowSchema.newInstance().addRowId().addColumn(
                "start_date", "metadata", "start_date",
                TimestampType.TIMESTAMP);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 1, 0, 30, 0), TimestampType.TIMESTAMP);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 15, 12, 0, 30), TimestampType.TIMESTAMP);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(ts(2015, 12, 31, 20, 15, 30),
                        TimestampType.TIMESTAMP);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(ts(2015, 11, 15, 0, 0, 0), TimestampType.TIMESTAMP);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(ts(2016, 01, 15, 22, 0, 15), TimestampType.TIMESTAMP);
        Row r6 = Row.newInstance().addField("row6", VarcharType.VARCHAR)
                .addField(ts(2016, 01, 31, 23, 0, 0), TimestampType.TIMESTAMP);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE start_date IN ("
                        + "TIMESTAMP '2015-12-01 00:30:00', "
                        + "TIMESTAMP '2015-12-31 20:15:30', "
                        + "TIMESTAMP '2016-01-15 22:00:15', "
                        + "TIMESTAMP '2015-11-15 00:00:00')")
                .withInputSchema(schema).withInput(r1, r2, r3, r4, r5, r6)
                .withOutput(r1, r3, r5, r4).runTest();
    }

    @Test
    @Ignore
    public void testSelectVarbinary() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("bytes",
                "metadata", "bytes", VarbinaryType.VARBINARY);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField("Check out all this data!".getBytes(),
                        VarbinaryType.VARBINARY);

        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField("Check out all this other data!".getBytes(),
                        VarbinaryType.VARBINARY);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable").withQuery("SELECT * FROM testmytable")
                .withInputSchema(schema).withInput(r1, r2).withOutput(r1, r2)
                .runTest();
    }

    @Test
    @Ignore
    public void testSelectVarchar() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("name",
                "metadata", "name", VarcharType.VARCHAR);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField("Alice", VarcharType.VARCHAR);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField("Bob", VarcharType.VARCHAR);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField("Carol", VarcharType.VARCHAR);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable").withQuery("SELECT * FROM testmytable")
                .withInputSchema(schema).withInput(r1, r2, r3)
                .withOutput(r1, r2, r3).runTest();
    }

    @Test
    @Ignore
    public void testSelectArray() throws Exception {
        Type elementType = VarcharType.VARCHAR;
        ArrayType arrayType = new ArrayType(elementType);
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("senders", "metadata", "senders", arrayType);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(AccumuloRowSerializer.getBlockFromArray(elementType,
                        ImmutableList.of("a", "b", "c")), arrayType);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(AccumuloRowSerializer.getBlockFromArray(elementType,
                        ImmutableList.of("d", "e", "f")), arrayType);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable").withQuery("SELECT * FROM testmytable")
                .withInputSchema(schema).withInput(r1, r2).withOutput(r1, r2)
                .runTest();
    }

    @Test
    @Ignore
    public void testSelectNestedArray() throws Exception {
        ArrayType nestedArrayType = new ArrayType(VarcharType.VARCHAR);
        ArrayType arrayType = new ArrayType(nestedArrayType);
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("senders", "metadata", "senders", arrayType);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(
                        AccumuloRowSerializer.getBlockFromArray(nestedArrayType,
                                ImmutableList.of(
                                        ImmutableList.of("a", "b", "c"),
                                        ImmutableList.of("d", "e", "f"),
                                        ImmutableList.of("g", "h", "i"))),
                        arrayType);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(
                        AccumuloRowSerializer.getBlockFromArray(nestedArrayType,
                                ImmutableList.of(
                                        ImmutableList.of("j", "k", "l"),
                                        ImmutableList.of("m", "n", "o"),
                                        ImmutableList.of("p", "q", "r"))),
                        arrayType);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable").withQuery("SELECT * FROM testmytable")
                .withInputSchema(schema).withInput(r1, r2).withOutput(r1, r2)
                .runTest();
    }

    @Test
    @Ignore
    public void testSelectVeryNestedArray() throws Exception {
        ArrayType veryNestedArrayType = new ArrayType(VarcharType.VARCHAR);
        ArrayType nestedArrayType = new ArrayType(veryNestedArrayType);
        ArrayType arrayType = new ArrayType(nestedArrayType);

        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("senders", "metadata", "senders", arrayType);

        // @formatter:off
        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(
                        AccumuloRowSerializer.getBlockFromArray(nestedArrayType,
                        		ImmutableList.of(
                                    ImmutableList.of(
                                            ImmutableList.of("a", "b", "c"),
                                            ImmutableList.of("d", "e", "f"),
                                            ImmutableList.of("g", "h", "i")),
                                    ImmutableList.of(
                                            ImmutableList.of("j", "k", "l"),
                                            ImmutableList.of("m", "n", "o"),
                                            ImmutableList.of("p", "q", "r")),
                                    ImmutableList.of(
                                            ImmutableList.of("s", "t", "u"),
                                            ImmutableList.of("v", "w", "x"),
                                            ImmutableList.of("y", "z", "aa")))),
                        arrayType);
        // @formatter:on

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable").withQuery("SELECT * FROM testmytable")
                .withInputSchema(schema).withInput(r1).withOutput(r1).runTest();
    }

    @Test
    @Ignore
    public void testSelectUberNestedArray() throws Exception {
        ArrayType uberNestedArrayType = new ArrayType(VarcharType.VARCHAR);
        ArrayType veryNestedArrayType = new ArrayType(uberNestedArrayType);
        ArrayType nestedArrayType = new ArrayType(veryNestedArrayType);
        ArrayType arrayType = new ArrayType(nestedArrayType);

        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("senders", "metadata", "senders", arrayType);

        // @formatter:off
        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(
                        AccumuloRowSerializer.getBlockFromArray(nestedArrayType,
                            ImmutableList.of(
                                ImmutableList.of(
                                    ImmutableList.of(
                                            ImmutableList.of("a", "b", "c"),
                                            ImmutableList.of("d", "e", "f"),
                                            ImmutableList.of("g", "h", "i")),
                                    ImmutableList.of(
                                            ImmutableList.of("j", "k", "l"),
                                            ImmutableList.of("m", "n", "o"),
                                            ImmutableList.of("p", "q", "r")),
                                    ImmutableList.of(
                                            ImmutableList.of("s", "t", "u"),
                                            ImmutableList.of("v", "w", "x"),
                                            ImmutableList.of("y", "z", "aa"))),
                                ImmutableList.of(
                                    ImmutableList.of(
                                            ImmutableList.of("a", "b", "c"),
                                            ImmutableList.of("d", "e", "f"),
                                            ImmutableList.of("g", "h", "i")),
                                    ImmutableList.of(
                                            ImmutableList.of("j", "k", "l"),
                                            ImmutableList.of("m", "n", "o"),
                                            ImmutableList.of("p", "q", "r")),
                                    ImmutableList.of(
                                            ImmutableList.of("s", "t", "u"),
                                            ImmutableList.of("v", "w", "x"),
                                            ImmutableList.of("y", "z", "aa"))),
                                ImmutableList.of(
                                    ImmutableList.of(
                                            ImmutableList.of("a", "b", "c"),
                                            ImmutableList.of("d", "e", "f"),
                                            ImmutableList.of("g", "h", "i")),
                                    ImmutableList.of(
                                            ImmutableList.of("j", "k", "l"),
                                            ImmutableList.of("m", "n", "o"),
                                            ImmutableList.of("p", "q", "r")),
                                    ImmutableList.of(
                                            ImmutableList.of("s", "t", "u"),
                                            ImmutableList.of("v", "w", "x"),
                                            ImmutableList.of("y", "z", "aa"))))),
                        arrayType);
        // @formatter:on

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable").withQuery("SELECT * FROM testmytable")
                .withInputSchema(schema).withInput(r1).withOutput(r1).runTest();
    }

    @Test
    @Ignore
    public void testSelectMap() throws Exception {
        Type keyType = VarcharType.VARCHAR;
        Type valueType = BigintType.BIGINT;
        MapType mapType = new MapType(keyType, valueType);
        RowSchema schema = RowSchema.newInstance().addRowId()
                .addColumn("peopleages", "metadata", "peopleages", mapType);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(
                        AccumuloRowSerializer.getBlockFromMap(mapType,
                                ImmutableMap.of("a", 1, "b", 2, "c", 3)),
                        mapType);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(
                        AccumuloRowSerializer.getBlockFromMap(mapType,
                                ImmutableMap.of("d", 4, "e", 5, "f", 6)),
                        mapType);

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable").withQuery("SELECT * FROM testmytable")
                .withInputSchema(schema).withInput(r1, r2).withOutput(r1, r2)
                .runTest();
    }

    @Test(expected = SQLException.class)
    @Ignore
    public void testSelectMapOfArrays() throws Exception {
        Type elementType = BigintType.BIGINT;
        Type keyMapType = new ArrayType(elementType);
        Type valueMapType = new ArrayType(elementType);
        MapType mapType = new MapType(keyMapType, valueMapType);
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("foo",
                "metadata", "foo", mapType);

        // @formatter:off
        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(
                        AccumuloRowSerializer.getBlockFromMap(mapType,
                                ImmutableMap.of(
                                        ImmutableList.of(1, 2, 3),
                                        ImmutableList.of(1, 2, 3))
                                ),
                        mapType);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(
                        AccumuloRowSerializer.getBlockFromMap(mapType,
                                ImmutableMap.of(
                                        ImmutableList.of(4, 5, 6),
                                        ImmutableList.of(4, 5, 6))
                                ),
                        mapType);
        // @formatter:on

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable").withQuery("SELECT * FROM testmytable")
                .withInputSchema(schema).withInput(r1, r2).withOutput(r1, r2)
                .runTest();
    }

    @Test(expected = SQLException.class)
    @Ignore
    public void testSelectMapOfMaps() throws Exception {
        Type keyType = VarcharType.VARCHAR;
        Type valueType = BigintType.BIGINT;
        Type keyMapType = new MapType(keyType, valueType);
        Type valueMapType = new MapType(keyType, valueType);
        MapType mapType = new MapType(keyMapType, valueMapType);
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("foo",
                "metadata", "foo", mapType);

        // @formatter:off
        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(
                        AccumuloRowSerializer.getBlockFromMap(mapType,
                                ImmutableMap.of(
                                        ImmutableMap.of("a", 1, "b", 2, "c", 3),
                                        ImmutableMap.of("a", 1, "b", 2, "c", 3))
                                ),
                        mapType);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(
                        AccumuloRowSerializer.getBlockFromMap(mapType,
                                ImmutableMap.of(
                                        ImmutableMap.of("d", 4, "e", 5, "f", 6),
                                        ImmutableMap.of("d", 4, "e", 5, "f", 6))
                                ),
                        mapType);
        // @formatter:on

        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable").withQuery("SELECT * FROM testmytable")
                .withInputSchema(schema).withInput(r1, r2).withOutput(r1, r2)
                .runTest();
    }

    @Test
    public void testSelectBigIntNoWhereWithSplits() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Long(15), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(new Long(-15), BigintType.BIGINT);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(new Long(45), BigintType.BIGINT);
        Row r6 = Row.newInstance().addField("row6", VarcharType.VARCHAR)
                .addField(new Long(60), BigintType.BIGINT);
        Row r7 = Row.newInstance().addField("row7", VarcharType.VARCHAR)
                .addField(new Long(90), BigintType.BIGINT);

        // no constraints on predicate pushdown
        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable").withQuery("SELECT * FROM testmytable")
                .withInputSchema(schema).withSplits("row4", "row6")
                .withInput(r1, r2, r3, r4, r5, r6, r7)
                .withOutput(r1, r2, r3, r4, r5, r6, r7).runTest();
    }

    @Test
    public void testSelectBigIntWhereRecordKeyIs() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Long(15), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT);

        // no constraints on predicate pushdown
        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery("SELECT * FROM testmytable WHERE recordkey = 'row2'")
                .withInputSchema(schema).withInput(r1, r2, r3).withOutput(r2)
                .runTest();
    }

    @Test
    public void testSelectBigIntWhereRecordKeyInRange() throws Exception {
        RowSchema schema = RowSchema.newInstance().addRowId().addColumn("age",
                "metadata", "age", BigintType.BIGINT);

        Row r1 = Row.newInstance().addField("row1", VarcharType.VARCHAR)
                .addField(new Long(0), BigintType.BIGINT);
        Row r2 = Row.newInstance().addField("row2", VarcharType.VARCHAR)
                .addField(new Long(15), BigintType.BIGINT);
        Row r3 = Row.newInstance().addField("row3", VarcharType.VARCHAR)
                .addField(new Long(30), BigintType.BIGINT);
        Row r4 = Row.newInstance().addField("row4", VarcharType.VARCHAR)
                .addField(new Long(-15), BigintType.BIGINT);
        Row r5 = Row.newInstance().addField("row5", VarcharType.VARCHAR)
                .addField(new Long(45), BigintType.BIGINT);
        Row r6 = Row.newInstance().addField("row6", VarcharType.VARCHAR)
                .addField(new Long(60), BigintType.BIGINT);
        Row r7 = Row.newInstance().addField("row7", VarcharType.VARCHAR)
                .addField(new Long(90), BigintType.BIGINT);

        // no constraints on predicate pushdown
        HARNESS.withHost("localhost").withPort(8080).withSchema("default")
                .withTable("testmytable")
                .withQuery(
                        "SELECT * FROM testmytable WHERE (recordkey < 'row6' AND recordkey >= 'row3') OR recordkey = 'row1'")
                .withInputSchema(schema).withSplits("row4", "row6")
                .withInput(r1, r2, r3, r4, r5, r6, r7)
                .withOutput(r1, r3, r4, r5).runTest();
    }

    private static Calendar c(int y, int m, int d) {
        return new GregorianCalendar(y, m - 1, d);
    }

    private static Long t(int h, int m, int s) {
        return new GregorianCalendar(1970, 0, 1, h, m, s).getTime().getTime();
    }

    private static Long ts(int y, int mo, int d, int h, int mi, int s) {
        return new GregorianCalendar(y, mo - 1, d, h, mi, s).getTime()
                .getTime();
    }
}
