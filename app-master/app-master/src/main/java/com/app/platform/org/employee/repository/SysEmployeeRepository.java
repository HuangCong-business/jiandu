package com.app.platform.org.employee.repository;

import com.app.platform.org.employee.domain.SysEmployee;
import org.springframework.data.jpa.repository.JpaRepository;

/** 员工表 {@code sys_employee} 的标准 JPA 仓储。 */
public interface SysEmployeeRepository extends JpaRepository<SysEmployee, Long> {
}
