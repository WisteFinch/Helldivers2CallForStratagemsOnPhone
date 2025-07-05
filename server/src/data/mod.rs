// 定义子模块
pub mod config;
pub mod input;
pub mod operation;
pub mod auth;

// 从子模块重新导出公共API
pub use config::{AppConfig, AuthConfig, AppConfig5, InputConfig, ServerConfig};
pub use input::{InputData, InputType, KeyType, Step};
pub use operation::{Operation, Status};
pub use auth::Auth; 