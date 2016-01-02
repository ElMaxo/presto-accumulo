package bloomberg.presto.accumulo.iterators;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.junit.Assert;
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

import bloomberg.presto.accumulo.iterators.SingleColumnValueFilter.CompareOp;
import bloomberg.presto.accumulo.serializers.LexicoderRowSerializer;

public class SingleColumnValueFilterTest {

    private Random rand = new Random();

    @SuppressWarnings("unchecked")
    public boolean testFilter(String filterFam, String filterQual,
            CompareOp filterOp, Object filterValue, Type type, Key testKey,
            Object testValue) throws IOException {

        Value vFilterValue = new Value(
                LexicoderRowSerializer.getLexicoder(type).encode(filterValue));
        Map<String, String> opts = SingleColumnValueFilter.getProperties(
                filterFam, filterQual, filterOp, vFilterValue.get());

        SingleColumnValueFilter filter = new SingleColumnValueFilter();
        filter.validateOptions(opts);
        filter.init(null, opts, null);

        Value vTestValue = new Value(
                LexicoderRowSerializer.getLexicoder(type).encode(testValue));
        return filter.accept(testKey, vTestValue);
    }

    public void testBigint(CompareOp op, BiFunction<Long, Long, Boolean> func)
            throws Exception {
        String filterFam = "cf";
        String filterQual = "cq";

        Key matchingKey = new Key("row", filterFam, filterQual);
        Key wrongFam = new Key("row", "cf2", filterQual);
        Key wrongQual = new Key("row", filterFam, "cq2");

        Set<Long> values = new HashSet<>();
        for (int i = 0; i < 100; ++i) {
            values.add((long) rand.nextInt());
        }

        for (long filterValue : values) {
            for (long testValue : values) {
                Assert.assertTrue("Filter did not accept key with wrong family",
                        testFilter(filterFam, filterQual, op, filterValue,
                                BigintType.BIGINT, wrongFam, testValue));
                Assert.assertTrue("Filter did not accept key with wrong qual",
                        testFilter(filterFam, filterQual, op, filterValue,
                                BigintType.BIGINT, wrongQual, testValue));

                boolean test = testFilter(filterFam, filterQual, op,
                        filterValue, BigintType.BIGINT, matchingKey, testValue);

                if (func.apply(testValue, filterValue)) {
                    Assert.assertTrue(test);
                } else {
                    Assert.assertFalse(test);
                }
            }
        }
    }

    @Test
    public void testBigintLess() throws Exception {
        testBigint(CompareOp.LESS, (x, y) -> Long.compare(x, y) < 0);
    }

    @Test
    public void testBigintLessOrEqual() throws Exception {
        testBigint(CompareOp.LESS_OR_EQUAL, (x, y) -> Long.compare(x, y) <= 0);
    }

    @Test
    public void testBigintEqual() throws Exception {
        testBigint(CompareOp.EQUAL, (x, y) -> Long.compare(x, y) == 0);
    }

    @Test
    public void testBigintNotEqual() throws Exception {
        testBigint(CompareOp.NOT_EQUAL, (x, y) -> Long.compare(x, y) != 0);
    }

    @Test
    public void testBigintNoOp() throws Exception {
        testBigint(CompareOp.NO_OP, (x, y) -> true);
    }

    @Test
    public void testBigintGreater() throws Exception {
        testBigint(CompareOp.GREATER, (x, y) -> Long.compare(x, y) > 0);
    }

    @Test
    public void testBigintGreaterOrEqual() throws Exception {
        testBigint(CompareOp.GREATER_OR_EQUAL,
                (x, y) -> Long.compare(x, y) >= 0);
    }

    public void testBoolean(CompareOp op,
            BiFunction<Boolean, Boolean, Boolean> func) throws Exception {
        String filterFam = "cf";
        String filterQual = "cq";

        Key matchingKey = new Key("row", filterFam, filterQual);
        Key wrongFam = new Key("row", "cf2", filterQual);
        Key wrongQual = new Key("row", filterFam, "cq2");

        Set<Boolean> values = new HashSet<>();
        values.add(true);
        values.add(false);

        for (boolean i : values) {
            for (boolean j : values) {
                byte[] filterValue = i ? LexicoderRowSerializer.TRUE
                        : LexicoderRowSerializer.FALSE;
                byte[] testValue = j ? LexicoderRowSerializer.TRUE
                        : LexicoderRowSerializer.FALSE;

                Assert.assertTrue("Filter did not accept key with wrong family",
                        testFilter(filterFam, filterQual, op, filterValue,
                                BooleanType.BOOLEAN, wrongFam, testValue));
                Assert.assertTrue("Filter did not accept key with wrong qual",
                        testFilter(filterFam, filterQual, op, filterValue,
                                BooleanType.BOOLEAN, wrongQual, testValue));

                boolean test = testFilter(filterFam, filterQual, op,
                        filterValue, BooleanType.BOOLEAN, matchingKey,
                        testValue);

                if (func.apply(j, i)) {
                    Assert.assertTrue(test);
                } else {
                    Assert.assertFalse(test);
                }
            }
        }
    }

