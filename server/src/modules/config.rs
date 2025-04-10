use std::fs;
use std::time::{SystemTime, UNIX_EPOCH};

use crate::data::{AppConfig5, AppConfig, Auth};
use crate::tool::*;
use rust_i18n::t;
use toml;
use serde_json;

// 常量定义
pub const CONF_PATH: &str = "./config.toml";
pub const OLD_CONF_PATH: &str = "./config.json";
pub const AUTH_PATH: &str = "./auth.json";
pub const AUTH_TIMEOUT: u64 = 60 * 60 * 24 * 3;

/// 加载配置文件
/// 
/// 尝试加载TOML格式配置，如果不存在则尝试加载并迁移JSON格式配置
pub async fn load_config() -> Option<AppConfig> {
    let mut app_config: AppConfig;
    let mut has_old_auth = false;
    
    // 尝试加载新的TOML配置
    if let Ok(toml_content) = fs::read_to_string(CONF_PATH) {
        match toml::from_str::<AppConfig>(&toml_content) {
            Ok(loaded_config) => {
                app_config = loaded_config.clone();
                
                let current_time = SystemTime::now()
                    .duration_since(UNIX_EPOCH)
                    .unwrap()
                    .as_secs();
                let timeout = loaded_config.auth.timeout_days * 60 * 60 * 24;
                app_config.auth_records = loaded_config.auth_records
                    .into_iter()
                    .filter(|x| x.time.abs_diff(current_time) <= timeout)
                    .collect();
                
                return Some(app_config);
            },
            Err(e) => {
                warning(format!("{}{}", t!("warn_conf_parse_failed"), e));
            }
        }
    }
    
    // 尝试加载旧的JSON配置并迁移
    let old_config = if let Ok(json_content) = fs::read_to_string(OLD_CONF_PATH) {
        match serde_json::from_str::<AppConfig5>(&json_content) {
            Ok(old_conf) => {
                Some(old_conf)
            },
            Err(e) => {
                warning(format!("{}{}", t!("warn_old_conf_parse_failed"), e));
                None
            }
        }
    } else {
        None
    };
    
    // 尝试加载旧的认证数据
    let old_auths = if let Ok(auth_content) = fs::read_to_string(AUTH_PATH) {
        match serde_json::from_str::<Vec<Auth>>(&auth_content) {
            Ok(auths) => {
                has_old_auth = true;
                auths
            },
            Err(e) => {
                warning(format!("{}{}", t!("warn_auth_parse_failed"), e));
                Vec::new()
            }
        }
    } else {
        Vec::new()
    };
    
    // 如果有旧配置和认证数据，迁移到新格式
    if let Some(old_conf) = old_config {
        info(t!("info_conf_migrate"));
        
        // 打印旧配置信息
        info(t!("n_old_conf_title"));
        println!(" {}{}", t!("n_conf_ip"), 
                    if old_conf.ip.is_empty() { 
                        t!("n_conf_ip_auto").to_string()
                    } else { 
                        old_conf.ip.clone()
                    });
        println!(" {}{}", t!("n_conf_port"), old_conf.port);
        println!(" {}", t!("n_conf_input"));
        println!("  ├─{}{}", t!("n_conf_input_open"), old_conf.open);
        println!("  ├─{}{}", t!("n_conf_input_up"), old_conf.up);
        println!("  ├─{}{}", t!("n_conf_input_down"), old_conf.down);
        println!("  ├─{}{}", t!("n_conf_input_left"), old_conf.left);
        println!("  └─{}{}", t!("n_conf_input_right"), old_conf.right);
        println!(" {}", t!("n_conf_type"));
        println!("  └─{}{}", t!("n_conf_type_open"), old_conf.openType);
        println!(" {}{}", t!("n_conf_input_delay"), old_conf.delay);
        
        // 打印认证记录信息
        if has_old_auth && !old_auths.is_empty() {
            println!(" {}{}", t!("n_auth_records"), old_auths.len());
        }
        
        // 转换为新格式
        app_config = AppConfig::from(old_conf.clone());
            
        // 添加认证数据
        if has_old_auth {
            // 过滤掉过期的认证记录
            let current_time = SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_secs();
            app_config.auth_records = old_auths
                .into_iter()
                .filter(|x| x.time.abs_diff(current_time) <= AUTH_TIMEOUT)
                .collect();
        }
            
        // 保存新格式配置
        match toml::to_string_pretty(&app_config) {
            Ok(toml_content) => {
                if let Err(e) = fs::write(CONF_PATH, toml_content) {
                    warning(format!("{}{}", t!("err_conf_save_failed"), e));
                } else {
                    info(t!("info_conf_migrated"));
                    
                    // 成功迁移后删除旧文件
                    if let Err(e) = fs::remove_file(OLD_CONF_PATH) {
                        warning(format!("{}: {}", t!("err_old_conf_remove_failed"), e));
                    }
                        
                    if has_old_auth {
                        if let Err(e) = fs::remove_file(AUTH_PATH) {
                            warning(format!("{}: {}", t!("err_old_auth_remove_failed"), e));
                        }
                    }
                }
            },
            Err(e) => {
                warning(format!("{}{}", t!("err_conf_serialize_failed"), e));
            }
        }
            
        return Some(app_config);
    }
    
    None
}

/// 保存配置到文件
pub async fn save_config(conf: &AppConfig, sync: bool) {
    // 使用TOML格式保存配置
    match toml::to_string_pretty(&conf) {
        Ok(toml_content) => {
            match fs::write(CONF_PATH, toml_content) {
                Ok(_) => match sync {
                    true => {
                        info(t!("info_sync_complete"));
                        std::process::exit(0);
                    }
                    false => info(t!("info_conf_saved")),
                },
                Err(e) => match sync {
                    true => {
                        error(format!("{}{}", t!("err_sync_failed"), e));
                        std::process::exit(0);
                    }
                    false => error(format!("{}{}", t!("err_conf_save_failed"), e)),
                },
            }
        },
        Err(e) => {
            error(format!("{}{}", t!("err_conf_serialize_failed"), e));
        }
    }
}
