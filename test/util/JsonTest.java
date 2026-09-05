package util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link Json} (JUnit 4).
 *
 * Because the project deliberately avoids third-party libraries, this
 * hand-written parser/writer sits between the browser and every REST
 * endpoint. A defect here would corrupt data on every request, so it
 * is tested directly rather than only through the API.
 */
public class JsonTest {

    // ==================== write() ====================

    @Test
    public void writesFlatObject() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("name", "Nimali");
        map.put("role", "RECEPTIONIST");
        assertEquals("{\"name\":\"Nimali\",\"role\":\"RECEPTIONIST\"}", Json.write(map));
    }

    @Test
    public void writesNumbersAndBooleansUnquoted() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("cost", 1500.0);
        map.put("active", true);
        assertEquals("{\"cost\":1500.0,\"active\":true}", Json.write(map));
    }

    @Test
    public void writesNullAsJsonLiteral() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("discount", null);
        assertEquals("{\"discount\":null}", Json.write(map));
    }

    @Test
    public void writesArrayOfObjects() {
        List<Object> list = new ArrayList<Object>();
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("id", "D0001");
        list.add(item);
        assertEquals("[{\"id\":\"D0001\"}]", Json.write(list));
    }

    @Test
    public void escapesQuotesAndBackslashes() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("note", "He said \"hello\"");
        assertEquals("{\"note\":\"He said \\\"hello\\\"\"}", Json.write(map));
    }

    @Test
    public void escapesNewlinesAndTabs() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("text", "line1\nline2\tend");
        assertEquals("{\"text\":\"line1\\nline2\\tend\"}", Json.write(map));
    }

    // ==================== parse() ====================

    @Test
    @SuppressWarnings("unchecked")
    public void parsesFlatObject() {
        Map<String, Object> result = (Map<String, Object>) Json.parse("{\"name\":\"Nimali\"}");
        assertEquals("Nimali", result.get("name"));
    }

    @Test
    public void parsesNumbersAsDouble() {
        Map<String, Object> result = Json.parseObject("{\"cost\":1500.50}");
        assertEquals(1500.50, ((Double) result.get("cost")).doubleValue(), 0.001);
    }

    @Test
    public void parsesBooleansAndNull() {
        Map<String, Object> result = Json.parseObject("{\"active\":true,\"discount\":null}");
        assertEquals(Boolean.TRUE, result.get("active"));
        assertNull(result.get("discount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void parsesNestedArrayOfObjects() {
        Map<String, Object> result = Json.parseObject(
                "{\"dentists\":[{\"id\":\"D0001\"},{\"id\":\"D0002\"}]}");
        List<Object> dentists = (List<Object>) result.get("dentists");
        assertEquals(2, dentists.size());
        assertEquals("D0002", ((Map<String, Object>) dentists.get(1)).get("id"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void parsesEmptyObjectAndArray() {
        assertTrue(((Map<String, Object>) Json.parse("{}")).isEmpty());
        assertTrue(((List<Object>) Json.parse("[]")).isEmpty());
    }

    @Test
    public void ignoresWhitespaceBetweenTokens() {
        Map<String, Object> result = Json.parseObject("{  \"a\" : 1 ,  \"b\" : 2  }");
        assertEquals(1.0, ((Double) result.get("a")).doubleValue(), 0.001);
        assertEquals(2.0, ((Double) result.get("b")).doubleValue(), 0.001);
    }

    @Test
    public void unescapesQuotesInsideStrings() {
        Map<String, Object> result = Json.parseObject("{\"note\":\"He said \\\"hi\\\"\"}");
        assertEquals("He said \"hi\"", result.get("note"));
    }

    @Test
    public void rejectsMalformedInput() {
        try {
            Json.parse("{\"unclosed\":");
            fail("Expected an exception for unterminated JSON.");
        } catch (IllegalArgumentException expected) {
            // correct behaviour
        }
        try {
            Json.parse("{bad}");
            fail("Expected an exception for an unquoted key.");
        } catch (IllegalArgumentException expected) {
            // correct behaviour
        }
    }

    // ==================== round trip ====================

    @Test
    public void writeThenParsePreservesData() {
        Map<String, Object> original = new LinkedHashMap<String, Object>();
        original.put("appointmentNumber", "APT2026-0001");
        original.put("treatmentCost", 4000.0);
        original.put("status", "SCHEDULED");

        Map<String, Object> roundTripped = Json.parseObject(Json.write(original));

        assertEquals("APT2026-0001", roundTripped.get("appointmentNumber"));
        assertEquals(4000.0, ((Double) roundTripped.get("treatmentCost")).doubleValue(), 0.001);
        assertEquals("SCHEDULED", roundTripped.get("status"));
    }

    // ==================== typed getters ====================

    @Test
    public void typedGettersReadValuesOfTheRightType() {
        Map<String, Object> map = Json.parseObject(
                "{\"name\":\"Nimali\",\"cost\":1500.0,\"active\":true}");
        assertEquals("Nimali", Json.getString(map, "name"));
        assertEquals(1500.0, Json.getDouble(map, "cost").doubleValue(), 0.001);
        assertEquals(Boolean.TRUE, Json.getBoolean(map, "active"));
    }

    @Test
    public void typedGettersReturnNullForMissingKeys() {
        Map<String, Object> map = Json.parseObject("{}");
        assertNull(Json.getString(map, "missing"));
        assertNull(Json.getDouble(map, "missing"));
        assertNull(Json.getBoolean(map, "missing"));
    }
}
