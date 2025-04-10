use std::time::{SystemTime, UNIX_EPOCH};
use std::io;

use rand::{distributions::Alphanumeric, Rng};
use fast_qr::qr::QRBuilder;
use tokio::io::{AsyncReadExt, AsyncWriteExt, Result};
use tokio::net::{TcpListener, TcpStream};
use serde_json::Value;

use crate::data::*;
use crate::tool::*;
use rust_i18n::t;
use crate::modules::config::{AUTH_TIMEOUT, load_config, save_config};
use crate::modules::auth::{load_auth, save_auth, is_client_authenticated};
use crate::modules::input::{macros, independent};

/// 服务器版本
pub const VERSION: &str = env!("CARGO_PKG_VERSION");
/// 缓冲区大小
const BUFFER_SIZE: usize = 4096;
/// 错误消息键
const ERR_INVALID_REQUEST: &str = "err_invalid_request";

/// 启动服务器
pub async fn run(debug: bool, disable_auth: bool) -> Result<()> {
    // 检查区域设置
    if sys_locale::get_locale().unwrap().as_str() == "zh-CN" || sys_locale::get_locale().unwrap().as_str() == "zh" {
        rust_i18n::set_locale("zh-CN");
    } else {
        rust_i18n::set_locale("en");
    }

    println(format!("{}{VERSION}{}", t!("title_1"), t!("title_2")));

    // 调试模式
    if debug {
        warning(format!("{}", t!("debug_mode")));
    }

    // 认证模式
    if disable_auth {
        warning(format!("{}", t!("auth_disabled")));
    }

    // 加载配置
    let conf: Config = match load_config().await {
        Some(s) => {
            info(t!("info_conf_loaded"));
            s
        }
        None => {
            warning(t!("warn_conf_load_failed"));
            let app_config = AppConfig::default();
            save_config(toml::to_string_pretty(&app_config).unwrap().as_str(), false).await;
            Config::from(app_config)
        }
    };

    // 显示配置信息
    display_config(&conf);

    // 检查认证数据
    let current_time = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_secs();
    let auths = (load_auth().await).unwrap_or_default();
    
    // 使用默认的超时时间
    let auth_timeout = AUTH_TIMEOUT;
    
    let filtered = auths
        .into_iter()
        .filter(|x| x.time.abs_diff(current_time) <= auth_timeout)
        .collect::<Vec<Auth>>();
    save_auth(&filtered).await;

    // 获取IP地址
    let mut ip: String = if conf.ip.is_empty() {
        local_ipaddress::get().unwrap()
    } else {
        if debug {
            warning(format!("{}{}", t!("d_specific_ip"), conf.ip.clone()));
        }
        conf.ip.clone()
    };

    // 监听端口
    let listener = match TcpListener::bind(format!("{}:{}", ip, conf.port)).await {
        Ok(ok) => ok,
        Err(err) => {
            error(err);
            ip = local_ipaddress::get().unwrap();
            warning(t!("warn_conf_network_temp"));
            TcpListener::bind(format!("{}:{}", ip, 0)).await?
        }
    };
    info(format!(
        "{}{}",
        t!("info_listening"),
        listener.local_addr().unwrap()
    ));

    // 显示二维码
    display_qr_code(&ip, conf.port);

    // 处理连接
    loop {
        let (client, _address) = listener.accept().await?;
        tokio::spawn(handle_connection(client, conf.clone(), debug, disable_auth));
    }
}

/// 显示配置信息
fn display_config(conf: &Config) {
    info(format!(
        "{}\n  {}\n    {}{}\n    {}{}\n    {}{}\n    {}{}\n    {}{}\n    {}{}\n  {}\n    {}{}",
        t!("n_conf_title"),
        t!("n_conf_input"),
        t!("n_conf_input_delay"),
        conf.delay.clone(),
        t!("n_conf_input_open"),
        conf.open,
        t!("n_conf_input_up"),
        conf.up,
        t!("n_conf_input_down"),
        conf.down,
        t!("n_conf_input_left"),
        conf.left,
        t!("n_conf_input_right"),
        conf.right,
        t!("n_conf_type"),
        t!("n_conf_type_open"),
        conf.openType,
    ));
}

