package com.xiaou.sre.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * Alertmanager webhook 中的单条告警。
 *
 * @author xiaou
 */
@Data
public class AlertmanagerAlert {

    @NotBlank
    @Size(max = 16)
    private String status;

    private Map<String, String> labels;

    private Map<String, String> annotations;

    @NotBlank
    @Size(max = 64)
    private String startsAt;

    private String endsAt;

    @JsonProperty("generatorURL")
    @Size(max = 1000)
    private String generatorUrl;

    @NotBlank
    @Size(max = 128)
    private String fingerprint;
}
