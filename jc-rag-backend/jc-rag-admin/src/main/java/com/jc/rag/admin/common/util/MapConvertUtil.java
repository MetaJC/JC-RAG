package com.jc.rag.admin.common.util;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Map/List 通用转换工具
 * @author JC
 */
public final class MapConvertUtil {

    private MapConvertUtil() {}

    /** 复制 map 中的指定 key 到目标 map, 同时做类型转换 */
    public static <V> void copyProp(Map<String, Object> src, Map<String, Object> dst, String... keys) {
        if (src == null || dst == null || keys == null) return;
        for (String k : keys) {
            if (src.containsKey(k) && src.get(k) != null) {
                dst.put(k, src.get(k));
            }
        }
    }

    /** "1,2,3" -> List<Long> */
    public static List<Long> parseIdList(String csv) {
        if (!StringUtils.hasText(csv)) return new ArrayList<>();
        List<Long> result = new ArrayList<>();
        for (String s : csv.split(",")) {
            s = s.trim();
            if (s.isEmpty()) continue;
            try { result.add(Long.parseLong(s)); } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    /** List<Long> -> "1,2,3" */
    public static String toCsv(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(ids.get(i));
        }
        return sb.toString();
    }

    /** Map -> 安全的 Object (Integer/Long/Double/String 保持原类型) */
    public static Object safeGet(Map<String, Object> m, String key) {
        if (m == null) return null;
        return m.get(key);
    }

    /** 字段脱敏 (用于 AOP 日志) */
    public static Map<String, Object> maskSensitive(Map<String, Object> m, List<String> sensitiveKeys) {
        if (m == null || sensitiveKeys == null || sensitiveKeys.isEmpty()) return m;
        Map<String, Object> copy = new HashMap<>(m);
        for (String k : sensitiveKeys) {
            if (copy.containsKey(k)) copy.put(k, "******");
        }
        return copy;
    }
}
