use data::*;
use rdev::{simulate, EventType};
use serde_json::Value;
use sys_locale::get_locale;
use std::io::Write;
use std::{fs, io};
use tokio::io::{AsyncReadExt, AsyncWriteExt, Result};
use tokio::net::{TcpListener, TcpStream};
use tokio::time::{sleep, Duration};
use std::time::{SystemTime, UNIX_EPOCH};
use tool::*;
use rand::{distributions::Alphanumeric, Rng};
use fast_qr::qr::QRBuilder;
use rust_i18n::{i18n, t};

pub mod data;
pub mod tool;

i18n!("src/locales");

const CONF_PATH: &str = "./config.json";
const AUTH_PATH: &str = "./auth.json";
const VERSION: &str = env!("CARGO_PKG_VERSION");
const AUTH_TIMEOUT: u64 = 259200;

pub async fn run() -> Result<()> {
    // Check locale.
    if get_locale().unwrap().as_str() == "zh-CN" {
        rust_i18n::set_locale("zh-CN");
    } else {
        rust_i18n::set_locale("zh-CN");
    }

    println(format!("{}{VERSION}{}", t!("title_1"), t!("title_2")));
    // Load configuration.
    let conf: Config = match load_config().await
    {
        Some(s) => {
            info(t!("info_conf_loaded"));
            s
        }
        None => {
            warning(t!("warn_conf_load_failed"));
            save_config(
                serde_json::to_string(&Config::default()).unwrap().as_str(),
                false,
            )
            .await;
            Config::default()
        }
    };

    // Check authentication data
    let current_time = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_secs();
    let auths = match load_auth().await {
        Some(s) => s,
        None => vec![]
    };
    let filtered = auths.into_iter()
        .filter(|x| x.time.abs_diff(current_time) <= AUTH_TIMEOUT)
        .collect::<Vec<Auth>>();
    save_auth(serde_json::to_string(&filtered).unwrap().as_str()).await;

    // Listen port.
    let listener =
        match TcpListener::bind(format!("{}:{}", local_ipaddress::get().unwrap(), conf.port)).await
        {
            Ok(ok) => ok,
            Err(err) => {
                error(err);
                warning(t!("warn_conf_network_temp"));
                TcpListener::bind(format!(
                    "{}:{}",
                    local_ipaddress::get().unwrap(),
                    0
                ))
                .await?
            }
        };
    info(format!(
        "{}{}",
        t!("info_listening"),
        listener.local_addr().unwrap()
    ));

    // Print QR
    println!();
    println(t!("n_scan_qr_code"));
    let qrcode = QRBuilder::new(format!("{{\"add\":\"{}\",\"port\":{}}}",
            local_ipaddress::get().unwrap(),
            conf.port
        ))
        .build()
        .unwrap();
    println(qrcode.to_str());
    println!();

    // Handle connection.
    loop {
        let (client, _address) = listener.accept().await?;
        tokio::spawn(handle_connection(client, conf.clone()));
    }
}

