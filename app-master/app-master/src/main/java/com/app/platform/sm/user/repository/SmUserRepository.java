package com.app.platform.sm.user.repository;

import com.app.platform.sm.user.domain.SmUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** sm_user 基础 CRUD、动态查询与登录名校验。 */
public interface SmUserRepository extends JpaRepository<SmUser, Long>, JpaSpecificationExecutor<SmUser> {

	Optional<SmUser> findByCodeIgnoreCase(String code);

	boolean existsByCodeIgnoreCase(String code);
}
