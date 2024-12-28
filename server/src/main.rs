use std::io::Write;
use std::net::{Ipv4Addr, SocketAddr, SocketAddrV4};
use std::str::FromStr;
use std::time::{SystemTime, UNIX_EPOCH};
use std::{fs, io};

use fast_qr::qr::QRBuilder;
use key::KeyFromString as _;
use log::{debug, error, info, warn};
use rand::{distributions::Alphanumeric, Rng};
use rdev::EventType;
use request::{CombinedMacro, IndependentInput, Request};
use rust_i18n::{i18n, t};
use serde::Serialize;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::{TcpListener, TcpStream};
use tokio::time::Duration;

use crate::error::I18NError;
use data::*;
use tool::*;

mod data;
mod error;
mod key;
mod logger;
mod request;
mod tool;

i18n!("src/locales");

type Result<T> = std::result::Result<T, I18NError>;

const CONF_PATH: &str = "./config.json";
const AUTH_PATH: &str = "./auth.json";
const VERSION: &str = env!("CARGO_PKG_VERSION");
const AUTH_TIMEOUT: u64 = 60 * 60 * 24 * 3;

#[derive(Debug)]
struct Session {
    #[allow(dead_code)]
    pub addr: SocketAddr,
    pub token: String,
    pub is_authenticated: bool,
}

impl Session {
    fn new(addr: SocketAddr) -> Self {
        Self {
            addr,
            token: "".to_string(),
            is_authenticated: false,
        }
    }
}

