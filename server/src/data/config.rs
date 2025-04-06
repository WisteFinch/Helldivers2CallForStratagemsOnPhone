use serde::{Deserialize, Serialize};

use crate::data::auth::Auth;

// 为了兼容性保留旧的配置结构
// To comply with the JSON specification, ignore non_snake_case warnings.
#[allow(non_snake_case)]
#[derive(Serialize, Deserialize, Clone)]
pub struct Config {
    pub port: u64,
    pub delay: u64,
    pub open: String,
    pub openType: String,
    pub up: String,
    pub down: String,
    pub left: String,
    pub right: String,
    pub ip: String,
}

impl Default for Config {
    fn default() -> Self {
        Self {
            port: 23333,
            delay: 25,
            open: String::from("ctrl_left"),
            openType: String::from("hold"),
            up: String::from("w"),
            down: String::from("s"),
            left: String::from("a"),
            right: String::from("d"),
            ip: String::from(""),
        }
    }
}

// 新的合并配置结构
#[derive(Serialize, Deserialize, Clone)]
pub struct AppConfig {
    pub server: ServerConfig,
    pub auth: AuthConfig,
    pub input: InputConfig,
    pub auth_records: Vec<Auth>,
}

#[derive(Serialize, Deserialize, Clone)]
pub struct ServerConfig {
    pub port: u64,
    pub ip: String,
}

#[derive(Serialize, Deserialize, Clone)]
pub struct AuthConfig {
    pub enabled: bool,
    pub timeout_days: u64,
}

#[derive(Serialize, Deserialize, Clone)]
pub struct InputConfig {
    pub delay: u64,
    pub open: String,
    pub open_type: String,
    pub up: String,
    pub down: String,
    pub left: String,
    pub right: String,
}

impl Default for AppConfig {
    fn default() -> Self {
        Self {
            server: ServerConfig {
                port: 23333,
                ip: String::from(""),
            },
            auth: AuthConfig {
                enabled: true,
                timeout_days: 3,
            },
            input: InputConfig {
                delay: 25,
                open: String::from("ctrl_left"),
                open_type: String::from("hold"),
                up: String::from("w"),
                down: String::from("s"),
                left: String::from("a"),
                right: String::from("d"),
            },
            auth_records: Vec::new(),
        }
    }
}

// 从旧配置转换到新配置
impl From<Config> for AppConfig {
    fn from(old_config: Config) -> Self {
        Self {
            server: ServerConfig {
                port: old_config.port,
                ip: old_config.ip,
            },
            auth: AuthConfig {
                enabled: true,
                timeout_days: 3,
            },
            input: InputConfig {
                delay: old_config.delay,
                open: old_config.open,
                open_type: old_config.openType,
                up: old_config.up,
                down: old_config.down,
                left: old_config.left,
                right: old_config.right,
            },
            auth_records: Vec::new(),
        }
    }
}

// 从新配置转换到旧配置，保持兼容性
impl From<AppConfig> for Config {
    fn from(app_config: AppConfig) -> Self {
        Self {
            port: app_config.server.port,
            ip: app_config.server.ip,
            delay: app_config.input.delay,
            open: app_config.input.open,
            openType: app_config.input.open_type,
            up: app_config.input.up,
            down: app_config.input.down,
            left: app_config.input.left,
            right: app_config.input.right,
        }
    }
} 