    @Test
    public void testBooleanLess() throws Exception {
        testBoolean(CompareOp.LESS, (x, y) -> Boolean.compare(x, y) < 0);
    }

    @Test
    public void testBooleanLessOrEqual() throws Exception {
        testBoolean(CompareOp.LESS_OR_EQUAL,
                (x, y) -> Boolean.compare(x, y) <= 0);
    }

    @Test
    public void testBooleanEqual() throws Exception {
        testBoolean(CompareOp.EQUAL, (x, y) -> Boolean.compare(x, y) == 0);
    }

    @Test
    public void testBooleanNotEqual() throws Exception {
        testBoolean(CompareOp.NOT_EQUAL, (x, y) -> Boolean.compare(x, y) != 0);
    }

    @Test
    public void testBooleanNoOp() throws Exception {
        testBoolean(CompareOp.NO_OP, (x, y) -> true);
    }

    @Test
    public void testBooleanGreater() throws Exception {
        testBoolean(CompareOp.GREATER, (x, y) -> Boolean.compare(x, y) > 0);
    }

    @Test
    public void testBooleanGreaterOrEqual() throws Exception {
        testBoolean(CompareOp.GREATER_OR_EQUAL,
                (x, y) -> Boolean.compare(x, y) >= 0);
    }

    public void testDate(CompareOp op, BiFunction<Date, Date, Boolean> func)
            throws Exception {
        String filterFam = "cf";
        String filterQual = "cq";

        Key matchingKey = new Key("row", filterFam, filterQual);
        Key wrongFam = new Key("row", "cf2", filterQual);
        Key wrongQual = new Key("row", filterFam, "cq2");

        Set<Date> values = new HashSet<>();
        for (int i = 0; i < 100; ++i) {
            values.add(new Date(rand.nextInt()));
        }

        for (Date filterValue : values) {
            for (Date testValue : values) {
                Assert.assertTrue("Filter did not accept key with wrong family",
                        testFilter(filterFam, filterQual, op,
                                filterValue.getTime(), DateType.DATE, wrongFam,
                                testValue.getTime()));
                Assert.assertTrue("Filter did not accept key with wrong qual",
                        testFilter(filterFam, filterQual, op,
                                filterValue.getTime(), DateType.DATE, wrongQual,
                                testValue.getTime()));

                boolean test = testFilter(filterFam, filterQual, op,
                        filterValue.getTime(), DateType.DATE, matchingKey,
                        testValue.getTime());

                if (func.apply(testValue, filterValue)) {
                    Assert.assertTrue(test);
                } else {
                    Assert.assertFalse(test);
                }
            }
        }
    }

    @Test
    public void testDateLess() throws Exception {
        testDate(CompareOp.LESS, (x, y) -> x.compareTo(y) < 0);
    }

    @Test
    public void testDateLessOrEqual() throws Exception {
        testDate(CompareOp.LESS_OR_EQUAL, (x, y) -> x.compareTo(y) <= 0);
    }

    @Test
    public void testDateEqual() throws Exception {
        testDate(CompareOp.EQUAL, (x, y) -> x.compareTo(y) == 0);
    }

    @Test
    public void testDateNotEqual() throws Exception {
        testDate(CompareOp.NOT_EQUAL, (x, y) -> x.compareTo(y) != 0);
    }

    @Test
    public void testDateNoOp() throws Exception {
        testDate(CompareOp.NO_OP, (x, y) -> true);
    }

    @Test
    public void testDateGreater() throws Exception {
        testDate(CompareOp.GREATER, (x, y) -> x.compareTo(y) > 0);
    }

    @Test
    public void testDateGreaterOrEqual() throws Exception {
        testDate(CompareOp.GREATER_OR_EQUAL, (x, y) -> x.compareTo(y) >= 0);
    }

