// 定义子模块
pub mod logger;
pub mod key_utils;

// 重新导出常用功能
pub use logger::{debug_log, error, info, print, println, warning};
pub use key_utils::StringToKey;