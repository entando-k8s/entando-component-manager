package org.entando.kubernetes.service.digitalexchange.job.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LabelDescriptor {

    private String key;
    private Map<String, String> titles;

}
