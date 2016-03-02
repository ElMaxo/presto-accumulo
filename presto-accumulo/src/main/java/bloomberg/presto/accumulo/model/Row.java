package bloomberg.presto.accumulo.model;

import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.facebook.presto.spi.type.Type;
import org.apache.commons.lang.StringUtils;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.facebook.presto.spi.type.BigintType.BIGINT;
import static com.facebook.presto.spi.type.BooleanType.BOOLEAN;
import static com.facebook.presto.spi.type.DateType.DATE;
import static com.facebook.presto.spi.type.DoubleType.DOUBLE;
import static com.facebook.presto.spi.type.TimeType.TIME;
import static com.facebook.presto.spi.type.TimestampType.TIMESTAMP;
import static com.facebook.presto.spi.type.VarbinaryType.VARBINARY;
import static com.facebook.presto.spi.type.VarcharType.VARCHAR;

/**
 * Class to contain an entire Presto row, made up of {@link Field} objects.
 *
 * Used by {@link bloomberg.presto.accumulo.io.AccumuloPageSink} for writing data as well as the
 * test cases.
 */
public class Row
{
    private List<Field> fields = new ArrayList<>();

    /**
     * Creates a new instance of {@link Row}.
     */
    public Row()
    {}

    /**
     * Copy constructor from one Row to another
     *
     * @param row
     *            Row, copied
     */
    public Row(Row row)
    {
        for (Field f : row.fields) {
            fields.add(new Field(f));
        }
    }

    /**
     * Appends the given field to the end of the row
     *
     * @param f
     *            Field to append
     * @return this, for fluent programming
     */
    public Row addField(Field f)
    {
        fields.add(f);
        return this;
    }

    /**
     * Appends the a new {@link Field} of the given object and type to the end of the row
     *
     * @param v
     *            Value of the field
     * @param t
     *            Type of the field
     * @return this, for fluent programming
     */
    public Row addField(Object v, Type t)
    {
        fields.add(new Field(v, t));
        return this;
    }

    /**
     * Gets the field at the given index
     *
     * @param i
     *            Index in the row to retrieve
     * @return Field
     * @throws IndexOutOfBoundsException
     *             If the index is out of bounds
     */
    public Field getField(int i)
    {
        return fields.get(i);
    }

    /**
     * Gets a list of all internal fields. Any changes to this list will affect this row.
     *
     * @return List of fields
     */
    public List<Field> getFields()
    {
        return fields;
    }

    /**
     * Gets the length of the row, i.e. number of fields
     *
     * @return Length
     */
    public int length()
    {
        return fields.size();
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(fields.toArray());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Row) {
            Row r = (Row) obj;
            int i = 0;
            for (Field f : r.getFields()) {
                if (!this.fields.get(i++).equals(f)) {
                    return false;
                }
            }

            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder bldr = new StringBuilder("(");
        for (Field f : fields) {
            bldr.append(f).append(",");
        }

        if (bldr.length() > 0) {
            bldr.deleteCharAt(bldr.length() - 1);
        }
        return bldr.append(')').toString();
    }

    /**
     * Creates a new {@link Row} from the given delimited string based on the given
     * {@link RowSchema}. Only supports plain Presto types
     *
     * @param schema
     *            Row's schema
     * @param str
     *            String to parse
     * @param delimiter
     *            Delimiter of the string
     * @return A new Row
     * @throws PrestoException
     *             If the length of the split string is not equal to the length of the
     * @throws PrestoException
     *             If the schema contains an unsupported type
     */
    public static Row fromString(RowSchema schema, String str, char delimiter)
    {
        Row r = Row.newRow();

        String[] fields = StringUtils.split(str, delimiter);

        if (fields.length != schema.getLength()) {
            throw new PrestoException(StandardErrorCode.INVALID_FUNCTION_ARGUMENT,
                    "Number of split tokens is not equal to schema length");
        }

        for (int i = 0; i < fields.length; ++i) {
            Type type = schema.getColumn(i).getType();

            if (type == BIGINT) {
                r.addField(Long.parseLong(fields[i]), BIGINT);
            }
            else if (type == BOOLEAN) {
                r.addField(Boolean.parseBoolean(fields[i]), BOOLEAN);
            }
            else if (type == DATE) {
                r.addField(
                        new Date(TimeUnit.MILLISECONDS.toDays(Date.valueOf(fields[i]).getTime())),
                        DATE);
            }
            else if (type == DOUBLE) {
                r.addField(Double.parseDouble(fields[i]), DOUBLE);
            }
            else if (type == TIME) {
                r.addField(Time.valueOf(fields[i]), TIME);
            }
            else if (type == TIMESTAMP) {
                r.addField(Timestamp.valueOf(fields[i]), TIMESTAMP);
            }
            else if (type == VARBINARY) {
                r.addField(fields[i].getBytes(), VARBINARY);
            }
            else if (type == VARCHAR) {
                r.addField(fields[i], VARCHAR);
            }
            else {
                throw new PrestoException(StandardErrorCode.NOT_SUPPORTED,
                        "Unsupported type " + type);
            }
        }

        return r;
    }

    /**
     * Static function to create a new Row
     *
     * @return New row
     */
    public static Row newRow()
    {
        return new Row();
    }
}
