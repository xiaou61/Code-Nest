package com.xiaou.sre.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Alertmanager webhook 请求。
 *
 * @author xiaou
 */
@Data
public class AlertmanagerWebhookRequest {

    private String receiver;
    private String status;
    private Map<String, String> groupLabels;
    private Map<String, String> commonLabels;
    private Map<String, String> commonAnnotations;
    @JsonProperty("externalURL")
    private String externalUrl;

    @Valid
    @NotEmpty
    @Size(max = 100)
    private List<AlertmanagerAlert> alerts;
}
