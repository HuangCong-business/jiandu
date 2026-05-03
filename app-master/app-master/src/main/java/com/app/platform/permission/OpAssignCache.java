package com.app.platform.permission;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 单租户操作码缓存（无 tenantId 前缀）。角色变更时按用户失效。
 */
@Component
public class OpAssignCache {

	// 菜单维度：该菜单下全部有效操作码
	private final ConcurrentHashMap<String, List<String>> menuAllOps = new ConcurrentHashMap<>();
	// 用户+菜单维度：角色分配得到的操作码
	private final ConcurrentHashMap<String, List<String>> userMenuAssignedOps = new ConcurrentHashMap<>();

	/** 缓存「菜单全量操作」；首次 miss 时调用 loader 并拷贝为不可变列表。 */
	public List<String> getMenuOpCodes(long menuId, Supplier<List<String>> loader) {
		String key = "m:" + menuId;
		return menuAllOps.computeIfAbsent(key, k -> List.copyOf(loader.get()));
	}

	public List<String> getAssignedOpCodes(long userId, long menuId, Supplier<List<String>> loader) {
		String key = userId + ":" + menuId;
		return userMenuAssignedOps.computeIfAbsent(key, k -> List.copyOf(loader.get()));
	}

	/** 用户角色变更后失效其所有 (user,menu) 缓存项。 */
	public void evictUser(long userId) {
		String prefix = userId + ":";
		userMenuAssignedOps.keySet().removeIf(key -> key.startsWith(prefix));
	}

	/** 菜单或操作定义变更：清菜单全量缓存及涉及该菜单的用户分配缓存。 */
	public void evictMenu(long menuId) {
		menuAllOps.remove("m:" + menuId);
		userMenuAssignedOps.keySet().removeIf(key -> key.endsWith(":" + menuId));
	}

	public void evictAll() {
		menuAllOps.clear();
		userMenuAssignedOps.clear();
	}
}
