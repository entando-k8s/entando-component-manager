package org.entando.kubernetes.client.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
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

    public enum EntandoCoreComponentDeleteResponseStatus {
        SUCCESS,PARTIAL_SUCCESS, FAILURE;
    }

    public enum EntandoCoreComponentDeleteStatus {
        SUCCESS, ERROR;
    }

}
