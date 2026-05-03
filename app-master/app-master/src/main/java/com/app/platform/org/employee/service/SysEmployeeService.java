package com.app.platform.org.employee.service;

import com.app.platform.org.employee.domain.SysEmployee;
import com.app.platform.org.employee.repository.SysEmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** 员工档案领域服务：当前仅提供按主键查询。 */
@Service
public class SysEmployeeService {

	private final SysEmployeeRepository sysEmployeeRepository;

	public SysEmployeeService(SysEmployeeRepository sysEmployeeRepository) {
		this.sysEmployeeRepository = sysEmployeeRepository;
	}

	/** 按员工/用户共享主键 id 查询一条员工记录。 */
	public Optional<SysEmployee> getById(Long id) {
		return sysEmployeeRepository.findById(id);
	}
}
