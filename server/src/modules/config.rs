use std::fs;
use std::time::{SystemTime, UNIX_EPOCH};
use std::io;

use crate::data::{Config, AppConfig, Auth};
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
pub async fn load_config() -> Option<Config> {
    let mut app_config: AppConfig;
    let mut has_old_auth = false;
    
    // 尝试加载新的TOML配置
    if let Ok(toml_content) = fs::read_to_string(CONF_PATH) {
        match toml::from_str::<AppConfig>(&toml_content) {
            Ok(loaded_config) => {
                app_config = loaded_config;
                // 转换为旧的Config格式以保持兼容性
                return Some(Config::from(app_config));
            },
            Err(e) => {
                warning(format!("解析TOML配置失败: {}", e));
            }
        }
    }
    
    // 尝试加载旧的JSON配置并迁移
    let old_config = if let Ok(json_content) = fs::read_to_string(OLD_CONF_PATH) {
        match serde_json::from_str::<Config>(&json_content) {
            Ok(old_conf) => {
                Some(old_conf)
            },
            Err(e) => {
                warning(format!("解析JSON配置失败: {}", e));
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
                warning(format!("解析认证数据失败: {}", e));
                Vec::new()
            }
        }
    } else {
        Vec::new()
    };
    
    // 如果有旧配置和认证数据，询问是否迁移到新格式
    if let Some(old_conf) = old_config {
        // 询问用户是否迁移
        println!("- {}", t!("ask_migrate_title"));
        
        // 打印旧配置信息
        println!("- {}", t!("old_config_info"));
        println!("  {}: {}", t!("config_port"), old_conf.port);
        println!("  {}: {}", t!("config_ip"), 
                 if old_conf.ip.is_empty() { 
                     t!("config_ip_auto").to_string()
                 } else { 
                     old_conf.ip.clone()
                 });
        println!("  {}: {}", t!("config_delay"), old_conf.delay);
        println!("  {}: {}", t!("config_open"), old_conf.open);
        println!("  {}: {}", t!("config_open_type"), old_conf.openType);
        println!("  {}: {}", t!("config_key_up"), old_conf.up);
        println!("  {}: {}", t!("config_key_down"), old_conf.down);
        println!("  {}: {}", t!("config_key_left"), old_conf.left);
        println!("  {}: {}", t!("config_key_right"), old_conf.right);
        
        // 打印认证记录信息
        if has_old_auth && !old_auths.is_empty() {
            println!("- {}: {}", t!("auth_records_info"), old_auths.len());
        }
        
        // 询问用户确认
        print(t!("ask_migrate"));
        let mut input = String::new();
        io::stdin().read_line(&mut input).unwrap();
        
        if input.to_lowercase().trim() == "y" || input.to_lowercase().trim() == "yes" 
           || input.trim().is_empty() { // 默认为是
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
                        warning(format!("保存新格式配置失败: {}", e));
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
                    warning(format!("序列化TOML配置失败: {}", e));
                }
            }
            
            return Some(old_conf);
        } else {
            // 用户拒绝迁移，提示必须迁移并退出程序
            error(t!("migrate_required"));
            std::process::exit(1);
        }
    }
    
    None
}

/// 保存配置到文件
pub async fn save_config(str: &str, sync: bool) {
    // 兼容旧的API，先尝试解析为AppConfig
    let app_config = match toml::from_str::<AppConfig>(str) {
        Ok(config) => config,
        Err(_) => {
            // 如果无法解析为AppConfig，可能是旧的JSON字符串
            let old_config: Config = match serde_json::from_str(str) {
                Ok(config) => config,
                Err(e) => {
                    error(format!("无法解析配置: {}", e));
                    return;
                }
            };
            AppConfig::from(old_config)
        }
    };

    // 使用TOML格式保存配置
    match toml::to_string_pretty(&app_config) {
        Ok(toml_content) => {
            match fs::write(CONF_PATH, toml_content) {
                Ok(_) => match sync {
                    true => {
                        info(t!("info_sync_complete"));
                        std::process::exit(0);
                    }
                    false => info(t!("info_conf_saved")),
                },
                Err(_) => match sync {
                    true => {
                        error(t!("err_sync_failed"));
                        std::process::exit(0);
                    }
                    false => error(t!("err_conf_save_failed")),
                },
            }
        },
        Err(e) => {
            error(format!("{}：{}", t!("err_conf_serialize_failed"), e));
        }
    }
}
