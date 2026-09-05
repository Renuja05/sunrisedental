package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Json}.
 *
 * Because the project deliberately avoids third-party libraries, this
 * hand-written parser/writer sits between the browser and every REST
 * endpoint. A defect here would corrupt data on every request, so it
 * is tested directly rather than only through the API.
 */
@DisplayName("Json")
class JsonTest {

    @Nested
    @DisplayName("write()")
    class Writer {

        @Test
        @DisplayName("writes a flat object with string values")
        void writesFlatObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "Nimali");
            map.put("role", "RECEPTIONIST");
            assertEquals("{\"name\":\"Nimali\",\"role\":\"RECEPTIONIST\"}", Json.write(map));
        }

        @Test
        @DisplayName("writes numbers and booleans unquoted")
        void writesNumbersAndBooleans() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("cost", 1500.0);
            map.put("active", true);
            assertEquals("{\"cost\":1500.0,\"active\":true}", Json.write(map));
        }

        @Test
        @DisplayName("writes null as the JSON literal null, not the text \"null\"")
        void writesNull() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("discount", null);
            assertEquals("{\"discount\":null}", Json.write(map));
        }

        @Test
        @DisplayName("writes an array of objects")
        void writesArray() {
            List<Object> list = new ArrayList<>();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", "D0001");
            list.add(item);
            assertEquals("[{\"id\":\"D0001\"}]", Json.write(list));
        }

        @Test
        @DisplayName("escapes quotes and backslashes so the output stays valid JSON")
        void escapesSpecialCharacters() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("note", "He said \"hello\"");
            assertEquals("{\"note\":\"He said \\\"hello\\\"\"}", Json.write(map));
        }

        @Test
        @DisplayName("escapes newlines and tabs")
        void escapesControlCharacters() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("text", "line1\nline2\tend");
            assertEquals("{\"text\":\"line1\\nline2\\tend\"}", Json.write(map));
        }
    }

    @Nested
    @DisplayName("parse()")
    class Parser {

        @Test
        @DisplayName("parses a flat object")
        @SuppressWarnings("unchecked")
        void parsesFlatObject() {
            Map<String, Object> result = (Map<String, Object>) Json.parse("{\"name\":\"Nimali\"}");
            assertEquals("Nimali", result.get("name"));
        }

        @Test
        @DisplayName("parses numbers as Double")
        void parsesNumbers() {
            Map<String, Object> result = Json.parseObject("{\"cost\":1500.50}");
            assertEquals(1500.50, (Double) result.get("cost"), 0.001);
        }

        @Test
        @DisplayName("parses booleans and null")
        void parsesBooleansAndNull() {
            Map<String, Object> result = Json.parseObject("{\"active\":true,\"discount\":null}");
            assertEquals(Boolean.TRUE, result.get("active"));
            assertNull(result.get("discount"));
        }

        @Test
        @DisplayName("parses a nested array of objects")
        @SuppressWarnings("unchecked")
        void parsesNestedArray() {
            Map<String, Object> result = Json.parseObject(
                    "{\"dentists\":[{\"id\":\"D0001\"},{\"id\":\"D0002\"}]}");
            List<Object> dentists = (List<Object>) result.get("dentists");
            assertEquals(2, dentists.size());
            assertEquals("D0002", ((Map<String, Object>) dentists.get(1)).get("id"));
        }

        @Test
        @DisplayName("parses an empty object and an empty array")
        @SuppressWarnings("unchecked")
        void parsesEmptyStructures() {
            assertTrue(((Map<String, Object>) Json.parse("{}")).isEmpty());
            assertTrue(((List<Object>) Json.parse("[]")).isEmpty());
        }

        @Test
        @DisplayName("ignores whitespace between tokens")
        void ignoresWhitespace() {
            Map<String, Object> result = Json.parseObject("{  \"a\" : 1 ,  \"b\" : 2  }");
            assertEquals(1.0, (Double) result.get("a"), 0.001);
            assertEquals(2.0, (Double) result.get("b"), 0.001);
        }

        @Test
        @DisplayName("unescapes quotes inside string values")
        void unescapesQuotes() {
            Map<String, Object> result = Json.parseObject("{\"note\":\"He said \\\"hi\\\"\"}");
            assertEquals("He said \"hi\"", result.get("note"));
        }

        @Test
        @DisplayName("throws a clear exception on malformed input rather than returning nonsense")
        void rejectsMalformedInput() {
            assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"unclosed\":"));
            assertThrows(IllegalArgumentException.class, () -> Json.parse("{bad}"));
        }
    }

    @Nested
    @DisplayName("round trip")
    class RoundTrip {

        @Test
        @DisplayName("writing then parsing returns the original values")
        void writeThenParsePreservesData() {
            Map<String, Object> original = new LinkedHashMap<>();
            original.put("appointmentNumber", "APT2026-0001");
            original.put("treatmentCost", 4000.0);
            original.put("status", "SCHEDULED");

            Map<String, Object> roundTripped = Json.parseObject(Json.write(original));

            assertEquals("APT2026-0001", roundTripped.get("appointmentNumber"));
            assertEquals(4000.0, (Double) roundTripped.get("treatmentCost"), 0.001);
            assertEquals("SCHEDULED", roundTripped.get("status"));
        }
    }

    @Nested
    @DisplayName("typed getters")
    class TypedGetters {

        @Test
        @DisplayName("getString / getDouble / getBoolean read values of the right type")
        void readsTypedValues() {
            Map<String, Object> map = Json.parseObject(
                    "{\"name\":\"Nimali\",\"cost\":1500.0,\"active\":true}");
            assertEquals("Nimali", Json.getString(map, "name"));
            assertEquals(1500.0, Json.getDouble(map, "cost"), 0.001);
            assertEquals(Boolean.TRUE, Json.getBoolean(map, "active"));
        }

        @Test
        @DisplayName("typed getters return null for a missing key instead of throwing")
        void missingKeysReturnNull() {
            Map<String, Object> map = Json.parseObject("{}");
            assertNull(Json.getString(map, "missing"));
            assertNull(Json.getDouble(map, "missing"));
            assertNull(Json.getBoolean(map, "missing"));
        }
    }
}
