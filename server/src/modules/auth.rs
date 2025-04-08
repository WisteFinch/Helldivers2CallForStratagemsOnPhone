use std::fs;

use crate::data::Auth;
use crate::tool::*;
use crate::modules::config::{CONF_PATH, AUTH_PATH};
use rust_i18n::t;
use toml;
use serde_json;
use crate::data::AppConfig;

/// 加载认证数据
pub async fn load_auth() -> Option<Vec<Auth>> {
    // 尝试从TOML配置文件中加载认证数据
    if let Ok(toml_content) = fs::read_to_string(CONF_PATH) {
        match toml::from_str::<AppConfig>(&toml_content) {
            Ok(app_config) => {
                return Some(app_config.auth_records);
            },
            Err(e) => {
                warning(format!("从配置加载认证数据失败: {}", e));
            }
        }
    }
    
    // 尝试从旧的auth.json文件加载认证数据
    let auths = match fs::read_to_string(AUTH_PATH) {
        Ok(ok) => match serde_json::from_str(&ok) {
            Ok(s) => s,
            Err(_) => return None,
        },
        Err(_) => return None,
    };

    Some(auths)
}

/// 保存认证数据
pub async fn save_auth(auth_data: &[Auth]) {
    // 先加载现有的TOML配置
    if let Ok(toml_content) = fs::read_to_string(CONF_PATH) {
        match toml::from_str::<AppConfig>(&toml_content) {
            Ok(mut app_config) => {
                // 更新认证记录
                app_config.auth_records = auth_data.to_vec();
                
                // 保存回TOML配置文件
                match toml::to_string_pretty(&app_config) {
                    Ok(new_toml_content) => {
                        if let Err(_) = fs::write(CONF_PATH, new_toml_content) {
                            error(t!("err_auth_save_failed"));
                        }
                        return;
                    },
                    Err(_) => {
                        error(t!("err_conf_serialize_failed"));
                    }
                }
            },
            Err(e) => {
                warning(format!("更新认证数据时解析配置失败: {}", e));
            }
        }
    }
    
    // 如果无法从TOML保存，回退到旧方式保存
    match fs::write(AUTH_PATH, serde_json::to_string(auth_data).unwrap_or_default()) {
        Ok(_) => {},
        Err(_) => error(t!("err_auth_save_failed")),
    }
}

/// 检查客户端认证
pub fn is_client_authenticated(
    sid: &str, 
    token: &str, 
    client_token: &str, 
    auths: &[Auth], 
    current_time: u64,
    auth_timeout: u64,
    disable_auth: bool
) -> (bool, bool) {
    // 如果禁用认证，直接返回认证通过
    if disable_auth {
        return (true, false);
    }
    
    // 检查是否已经基于SID认证
    let mut is_auth_by_sid = false;
    let mut is_exist = false;
    
    for auth in auths {
        if auth.sid == sid {
            is_exist = true;
            if current_time.abs_diff(auth.time) <= auth_timeout {
                is_auth_by_sid = true;
            }
            break;
        }
    }
    
    // 如果SID已认证，且客户端提供了匹配的token，则认证成功
    // 如果client_token为"NULL"，则表示这是初始认证请求，不需要token匹配
    let token_match = client_token == "NULL" || client_token == token;
    
    (is_auth_by_sid && token_match, is_exist)
}