    public void testDouble(CompareOp op,
            BiFunction<Double, Double, Boolean> func) throws Exception {
        String filterFam = "cf";
        String filterQual = "cq";

        Key matchingKey = new Key("row", filterFam, filterQual);
        Key wrongFam = new Key("row", "cf2", filterQual);
        Key wrongQual = new Key("row", filterFam, "cq2");

        Set<Double> values = new HashSet<>();
        for (int i = 0; i < 100; ++i) {
            values.add(rand.nextDouble());
        }

        for (double filterValue : values) {
            for (double testValue : values) {
                Assert.assertTrue("Filter did not accept key with wrong family",
                        testFilter(filterFam, filterQual, op, filterValue,
                                DoubleType.DOUBLE, wrongFam, testValue));
                Assert.assertTrue("Filter did not accept key with wrong qual",
                        testFilter(filterFam, filterQual, op, filterValue,
                                DoubleType.DOUBLE, wrongQual, testValue));

                boolean test = testFilter(filterFam, filterQual, op,
                        filterValue, DoubleType.DOUBLE, matchingKey, testValue);

                if (func.apply(testValue, filterValue)) {
                    Assert.assertTrue(test);
                } else {
                    Assert.assertFalse(test);
                }
            }
        }
    }

    @Test
    public void testDoubleLess() throws Exception {
        testDouble(CompareOp.LESS, (x, y) -> x.compareTo(y) < 0);
    }

    @Test
    public void testDoubleLessOrEqual() throws Exception {
        testDouble(CompareOp.LESS_OR_EQUAL, (x, y) -> x.compareTo(y) <= 0);
    }

    @Test
    public void testDoubleEqual() throws Exception {
        testDouble(CompareOp.EQUAL, (x, y) -> x.compareTo(y) == 0);
    }

    @Test
    public void testDoubleNotEqual() throws Exception {
        testDouble(CompareOp.NOT_EQUAL, (x, y) -> x.compareTo(y) != 0);
    }

    @Test
    public void testDoubleNoOp() throws Exception {
        testDouble(CompareOp.NO_OP, (x, y) -> true);
    }

    @Test
    public void testDoubleGreater() throws Exception {
        testDouble(CompareOp.GREATER, (x, y) -> x.compareTo(y) > 0);
    }

    @Test
    public void testDoubleGreaterOrEqual() throws Exception {
        testDouble(CompareOp.GREATER_OR_EQUAL, (x, y) -> x.compareTo(y) >= 0);
    }

    public void testTime(CompareOp op, BiFunction<Time, Time, Boolean> func)
            throws Exception {
        String filterFam = "cf";
        String filterQual = "cq";

        Key matchingKey = new Key("row", filterFam, filterQual);
        Key wrongFam = new Key("row", "cf2", filterQual);
        Key wrongQual = new Key("row", filterFam, "cq2");

        Set<Time> values = new HashSet<>();
        for (int i = 0; i < 100; ++i) {
            values.add(new Time(rand.nextInt()));
        }

        for (Time filterValue : values) {
            for (Time testValue : values) {

                Assert.assertTrue("Filter did not accept key with wrong family",
                        testFilter(filterFam, filterQual, op,
                                filterValue.getTime(), TimeType.TIME, wrongFam,
                                testValue.getTime()));
                Assert.assertTrue("Filter did not accept key with wrong qual",
                        testFilter(filterFam, filterQual, op,
                                filterValue.getTime(), TimeType.TIME, wrongQual,
                                testValue.getTime()));

                boolean test = testFilter(filterFam, filterQual, op,
                        filterValue.getTime(), TimeType.TIME, matchingKey,
                        testValue.getTime());

                if (func.apply(testValue, filterValue)) {
                    Assert.assertTrue(test);
                } else {
                    Assert.assertFalse(test);
                }
            }
        }
    }

    @Test
    public void testTimeLess() throws Exception {
        testTime(CompareOp.LESS, (x, y) -> x.compareTo(y) < 0);
    }

    @Test
    public void testTimeLessOrEqual() throws Exception {
        testTime(CompareOp.LESS_OR_EQUAL, (x, y) -> x.compareTo(y) <= 0);
    }

    @Test
    public void testTimeEqual() throws Exception {
        testTime(CompareOp.EQUAL, (x, y) -> x.compareTo(y) == 0);
    }