#[derive(Debug, Clone, Serialize)]
#[serde(untagged)]
enum Response {
    Status { status: i32, ver: &'static str },
    Auth { auth: bool, token: Option<String> },
}

impl Response {
    fn into_response(self) -> Result<String> {
        let mut data = serde_json::to_string(&self)?;
        data.push('\n');
        Ok(data)
    }
}

#[tokio::main]
async fn main() -> Result<()> {
    let args: Vec<String> = std::env::args().collect();
    let mut debug = false;
    for arg in args {
        if arg.to_lowercase() == "debug" {
            debug = true;
        }
    }

    // Check locale.
    let locale = sys_locale::get_locale().unwrap();
    if ["zh-CN", "zh"].contains(&locale.as_str()) {
        rust_i18n::set_locale("zh-CN");
    } else {
        rust_i18n::set_locale("en");
    }

    // Print title
    println!("{}{}{}", t!("title_1"), VERSION, t!("title_2"));

    // Initialize logger
    if debug {
        logger::initialize(log::LevelFilter::Debug);
        warn!("{}", t!("debug_mode"));
    } else {
        logger::initialize(log::LevelFilter::Info);
    }

    if let Err(e) = run().await {
        error!("{}", e);
    }
    Ok(())
}

async fn run() -> Result<()> {
    // Load configuration.
    let conf: Config = match load_config().await {
        Some(s) => {
            info!("{}", t!("info_conf_loaded"));
            s
        }
        None => {
            warn!("{}", t!("warn_conf_load_failed"));
            save_config(
                serde_json::to_string(&Config::default()).unwrap().as_str(),
                false,
            )
            .await;
            Config::default()
        }
    };

    // Display config.
    info!(
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
        conf.open_type,
    );

    // Check authentication data
    let current_time = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_secs();
    let auths = (load_auth().await).unwrap_or_default();
    let filtered = auths
        .into_iter()
        .filter(|x| x.time.abs_diff(current_time) <= AUTH_TIMEOUT)
        .collect::<Vec<Auth>>();
    save_auth(serde_json::to_string(&filtered)?.as_str()).await;

    // Get ip address.
    let ip: Ipv4Addr = if conf.ip.is_empty() {
        let ip_string = local_ipaddress::get().unwrap_or("127.0.0.1".to_string());
        Ipv4Addr::from_str(&ip_string).unwrap()
    } else {
        debug!("{}{}", t!("d_specific_ip"), conf.ip);
        match Ipv4Addr::from_str(&conf.ip) {
            Ok(ip) => ip,
            Err(e) => {
                error!("Failed to parse ip address: {}", e);
                warn!("{}", t!("warn_conf_network_temp"));
                let ip_string = local_ipaddress::get().unwrap_or("127.0.0.1".to_string());
                Ipv4Addr::from_str(&ip_string).unwrap()
            }
        }
    };

    let socket_addr: SocketAddrV4 = SocketAddrV4::new(ip, conf.port as u16);

    // Listen port.
    let listener = TcpListener::bind(socket_addr)
        .await
        .map_err(I18NError::BindAddr)?;
    info!("{}{}", t!("info_listening"), listener.local_addr().unwrap());

    // Print QR
    println!();
    println(t!("n_scan_qr_code"));

    let qr_value = serde_json::json!({
        "add": ip,
        "port": conf.port,
    });
    let qrcode = QRBuilder::new(serde_json::to_string(&qr_value)?).build()?;
    println(qrcode.to_str());
    println(t!("n_admin"));
    println!();

    // Handle connection.
    loop {
        let (client, _address) = listener.accept().await?;
        tokio::spawn(handle_connection(client, conf.clone()));
    }
}

async fn handle_connection(mut client: TcpStream, conf: Config) -> Result<()> {
    println!();
    info!("{}{}", t!("info_connect"), client.peer_addr()?);

    let mut session = Session::new(client.peer_addr()?);
    session.token = rand::thread_rng()
        .sample_iter(&Alphanumeric)
        .take(16)
        .map(char::from)
        .collect();
    session.is_authenticated = false;
    debug!("Created session: {:?}", session);

    let mut buffer = vec![0; 4096];

    // Handel client requests.
    loop {
        let size = client.read(&mut buffer).await?;
        if size == 0 {
            info!("{}{}", t!("info_close"), client.peer_addr()?);
            return Ok(());
        }

        let request_raw = match std::str::from_utf8(&buffer[..size]) {
            Ok(r) => r,
            Err(e) => {
                error!("{}", I18NError::InvalidUtf8(e));
                continue;
            }
        };
        debug!(
            " >>> {}",
            &request_raw.strip_suffix('\n').unwrap_or(request_raw)
        );

        // Remove redundant requests.
        let index = request_raw.find('\n').unwrap();
        let request = &request_raw[..index + 1];

        if request_raw.len() != index + 1 {
            debug!("{}{}", t!("d_remove_redundant"), request);
        }

        if let Err(e) = handle_message(&mut client, request, &mut session, &conf).await {
            error!("{}", e);
            continue;
        };
    }
}

async fn handle_message(
    client: &mut TcpStream,
    message: &str,
    session: &mut Session,
    conf: &Config,
) -> Result<()> {
    let addr = client.peer_addr()?;

    let request = Request::from_data(message)?;
    debug!(" Request: {:?}", request);

    match request {
        Request::Status { ver } => {
            // Check version.
            let res = if compare_ver(&ver, VERSION) {
                // Check authentication.
                if session.is_authenticated {
                    Response::Status {
                        status: Status::Success as i32,
                        ver: VERSION,
                    }
                } else {
                    Response::Status {
                        status: Status::Unauthorized as i32,
                        ver: VERSION,
                    }
                }
            } else {
                warn!("{}{ver}{}", t!("warn_ver_1"), t!("warn_ver_2"));
                Response::Status {
                    status: Status::VersionMismatch as i32,
                    ver: VERSION,
                }
            };

            let res_data = res.into_response()?;

            debug!(
                " <<< {}",
                res_data.strip_suffix('\n').unwrap_or(res_data.as_str())
            );

            client.write_all(res_data.as_bytes()).await?;
        }
        Request::Request => {
            // if session.is_authenticated && client_token == session.token {
            //     let res: String = serde_json::to_string(&conf).unwrap();

            //     debug!(" <<< {}", res);

            //     client.write_all(res.as_bytes()).await?;
            //     info!("{}{}", t!("info_send_config"), addr)
            // } else {
            //     warn!("{}", t!("warn_reject_request"))
            // }
            unimplemented!()
        }
        Request::Sync {
            config: mut sync_config,
            token,
        } => {
            if session.is_authenticated && token == session.token {
                println(format!("{}{}", t!("n_sync_conf"), addr));
                print(t!("ask_sync"));
                let mut input = String::new();
                io::stdin().read_line(&mut input)?;
                if input.to_lowercase().trim() == "y" || input.to_lowercase().trim() == "yes" {
                    sync_config.ip = conf.ip.clone();
                    save_config(serde_json::to_string(&*sync_config)?.as_str(), true).await
                } else {
                    warn!("{}", t!("warn_reject_sync"));
                }
            } else {
                warn!("{}", t!("warn_reject_request"))
            }
        }
        Request::Combined { r#macro, token } => {
            if session.is_authenticated && token == session.token {
                macros(r#macro, conf).await?
            } else {
                warn!("{}", t!("warn_reject_request"))
            }
        }
        Request::Independent { input, token } => {
            if session.is_authenticated && token == session.token {
                independent(input, conf).await?
            } else {
                warn!("{}", t!("warn_reject_request"))
            }
        }
        Request::Auth { sid } => {
            let current_time = SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_secs();
            let mut auths = (load_auth().await).unwrap_or_default();

            // Check if authentication is available.
            let mut is_exist = false;
            for v in &auths {
                if v.sid == sid && current_time.abs_diff(v.time) <= AUTH_TIMEOUT {
                    session.is_authenticated = true;
                    is_exist = true;
                }
            }

            // Ask user for authentication.
            if !session.is_authenticated {
                println(format!(
                    "{}{}{}{}",
                    t!("n_auth_1"),
                    addr,
                    t!("n_auth_2"),
                    sid
                ));
                print(t!("ask_auth"));
                let mut input = String::new();
                io::stdin().read_line(&mut input).unwrap();
                if input.to_lowercase().trim() == "y" || input.to_lowercase().trim() == "yes" {
                    session.is_authenticated = true;
                    info!("{}", t!("info_auth"));
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
                    warn!("{}", t!("warn_reject_auth"));
                }
            }

            // Send token to client.
            let res = if session.is_authenticated {
                save_auth(serde_json::to_string(&auths).unwrap().as_str()).await;
                Response::Auth {
                    auth: session.is_authenticated,
                    token: Some(session.token.clone()),
                }
            } else {
                Response::Auth {
                    auth: session.is_authenticated,
                    token: None,
                }
            };

            let res_data = res.into_response()?;

            debug!(
                " <<< {}",
                res_data.strip_suffix('\n').unwrap_or(res_data.as_str())
            );

            client.write_all(res_data.as_bytes()).await?;
        }
    }
    Ok(())
}

async fn load_config() -> Option<Config> {
    let conf: Config = match fs::read_to_string(CONF_PATH) {
        Ok(ok) => match serde_json::from_str(&ok) {
            Ok(s) => s,
            Err(_) => return None,
        },
        Err(_) => return None,
    };

    Some(conf)
}

async fn load_auth() -> Option<Vec<Auth>> {
    let auths = match fs::read_to_string(AUTH_PATH) {
        Ok(ok) => match serde_json::from_str(&ok) {
            Ok(s) => s,
            Err(_) => return None,
        },
        Err(_) => return None,
    };

    Some(auths)
}

async fn save_config(str: &str, sync: bool) {
    match fs::write(CONF_PATH, str) {
        Ok(_) => match sync {
            true => {
                info!("{}", t!("info_sync_complete"));
                std::process::exit(0);
            }
            false => info!("{}", t!("info_conf_saved")),
        },
        Err(_) => match sync {
            true => {
                error!("{}", t!("err_sync_failed"));
                std::process::exit(0);
            }
            false => error!("{}", t!("err_conf_save_failed")),
        },
    }
}

async fn save_auth(str: &str) {
    if let Err(e) = fs::write(AUTH_PATH, str) {
        error!("{} {}", t!("err_auth_save_failed"), e);
    }
}

async fn macros(macros: CombinedMacro, conf: &Config) -> Result<()> {
    let name = macros.name;
    let list = macros.steps;

    // Press open
    print(format!("{name}: "));
    if conf.open_type == "hold" {
        execute(Step::Open, InputType::Press, conf).await.unwrap();
    } else if conf.open_type == "long_press" {
        execute(Step::Open, InputType::Press, conf).await.unwrap();
        tokio::time::sleep(Duration::from_millis(400)).await;
        execute(Step::Open, InputType::Release, conf).await.unwrap();
    } else if conf.open_type == "double_tap" {
        execute(Step::Open, InputType::Click, conf).await.unwrap();
        execute(Step::Open, InputType::Click, conf).await.unwrap();
    } else {
        execute(Step::Open, InputType::Click, conf).await.unwrap();
    }

    // Click steps
    for i in list {
        execute(Step::from_u64(i as u64), InputType::Click, conf)
            .await
            .unwrap();
    }

    // Release open
    if conf.open_type == "hold" {
        execute(Step::Open, InputType::Release, conf).await.unwrap();
    }
    println!();

    Ok(())
}

async fn independent(input: IndependentInput, conf: &Config) -> Result<()> {
    let step = Step::from_u64(input.step as u64);
    let t = InputType::from_u64(input.r#type as u64);
    execute(step, t, conf).await.unwrap();

    Ok(())
}

fn simulate_key_event(step: Step, event_type: u32, conf: &Config) {
    let data_str = match step {
        Step::Open => conf.open.as_str(),
        Step::Up => conf.up.as_str(),
        Step::Down => conf.down.as_str(),
        Step::Left => conf.left.as_str(),
        Step::Right => conf.right.as_str(),
    };
    let Some(data) = InputData::from_str(data_str) else {
        // TODO: i18n
        error!("{}", "err_parse_input_failed");
        return;
    };

    // TODO: avoid unwrap
    if event_type == 0 {
        match data {
            InputData::Keyboard(key) => rdev::simulate(&EventType::KeyPress(key)).unwrap(),
            InputData::MouseButton(button) => {
                rdev::simulate(&EventType::ButtonPress(button)).unwrap()
            }
            InputData::WheelUp => rdev::simulate(&EventType::Wheel {
                delta_x: 0,
                delta_y: 1,
            })
            .unwrap(),
            InputData::WheelDown => rdev::simulate(&EventType::Wheel {
                delta_x: 0,
                delta_y: -1,
            })
            .unwrap(),
        }
    } else {
        match data {
            InputData::Keyboard(key) => rdev::simulate(&EventType::KeyRelease(key)).unwrap(),
            InputData::MouseButton(button) => {
                rdev::simulate(&EventType::ButtonRelease(button)).unwrap()
            }
            InputData::WheelUp | InputData::WheelDown => (),
        };
    }
}

async fn execute(step: Step, t: InputType, conf: &Config) -> Result<()> {
    match t {
        // TODO: 应当在解析完成所有step无错误后再开始执行
        InputType::Click => {
            simulate_key_event(step.clone(), 0, conf);
            print(step.clone());
            let _ = io::stdout().flush();
            tokio::time::sleep(Duration::from_millis(conf.delay)).await;
            simulate_key_event(step, 1, conf);
        }
        InputType::Press => {
            simulate_key_event(step, 0, conf);
        }
        InputType::Release => {
            simulate_key_event(step, 1, conf);
        }
        InputType::Begin => {
            print(t!("n_free_input"));
        }
        InputType::End => {
            println!();
        }
    }
    tokio::time::sleep(Duration::from_millis(conf.delay)).await;

    Ok(())
}
