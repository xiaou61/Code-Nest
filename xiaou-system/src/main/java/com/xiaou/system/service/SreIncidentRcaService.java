package com.xiaou.system.service;

import com.xiaou.system.dto.SreRcaReport;

import java.util.Optional;

/**
 * SRE 事故只读根因分析服务。
 *
 * @author xiaou
 */
public interface SreIncidentRcaService {

    Optional<SreRcaReport> investigate(Long incidentId);
}
