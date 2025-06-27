use std::io;
use std::time::{SystemTime, UNIX_EPOCH};

use fast_qr::qr::QRBuilder;
use rand::{distributions::Alphanumeric, Rng};
use serde_json::Value;
use tokio::io::{AsyncReadExt, AsyncWriteExt, Result};
use tokio::net::{TcpListener, TcpStream};

use crate::data::*;
use crate::modules::auth::{is_client_authenticated, load_auth, save_auth};
use crate::modules::config::{load_config, save_config, AUTH_TIMEOUT};
use crate::modules::input::{input, macros};
use crate::tool::*;
use rust_i18n::t;

/// 服务器版本
pub const VERSION: &str = env!("CARGO_PKG_VERSION");
/// 缓冲区大小
const BUFFER_SIZE: usize = 4096;
/// 错误消息键
const ERR_INVALID_REQUEST: &str = "err_invalid_request";
/// 服务器API版本
const SERVER_API_VERSION: u64 = 6;

/// 启动服务器
pub async fn run(mut debug: bool, mut disable_auth: bool) -> Result<()> {
    // 检查区域设置
    if sys_locale::get_locale().unwrap().as_str() == "zh-CN"
        || sys_locale::get_locale().unwrap().as_str() == "zh"
    {
        rust_i18n::set_locale("zh-CN");
    } else {
        rust_i18n::set_locale("en");
    }

    println(format!("{}{VERSION}{}", t!("title_1"), t!("title_2")));

    // 加载配置
    let conf: AppConfig = match load_config().await {
        Some(s) => {
            info(t!("info_conf_loaded"));
            s
        }
        None => {
            warning(t!("warn_conf_load_failed"));
            let app_config = AppConfig::default();
            save_config(&app_config, false).await;
            app_config
        }
    };

    // 调试模式
    debug = debug || conf.debug;
    if debug {
        warning(format!("{}", t!("debug_mode")));
    }

    // 认证模式
    if disable_auth {
        warning(format!("{}", t!("auth_disabled")));
    }

    // 显示配置信息
    display_config(&conf);

    // 检查认证数据
    disable_auth = disable_auth || !conf.auth.enabled;
    if !disable_auth {
        let current_time = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs();
        let auths = (load_auth().await).unwrap_or_default();

        let auth_timeout = conf.auth.timeout * 60 * 60 * 24;

        let filtered = auths
            .into_iter()
            .filter(|x| x.time.abs_diff(current_time) <= auth_timeout)
            .collect::<Vec<Auth>>();
        save_auth(&filtered).await;
    }

    // 获取IP地址
    let mut ip: String = if conf.server.ip.is_empty() {
        local_ipaddress::get().unwrap()
    } else {
        if debug {
            warning(format!("{}{}", t!("d_specific_ip"), conf.server.ip.clone()));
        }
        conf.server.ip.clone()
    };

    // 监听端口
    let listener = match TcpListener::bind(format!("{}:{}", ip, conf.server.port)).await {
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
    display_qr_code(&ip, conf.server.port);

    // 处理连接
    loop {
        let (client, _address) = listener.accept().await?;
        tokio::spawn(handle_connection(client, conf.clone(), debug, disable_auth));
    }
}

/// 显示配置信息
fn display_config(conf: &AppConfig) {
    info(t!("n_conf_title"));
    println!(" {:40}{}", t!("n_conf_input"), t!("n_conf_advanced"));
    println!(
        " {:40}{}",
        format!(" ├─{}{}", t!("n_conf_input_up"), conf.input.up),
        format!(
            " ├─{}{}",
            t!("n_conf_ip"),
            if !conf.server.ip.is_empty() {
                conf.server.ip.clone()
            } else {
                t!("n_conf_ip_auto").to_string()
            }
        )
    );
    println!(
        " {:40}{}",
        format!(" ├─{}{}", t!("n_conf_input_down"), conf.input.down),
        format!(
            " ├─{}{}",
            t!("n_conf_auth"),
            if conf.auth.enabled {
                t!("enabled")
            } else {
                t!("disabled")
            }
        )
    );
    println!(
        " {:40}{}",
        format!(" ├─{}{}", t!("n_conf_input_left"), conf.input.left),
        format!(
            " ├─{}{}{}",
            t!("n_conf_auth_timeout"),
            conf.auth.timeout,
            t!("unit_days")
        )
    );
    println!(
        " {:40}{}",
        format!(" ├─{}{}", t!("n_conf_input_right"), conf.input.right),
        format!(
            " └─{}{}",
            t!("n_conf_debug"),
            if conf.debug {
                t!("enabled")
            } else {
                t!("disabled")
            }
        )
    );
    println!(
        " {:40}",
        format!(" ├─{}{}", t!("n_conf_input_open"), conf.input.open),
    );
    println!(
        " {:40}",
        format!(" ├─{}{}", t!("n_conf_type_open"), conf.input.keytype),
    );
    println!(
        " {:40}",
        format!(" └─{}{}{}", t!("n_conf_input_delay"), conf.input.delay, t!("unit_ms"))
    );
    println!(
        " {:40}",
        format!(" {}{}", t!("n_conf_port"), conf.server.port)
    );
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
pub async fn handle_connection(
    mut client: TcpStream,
    conf: AppConfig,
    debug: bool,
    disable_auth: bool,
) -> Result<()> {
    let mut is_authed = false;
    let mut buffer = vec![0; BUFFER_SIZE];
    let mut api_ver: u64 = SERVER_API_VERSION;
    let mut api_ver_logged = false;

    let token: String = rand::thread_rng()
        .sample_iter(&Alphanumeric)
        .take(16)
        .map(char::from)
        .collect();

    println!();
    info(format!("{}{}", t!("info_connect"), client.peer_addr()?));

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
                api_ver = json["api"].as_u64().unwrap_or(5);
                if !api_ver_logged && api_ver != SERVER_API_VERSION {
                    warning(format!(
                        "{}{}{}",
                        t!("warn_api_compatible_1"),
                        api_ver,
                        t!("warn_api_compatible_2")
                    ));
                    api_ver_logged = true;
                }
                match api_ver {
                    5 => handle_status_api5(&mut client, is_authed, debug).await?,
                    _ => handle_status(&mut client, is_authed, debug).await?,
                }
            }
            Operation::Auth => {
                handle_auth(
                    &mut client,
                    json,
                    &mut is_authed,
                    debug,
                    &token,
                    disable_auth,
                )
                .await?;
            }
            _ => {
                // 所有其他操作都需要认证
                if is_authed && client_token == token {
                    match opt {
                        Operation::Request => handle_request(&mut client, &conf, debug).await?,
                        Operation::Sync => match api_ver {
                            5 => handle_sync_api5(&mut client, json, &conf).await?,
                            _ => handle_sync(&mut client, json, &conf).await?,
                        },
                        Operation::Macro => macros(json["macro"].clone(), &conf).await?,
                        Operation::Input => input(json["input"].clone(), &conf).await?,
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
    let res: String;

    // 检查认证
    if is_authed {
        res = format!(
            "{{\"api\":{SERVER_API_VERSION},\"status\":{}}}\n",
            Status::Success
        );
    } else {
        res = format!(
            "{{\"api\":{SERVER_API_VERSION},\"status\":{}}}\n",
            Status::Unauthorized
        );
    }

    if debug {
        debug_log(format!(" <<< {}", res));
    }

    client.write_all(res.as_bytes()).await?;
    Ok(())
}

/// 处理状态请求（API5）
async fn handle_status_api5(client: &mut TcpStream, is_authed: bool, debug: bool) -> Result<()> {
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
async fn handle_request(client: &mut TcpStream, conf: &AppConfig, debug: bool) -> Result<()> {
    let res: String = serde_json::to_string(conf).unwrap();

    if debug {
        debug_log(format!(" <<< {}", res));
    }

    client.write_all(res.as_bytes()).await?;
    info(format!("{}{}", t!("info_send_config"), client.peer_addr()?));
    Ok(())
}

/// 处理同步请求
async fn handle_sync(client: &mut TcpStream, json: Value, conf: &AppConfig) -> Result<()> {
    println(format!("{}{}", t!("n_sync_conf"), client.peer_addr()?));
    print(t!("ask_sync"));
    let mut input = String::new();
    io::stdin().read_line(&mut input).unwrap();
    if input.to_lowercase().trim() == "y" || input.to_lowercase().trim() == "yes" {
        match serde_json::from_str::<AppConfig>(json["config"].to_string().as_str()) {
            Ok(mut client_config) => {
                // 判断是否指定ip
                if client_config.server.ip.is_empty() {
                    client_config.server.ip = conf.server.ip.clone();
                }
                // 保留认证信息
                client_config.records = conf.records.clone();

                save_config(&client_config, true).await;
            }
            Err(e) => {
                error(format!("{}{}", t!("err_conf_parse_failed"), e));
            }
        }
    } else {
        warning(t!("warn_reject_sync"));
    }
    Ok(())
}

/// 处理同步请求（API5）
async fn handle_sync_api5(client: &mut TcpStream, json: Value, conf: &AppConfig) -> Result<()> {
    println(format!("{}{}", t!("n_sync_conf"), client.peer_addr()?));
    print(t!("ask_sync"));
    let mut input = String::new();
    io::stdin().read_line(&mut input).unwrap();
    if input.to_lowercase().trim() == "y" || input.to_lowercase().trim() == "yes" {
        match serde_json::from_str::<AppConfig5>(json["config"].to_string().as_str()) {
            Ok(client_config) => {
                // 转换为AppConfig，但保留本地IP和认证信息
                let mut app_config = AppConfig::from(client_config);
                app_config.server.ip = conf.server.ip.clone();
                app_config.records = conf.records.clone();

                save_config(&app_config, true).await;
            }
            Err(e) => {
                error(format!("{}{}", t!("err_conf_parse_failed"), e));
            }
        }
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
    disable_auth: bool,
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
        disable_auth,
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