/// 显示QR码
fn display_qr_code(ip: &str, port: u64) {
    println!();
    println(t!("n_scan_qr_code"));
    let qrcode = QRBuilder::new(format!("{{\"add\":\"{}\",\"port\":{}}}", ip, port))
        .build()
        .unwrap();
    println(qrcode.to_str());
    println(t!("n_admin"));
    println!();
}

/// 处理客户端连接
pub async fn handle_connection(mut client: TcpStream, conf: Config, debug: bool, disable_auth: bool) -> Result<()> {
    let mut is_authed = false;
    let token: String = rand::thread_rng()
        .sample_iter(&Alphanumeric)
        .take(16)
        .map(char::from)
        .collect();

    println!();
    info(format!("{}{}", t!("info_connect"), client.peer_addr()?));
    let mut buffer = vec![0; BUFFER_SIZE];

    // 处理客户端请求
    loop {
        let size = client.read(&mut buffer).await?;
        if size == 0 {
            info(format!("{}{}", t!("info_close"), client.peer_addr()?));
            return Ok(());
        }
        let request_raw = std::str::from_utf8(&buffer[..size]).unwrap();

        // 显示调试日志
        if debug {
            debug_log(format!(" >>> {}", &request_raw));
        }

        // 移除冗余请求
        let index = match request_raw.find('\n') {
            Some(idx) => idx,
            None => {
                error(t!(ERR_INVALID_REQUEST));
                warning(format!("{}{}", t!("warn_force_close"), client.peer_addr()?));
                return Ok(());
            }
        };
        
        let request = &request_raw[..index + 1];
        if debug && request_raw.len() != index + 1 {
            debug_log(format!("{}{}", t!("d_remove_redundant"), request));
        }

        // 解析JSON
        let json: Value = match serde_json::from_str(request) {
            Ok(ok) => ok,
            Err(_) => {
                error(t!("err_parse_json_failed"));
                warning(format!("{}{}", t!("warn_force_close"), client.peer_addr()?));
                return Ok(());
            }
        };
        let opt: Operation = match json["opt"].as_u64() {
            Some(s) => Operation::from_u64(s),
            None => {
                error(t!("err_parse_opt_failed"));
                warning(format!("{}{}", t!("warn_force_close"), client.peer_addr()?));
                return Ok(());
            }
        };

        // 获取客户端令牌
        let client_token = json["token"].as_str().unwrap_or("NULL");
        
        // 检查操作和权限
        match opt {
            Operation::Status => {
                handle_status(&mut client, is_authed, debug).await?;
            }
            Operation::Auth => {
                handle_auth(&mut client, json, &mut is_authed, debug, &token, disable_auth).await?;
            }
            _ => {
                // 所有其他操作都需要认证
                if is_authed && client_token == token {
                    match opt {
                        Operation::Request => handle_request(&mut client, &conf, debug).await?,
                        Operation::Sync => handle_sync(&mut client, json, &conf).await?,
                        Operation::Combined => macros(json["macro"].clone(), &conf).await?,
                        Operation::Independent => independent(json["input"].clone(), &conf).await?,
                        _ => {} // 已经处理的Status和Auth操作
                    }
                } else {
                    warning(t!("warn_reject_request"));
                }
            }
        }
    }
}

/// 处理状态请求
async fn handle_status(client: &mut TcpStream, is_authed: bool, debug: bool) -> Result<()> {
    // 版本检查总是通过
    let res: String;
    
    // 检查认证
    if is_authed {
        res = format!("{{\"status\":{},\"ver\":{VERSION}}}\n", Status::Success);
    } else {
        res = format!(
            "{{\"status\":{},\"ver\":{VERSION}}}\n",
            Status::Unauthorized
        );
    }

    if debug {
        debug_log(format!(" <<< {}", res));
    }

    client.write_all(res.as_bytes()).await?;
    Ok(())
}

