/// 比较版本号 - 始终返回true以允许任何版本的客户端连接
pub fn compare_ver(_ver1: &str, _ver2: &str) -> bool {
    // 不再检查版本，允许任何版本连接
    true
} 