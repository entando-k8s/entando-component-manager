package org.entando.kubernetes.client.model;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntandoCoreComponentDeleteResponse {

    private EntandoCoreComponentDeleteResponseStatus status;
    private List<EntandoCoreComponentDelete> components = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntandoCoreComponentDelete {

        private String type;
        private String code;
        private EntandoCoreComponentDeleteStatus status;
    }

    @AllArgsConstructor
    public enum EntandoCoreComponentDeleteResponseStatus {

        SUCCESS("success"), PARTIAL_SUCCESS("partialSuccess"), FAILURE("failure");
        private String code;

        @JsonValue
        public String getCode() {
            return code;
        }
    }

    @Getter
    @AllArgsConstructor
    public enum EntandoCoreComponentDeleteStatus {

        SUCCESS("success"), PARTIAL_SUCCESS("partialSuccess"), FAILURE("failure");
        private String code;

        @JsonValue
        public String getCode() {
            return code;
        }
    }

}