async fn handle_connection(mut client: TcpStream, conf: Config) -> Result<()> {
    let mut is_authed = false;
    let token: String = rand::thread_rng()
        .sample_iter(&Alphanumeric)
        .take(16)
        .map(char::from)
        .collect();

    println!();
    info(format!(
        "{}{}",
        t!("info_connect"),
        client.peer_addr()?
    ));
    let mut buffer = vec![0; 4096];

    // Handel client requests.
    loop {
        let size = client.read(&mut buffer).await?;
        if size == 0 {
            info(format!(
                "{}{}",
                t!("info_close"),
                client.peer_addr()?
            ));
            return Ok(());
        }

        // Debug.
        // println!("{}", std::str::from_utf8(&buffer[..size]).unwrap());

        // Parsing json.
        let json: Value = match serde_json::from_str(std::str::from_utf8(&buffer[..size]).unwrap())
        {
            Ok(ok) => ok,
            Err(_) => {
                error(t!("err_parse_json_failed"));
                warning(format!("{}{}", t!("warn_force_close"), client.peer_addr()?));
                return Ok(());
            }
        };
        let opt: Operation = match json["opt"].as_u64()
        {
            Some(s) => Operation::from_u64(s),
            None => {
                error(t!("err_parse_opt_failed"));
                warning(format!("{}{}", t!("warn_force_close"), client.peer_addr()?));
                return Ok(());
            }
        };

        // Analyse operation.
        let client_token = json["token"].as_str().unwrap_or("NULL");
        match opt {
            Operation::Status => {
                // Check version.
                let ver = json["ver"].as_str().unwrap_or("NULL");
                if compare_ver(ver, VERSION) {
                    // Check authentication.
                    if is_authed {
                        client
                        .write_all(format!("{{\"status\":{},\"ver\":{VERSION}}}\n", Status::Success).as_bytes())
                        .await?;
                    } else {
                        client
                        .write_all(format!("{{\"status\":{},\"ver\":{VERSION}}}\n", Status::Unauthorized).as_bytes())
                        .await?;
                    }
                } else {
                    warning(format!("{}{ver}{}", t!("warn_ver_1"), t!("warn_ver_2")));
                    client
                    .write_all(format!("{{\"status\":{},\"ver\":{VERSION}}}\n", Status::VersionMismatch).as_bytes())
                    .await?;
                }
            }
            Operation::Request => {
                if is_authed && client_token == token {
                    client
                    .write_all(serde_json::to_string(&conf).unwrap().as_bytes())
                    .await?;
                    info(format!("{}{}", t!("info_send_config"), client.peer_addr()?))
                } else {
                    warning(t!("warn_reject_request"))
                }
            }
            Operation::Sync => {
                if is_authed && client_token == token {
                    println(format!("{}{}", t!("n_sync_conf"), client.peer_addr()?));
                    print(t!("ask_sync"));
                    let mut input = String::new();
                    io::stdin().read_line(&mut input).unwrap();
                    if input.to_lowercase().trim() == "y" || input.to_lowercase().trim() == "yes" {
                        save_config(json["config"].to_string().as_str(), true).await
                    } else {
                        warning(t!("warn_reject_sync"));
                    }
                } else {
                    warning(t!("warn_reject_request"))
                }
            },
            Operation::Combined => {
                if is_authed && client_token == token {
                    macros(json["macro"].clone(), &conf).await?
                } else {
                    warning(t!("warn_reject_request"))
                }
            }
            Operation::Independent => {
                if is_authed && client_token == token {
                    independent(json["input"].clone(), &conf).await?
                } else {
                    warning(t!("warn_reject_request"))
                }
            }
            Operation::Auth => {
                let sid = json["sid"].as_str().unwrap_or("NULL");
                let current_time = SystemTime::now()
                    .duration_since(UNIX_EPOCH)
                    .unwrap()
                    .as_secs();
                let mut auths = match load_auth().await {
                    Some(s) => s,
                    None => vec![]
                };

                // Check if authentication is available.
                let mut is_exist = false;
                for v in &auths {
                    if v.sid == sid && current_time.abs_diff(v.time) <= AUTH_TIMEOUT {
                        is_authed = true;
                        is_exist = true;
                    }
                }

                // Ask user for authentication.
                if !is_authed {
                    println(format!("{}{}{}{}", t!("n_auth_1"), client.peer_addr()?, t!("n_auth_2"), sid));
                    print(t!("ask_auth"));
                    let mut input = String::new();
                    io::stdin().read_line(&mut input).unwrap();
                    if input.to_lowercase().trim() == "y" || input.to_lowercase().trim() == "yes" {
                        is_authed = true;
                        info(t!("info_auth"));
                        if is_exist {
                            for v in &mut auths {
                                if v.sid == sid {
                                    v.time = current_time;
                                }
                            }
                        } else {
                            auths.push(Auth{sid: sid.to_string(), time: current_time});
                        }
                    } else {
                        warning(t!("warn_reject_auth"));
                    }
                }

                // Send token to client.
                if is_authed {
                    client
                        .write_all(format!("{{\"auth\":{},\"token\":{}}}\n", is_authed.clone(), token).as_bytes())
                        .await?;
                    save_auth(serde_json::to_string(&auths.clone()).unwrap().as_str()).await
                } else {
                    client
                        .write_all(format!("{{\"auth\":{}}}\n", is_authed.clone()).as_bytes())
                        .await?;
                }
            },
        }
    }
}

