/*
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
package bloomberg.presto.accumulo;

import static bloomberg.presto.accumulo.MetadataUtil.CATALOG_CODEC;
import static com.facebook.presto.spi.type.BigintType.BIGINT;
import static com.facebook.presto.spi.type.VarcharType.VARCHAR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.net.URI;
import java.net.URL;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

public class TestAccumuloClient {
    @Test
    public void testMetadata() throws Exception {
        URL metadataUrl = Resources.getResource(TestAccumuloClient.class,
                "/example-data/example-metadata.json");
        assertNotNull(metadataUrl, "metadataUrl is null");
        URI metadata = metadataUrl.toURI();
        AccumuloClient client = new AccumuloClient(
                new AccumuloConfig().setMetadata(metadata), CATALOG_CODEC);
        assertEquals(client.getSchemaNames(),
                ImmutableSet.of("example", "tpch"));
        assertEquals(client.getTableNames("example"),
                ImmutableSet.of("numbers"));
        assertEquals(client.getTableNames("tpch"),
                ImmutableSet.of("orders", "lineitem"));

        AccumuloTable table = client.getTable("example", "numbers");
        assertNotNull(table, "table is null");
        assertEquals(table.getName(), "numbers");
        assertEquals(table.getColumns(), ImmutableList.of(new AccumuloColumn(
                "text", VARCHAR), new AccumuloColumn("value", BIGINT)));
        assertEquals(
                table.getSources(),
                ImmutableList.of(metadata.resolve("numbers-1.csv"),
                        metadata.resolve("numbers-2.csv")));
    }
}