    @Test
    public void testTimeNotEqual() throws Exception {
        testTime(CompareOp.NOT_EQUAL, (x, y) -> x.compareTo(y) != 0);
    }

    @Test
    public void testTimeNoOp() throws Exception {
        testTime(CompareOp.NO_OP, (x, y) -> true);
    }

    @Test
    public void testTimeGreater() throws Exception {
        testTime(CompareOp.GREATER, (x, y) -> x.compareTo(y) > 0);
    }

    @Test
    public void testTimeGreaterOrEqual() throws Exception {
        testTime(CompareOp.GREATER_OR_EQUAL, (x, y) -> x.compareTo(y) >= 0);
    }

    public void testTimestamp(CompareOp op,
            BiFunction<Timestamp, Timestamp, Boolean> func) throws Exception {
        String filterFam = "cf";
        String filterQual = "cq";

        Key matchingKey = new Key("row", filterFam, filterQual);
        Key wrongFam = new Key("row", "cf2", filterQual);
        Key wrongQual = new Key("row", filterFam, "cq2");

        Set<Timestamp> values = new HashSet<>();
        for (int i = 0; i < 100; ++i) {
            values.add(new Timestamp(rand.nextInt()));
        }

        for (Timestamp filterValue : values) {
            for (Timestamp testValue : values) {
                Assert.assertTrue("Filter did not accept key with wrong family",
                        testFilter(filterFam, filterQual, op,
                                filterValue.getTime(), TimestampType.TIMESTAMP,
                                wrongFam, testValue.getTime()));
                Assert.assertTrue("Filter did not accept key with wrong qual",
                        testFilter(filterFam, filterQual, op,
                                filterValue.getTime(), TimestampType.TIMESTAMP,
                                wrongQual, testValue.getTime()));

                boolean test = testFilter(filterFam, filterQual, op,
                        filterValue.getTime(), TimestampType.TIMESTAMP,
                        matchingKey, testValue.getTime());

                if (func.apply(testValue, filterValue)) {
                    Assert.assertTrue(test);
                } else {
                    Assert.assertFalse(test);
                }
            }
        }
    }

    @Test
    public void testTimestampLess() throws Exception {
        testTimestamp(CompareOp.LESS, (x, y) -> x.compareTo(y) < 0);
    }

    @Test
    public void testTimestampLessOrEqual() throws Exception {
        testTimestamp(CompareOp.LESS_OR_EQUAL, (x, y) -> x.compareTo(y) <= 0);
    }

    @Test
    public void testTimestampEqual() throws Exception {
        testTimestamp(CompareOp.EQUAL, (x, y) -> x.compareTo(y) == 0);
    }

    @Test
    public void testTimestampNotEqual() throws Exception {
        testTimestamp(CompareOp.NOT_EQUAL, (x, y) -> x.compareTo(y) != 0);
    }

    @Test
    public void testTimestampNoOp() throws Exception {
        testTimestamp(CompareOp.NO_OP, (x, y) -> true);
    }

    @Test
    public void testTimestampGreater() throws Exception {
        testTimestamp(CompareOp.GREATER, (x, y) -> x.compareTo(y) > 0);
    }

    @Test
    public void testTimestampGreaterOrEqual() throws Exception {
        testTimestamp(CompareOp.GREATER_OR_EQUAL,
                (x, y) -> x.compareTo(y) >= 0);
    }

    public void testVarbinary(CompareOp op,
            BiFunction<byte[], byte[], Boolean> func) throws Exception {
        String filterFam = "cf";
        String filterQual = "cq";

        Key matchingKey = new Key("row", filterFam, filterQual);
        Key wrongFam = new Key("row", "cf2", filterQual);
        Key wrongQual = new Key("row", filterFam, "cq2");

        Set<byte[]> bytes = new HashSet<>();
        for (int i = 0; i < 100; ++i) {
            bytes.add(UUID.randomUUID().toString().getBytes());
        }

        for (byte[] filterValue : bytes) {
            for (byte[] testValue : bytes) {
                Assert.assertTrue("Filter did not accept key with wrong family",
                        testFilter(filterFam, filterQual, op, filterValue,
                                VarbinaryType.VARBINARY, wrongFam, testValue));
                Assert.assertTrue("Filter did not accept key with wrong qual",
                        testFilter(filterFam, filterQual, op, filterValue,
                                VarbinaryType.VARBINARY, wrongQual, testValue));

                boolean test = testFilter(filterFam, filterQual, op,
                        filterValue, VarbinaryType.VARBINARY, matchingKey,
                        testValue);

                if (func.apply(testValue, filterValue)) {
                    Assert.assertTrue(test);
                } else {
                    Assert.assertFalse(test);
                }
            }
        }
    }

