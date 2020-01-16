package org.entando.kubernetes;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.junit.Test;

public class ZipReaderTest {

    @Test
    public void test_yaml_parsing_without_quotes() throws IOException {
        String yaml = "id: ID\nvalue: 1";

        YAMLMapper mapper = new YAMLMapper();
        TempObject obj = mapper.readValue(yaml, TempObject.class);
        assertEquals("ID", obj.getId());
        assertEquals(Integer.valueOf(1), obj.getValue());
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TempObject {

        String id;
        Integer value;

    }

}
