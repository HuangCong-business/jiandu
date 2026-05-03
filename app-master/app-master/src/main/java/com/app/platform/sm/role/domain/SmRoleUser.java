package com.app.platform.sm.role.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 用户-角色关联实体，对应表 {@code sm_role_user}。
 * 复合主键由 {@link SmRoleUserId} 描述。
 */
@Entity
@Table(name = "sm_role_user")
@IdClass(SmRoleUser.SmRoleUserId.class)
public class SmRoleUser {

	@Id
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Id
	@Column(name = "role_id", nullable = false)
	private Long roleId;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getRoleId() {
		return roleId;
	}

	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}

	/**
	 * JPA {@code @IdClass} 所需的复合主键类型，字段名须与实体 id 属性一致。
	 */
	public static final class SmRoleUserId implements Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

		private Long userId;
		private Long roleId;

		public SmRoleUserId() {
		}

		public SmRoleUserId(Long userId, Long roleId) {
			this.userId = userId;
			this.roleId = roleId;
		}

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public Long getRoleId() {
			return roleId;
		}

		public void setRoleId(Long roleId) {
			this.roleId = roleId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			SmRoleUserId that = (SmRoleUserId) o;
			return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, roleId);
		}
	}
}