    @Test
    public void testVarbinaryLess() throws Exception {
        testVarbinary(CompareOp.LESS,
                (x, y) -> ByteBuffer.wrap(x).compareTo(ByteBuffer.wrap(y)) < 0);
    }

    @Test
    public void testVarbinaryLessOrEqual() throws Exception {
        testVarbinary(CompareOp.LESS_OR_EQUAL, (x,
                y) -> ByteBuffer.wrap(x).compareTo(ByteBuffer.wrap(y)) <= 0);
    }

    @Test
    public void testVarbinaryEqual() throws Exception {
        testVarbinary(CompareOp.EQUAL, (x,
                y) -> ByteBuffer.wrap(x).compareTo(ByteBuffer.wrap(y)) == 0);
    }

    @Test
    public void testVarbinaryNotEqual() throws Exception {
        testVarbinary(CompareOp.NOT_EQUAL, (x,
                y) -> ByteBuffer.wrap(x).compareTo(ByteBuffer.wrap(y)) != 0);
    }

    @Test
    public void testVarbinaryNoOp() throws Exception {
        testVarbinary(CompareOp.NO_OP, (x, y) -> true);
    }

    @Test
    public void testVarbinaryGreater() throws Exception {
        testVarbinary(CompareOp.GREATER,
                (x, y) -> ByteBuffer.wrap(x).compareTo(ByteBuffer.wrap(y)) > 0);
    }

    @Test
    public void testVarbinaryGreaterOrEqual() throws Exception {
        testVarbinary(CompareOp.GREATER_OR_EQUAL, (x,
                y) -> ByteBuffer.wrap(x).compareTo(ByteBuffer.wrap(y)) >= 0);
    }

    public void testVarchar(CompareOp op,
            BiFunction<String, String, Boolean> func) throws Exception {
        String filterFam = "cf";
        String filterQual = "cq";

        Key matchingKey = new Key("row", filterFam, filterQual);
        Key wrongFam = new Key("row", "cf2", filterQual);
        Key wrongQual = new Key("row", filterFam, "cq2");

        Set<String> bytes = new HashSet<>();
        for (int i = 0; i < 100; ++i) {
            bytes.add(UUID.randomUUID().toString());
        }

        for (String filterValue : bytes) {
            for (String testValue : bytes) {
                Assert.assertTrue("Filter did not accept key with wrong family",
                        testFilter(filterFam, filterQual, op, filterValue,
                                VarcharType.VARCHAR, wrongFam, testValue));
                Assert.assertTrue("Filter did not accept key with wrong qual",
                        testFilter(filterFam, filterQual, op, filterValue,
                                VarcharType.VARCHAR, wrongQual, testValue));

                boolean test = testFilter(filterFam, filterQual, op,
                        filterValue, VarcharType.VARCHAR, matchingKey,
                        testValue);

                if (func.apply(testValue, filterValue)) {
                    Assert.assertTrue(test);
                } else {
                    Assert.assertFalse(test);
                }
            }
        }
    }

    @Test
    public void testVarcharLess() throws Exception {
        testVarchar(CompareOp.LESS, (x, y) -> x.compareTo(y) < 0);
    }

    @Test
    public void testVarcharLessOrEqual() throws Exception {
        testVarchar(CompareOp.LESS_OR_EQUAL, (x, y) -> x.compareTo(y) <= 0);
    }

    @Test
    public void testVarcharEqual() throws Exception {
        testVarchar(CompareOp.EQUAL, (x, y) -> x.compareTo(y) == 0);
    }

    @Test
    public void testVarcharNotEqual() throws Exception {
        testVarchar(CompareOp.NOT_EQUAL, (x, y) -> x.compareTo(y) != 0);
    }

    @Test
    public void testVarcharNoOp() throws Exception {
        testVarchar(CompareOp.NO_OP, (x, y) -> true);
    }

    @Test
    public void testVarcharGreater() throws Exception {
        testVarchar(CompareOp.GREATER, (x, y) -> x.compareTo(y) > 0);
    }

    @Test
    public void testVarcharGreaterOrEqual() throws Exception {
        testVarchar(CompareOp.GREATER_OR_EQUAL, (x, y) -> x.compareTo(y) >= 0);
    }
}