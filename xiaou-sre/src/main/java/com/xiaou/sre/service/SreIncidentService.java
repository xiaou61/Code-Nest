package com.xiaou.sre.service;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.dto.request.SreIncidentQuery;

/**
 * SRE 事故查询和人工生命周期操作。
 *
 * @author xiaou
 */
public interface SreIncidentService {

    PageResult<SreIncident> list(SreIncidentQuery query);

    SreIncident getById(Long id);

    boolean acknowledge(Long id, Long adminId);

    boolean resolve(Long id, Long adminId);
}