/// 处理配置请求
async fn handle_request(client: &mut TcpStream, conf: &Config, debug: bool) -> Result<()> {
    let res: String = serde_json::to_string(conf).unwrap();

    if debug {
        debug_log(format!(" <<< {}", res));
    }

    client.write_all(res.as_bytes()).await?;
    info(format!("{}{}", t!("info_send_config"), client.peer_addr()?));
    Ok(())
}

/// 处理同步请求
async fn handle_sync(client: &mut TcpStream, json: Value, conf: &Config) -> Result<()> {
    println(format!("{}{}", t!("n_sync_conf"), client.peer_addr()?));
    print(t!("ask_sync"));
    let mut input = String::new();
    io::stdin().read_line(&mut input).unwrap();
    if input.to_lowercase().trim() == "y" || input.to_lowercase().trim() == "yes" {
        let client_config: Config =
            serde_json::from_str(json["config"].to_string().as_str())
                .unwrap_or_default();
        
        // 转换为AppConfig，但保留本地IP
        let mut app_config = AppConfig::from(client_config);
        app_config.server.ip = conf.ip.clone();
        
        save_config(toml::to_string_pretty(&app_config).unwrap().as_str(), true).await;
    } else {
        warning(t!("warn_reject_sync"));
    }
    Ok(())
}

/// 处理认证请求
async fn handle_auth(
    client: &mut TcpStream, 
    json: Value, 
    is_authed: &mut bool,
    debug: bool,
    token: &str,
    disable_auth: bool
) -> Result<()> {
    let sid = json["sid"].as_str().unwrap_or("NULL");
    let client_token = json["token"].as_str().unwrap_or("NULL");
    let current_time = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_secs();
    let mut auths = (load_auth().await).unwrap_or_default();

    // 使用is_client_authenticated函数检查认证
    let (auth_result, is_exist) = is_client_authenticated(
        sid, 
        token, 
        client_token, 
        &auths, 
        current_time,
        AUTH_TIMEOUT,
        disable_auth
    );
    
    // 如果禁用认证或已通过认证
    if auth_result {
        *is_authed = true;
        if disable_auth {
            info(t!("info_auth_disabled"));
        }
    } else {
        // 要求用户认证
        println(format!(
            "{}{}{}{}",
            t!("n_auth_1"),
            client.peer_addr()?,
            t!("n_auth_2"),
            sid
        ));
        print(t!("ask_auth"));
        let mut input = String::new();
        io::stdin().read_line(&mut input).unwrap();
        if input.to_lowercase().trim() == "y" || input.to_lowercase().trim() == "yes" {
            *is_authed = true;
            info(t!("info_auth"));
            if is_exist {
                for v in &mut auths {
                    if v.sid == sid {
                        v.time = current_time;
                    }
                }
            } else {
                auths.push(Auth {
                    sid: sid.to_string(),
                    time: current_time,
                });
            }
        } else {
            warning(t!("warn_reject_auth"));
        }
    }

    // 向客户端发送令牌
    let res: String;
    if *is_authed {
        res = format!(
            "{{\"auth\":{},\"token\":\"{}\"}}\n",
            is_authed.clone(),
            token
        );
        if !disable_auth {
            save_auth(&auths).await;
        }
    } else {
        res = format!("{{\"auth\":{}}}\n", is_authed.clone());
    }

    if debug {
        debug_log(format!(" <<< {}", res));
    }

    client.write_all(res.as_bytes()).await?;
    Ok(())
}

/// 比较版本号
pub fn compare_ver(_ver1: &str, _ver2: &str) -> bool {
    // 不再检查版本，允许任何版本连接
    true
}
