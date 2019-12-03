package org.entando.kubernetes;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import org.junit.Test;

public class ZipReaderTest {

    @Test
    public void test_yaml_parsing_without_quotes() throws IOException {
        String yaml = "---\n"
                + "id: ID\n"
                + "value: 1";

        YAMLMapper mapper = new YAMLMapper();
        // factory.configure()
        TempObject obj = mapper.readValue(yaml, TempObject.class);
        assertEquals("ID", obj.getId());
        assertEquals(Integer.valueOf(1), obj.getValue());
    }

    @Getter
    @Setter
    private static class TempObject {

        String id;
        Integer value;

    }

}
