pub mod config;
pub mod auth;
pub mod net;
pub mod input;

// 仅导出外部模块需要的功能
// 避免导出太多内部实现细节
pub use net::run;  // 只导出主要的run函数
pub use config::{load_config, save_config};  // 导出配置相关函数
pub use auth::{load_auth, save_auth};  // 导出认证相关函数