async fn load_config() -> Option<Config> {
    let conf: Config = match fs::read_to_string(CONF_PATH)
    {
        Ok(ok) => {
            match serde_json::from_str(&ok) {
                Ok(s) => s,
                Err(_) => return None
            }
        }
        Err(_) => return None,
    };

    Some(conf)
}

async fn load_auth() -> Option<Vec<Auth>> {
    let auths = match fs::read_to_string(AUTH_PATH)
    {
        Ok(ok) => match serde_json::from_str(&ok) {
            Ok(s) => s,
            Err(_) => return None
        }
        Err(_) => return None,
    };

    Some(auths)
}

async fn save_config(str: &str, sync: bool) {
    match fs::write(CONF_PATH, str) {
        Ok(_) => match sync {
            true => {
                info(t!("info_sync_complete"));
                std::process::exit(0);
            }
            false => info(t!("info_conf_saved")),
        }
        Err(_) => match sync {
            true => {
                error(t!("err_sync_failed"));
                std::process::exit(0);
            }
            false => error(t!("err_conf_save_failed")),
        }
    }
}

async fn save_auth(str: &str) {
    match fs::write(AUTH_PATH, str) {
        Ok(_) => {}
        Err(_) => error(t!("err_auth_save_failed")),
    }
}

async fn macros(value: Value, conf: &Config) -> Result<()> {
    let name = value["name"].as_str().unwrap_or("");
    let list = match value["steps"].as_array()
    {
        Some(s) => s,
        None => {
            error(t!("err_parse_step_failed"));
            return Ok(());
        }
    };

    // Press open
    print(format!("{name}: "));
    execute(Step::Open, InputType::Press, conf).await.unwrap();

    // Click steps
    for i in list {
        execute(Step::from_u64(i.as_u64().unwrap()), InputType::Click, conf)
            .await
            .unwrap();
    }

    // Release open
    execute(Step::Open, InputType::Release, conf).await.unwrap();
    println!();

    Ok(())
}

async fn independent(value: Value, conf: &Config) -> Result<()> {
    let step = match value["step"].as_u64()
    {
        Some(s) => Step::from_u64(s),
        None => {
            error(t!("err_parse_step_failed"));
            return Ok(());
        }
    };
    let t = match value["type"].as_u64()
    {
        Some(s) => InputType::from_u64(s),
        None => {
            error(t!("err_parse_input_failed"));
            return Ok(());
        }
    };
    execute(step, t, conf).await.unwrap();

    Ok(())
}

fn simulate_key_event(step: Step, event_type: u32, conf: &Config)
{
    let data = match step
    {
        Step::Open => conf.open.clone().to_key(),
        Step::Up => conf.up.clone().to_key(),
        Step::Down => conf.down.clone().to_key(),
        Step::Left => conf.left.clone().to_key(),
        Step::Right => conf.right.clone().to_key(),
    };
    if event_type == 0 {
        match data.key_type {
            KeyType::Keyboard => simulate(&EventType::KeyPress(data.keyboard)).unwrap(),
            KeyType::MouseButton => simulate(&EventType::ButtonPress(data.mouse_button)).unwrap(),
            KeyType::WheelUp => simulate(&EventType::Wheel { delta_x: 0, delta_y: 1 }).unwrap(),
            KeyType::WheelDown => simulate(&EventType::Wheel { delta_x: 0, delta_y: -1 }).unwrap()
        }
    } else {
        match data.key_type {
            KeyType::Keyboard => simulate(&EventType::KeyRelease(data.keyboard)).unwrap(),
            KeyType::MouseButton => simulate(&EventType::ButtonRelease(data.mouse_button)).unwrap(),
            KeyType::WheelUp => (),
            KeyType::WheelDown => ()
        }
    }
}

async fn execute(step: Step, t: InputType, conf: &Config) -> Result<()> {
    match t {
        InputType::Click => {
            simulate_key_event(step.clone(), 0, conf);
            print(step.clone());
            let _ = io::stdout().flush();
            sleep(Duration::from_millis(conf.delay as u64)).await;
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
    sleep(Duration::from_millis(conf.delay)).await;

    Ok(())
}
