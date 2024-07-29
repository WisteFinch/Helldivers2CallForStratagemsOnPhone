use data::*;
use rdev::{simulate, EventType};
use serde_json::Value;
use std::io::Write;
use std::{fs, io};
use tokio::io::{AsyncReadExt, AsyncWriteExt, Result};
use tokio::net::{TcpListener, TcpStream};
use tokio::time::{sleep, Duration};
use tool::*;

pub mod data;
pub mod tool;

const CONF_PATH: &str = "./config.json";
const VERSION: &str = "0.2.1";

pub async fn run() -> Result<()> {
    println(format!("=== Call for stratagem server v{VERSION} ==="));
    // Load configuration
    let conf: Config = match load_config().await {
        Some(s) => {
            info("Configuration loaded.");
            s
        }
        None => {
            error("Failed to load configuration, loading default configuration!");
            save_config(
                serde_json::to_string(&Config::default()).unwrap().as_str(),
                false,
            )
            .await;
            Config::default()
        }
    };

    // Listen port
    let listener =
        match TcpListener::bind(format!("{}:{}", local_ipaddress::get().unwrap(), conf.port)).await
        {
            Ok(ok) => ok,
            Err(err) => {
                error(err);
                warning("Using temporary network configuration");
                TcpListener::bind(format!(
                    "{}:{}",
                    local_ipaddress::get().unwrap(),
                    0
                ))
                .await?
            }
        };
    info(format!(
        "Listening: {}",
        listener.local_addr().unwrap()
    ));
    loop {
        let (client, _address) = listener.accept().await?;
        tokio::spawn(handle_connection(client, conf.clone()));
    }
}

async fn handle_connection(mut client: TcpStream, conf: Config) -> Result<()> {
    println("");
    info(format!(
        "Established connection to: {}",
        client.peer_addr()?
    ));
    let mut buffer = vec![0; 4096];
    loop {
        let size = client.read(&mut buffer).await?;
        if size == 0 {
            info(format!(
                "Connection closed by client: {}",
                client.peer_addr()?
            ));
            return Ok(());
        }

        // Parsing json
        let json: Value = match serde_json::from_str(std::str::from_utf8(&buffer[..size]).unwrap())
        {
            Ok(ok) => ok,
            Err(_) => {
                error("Failed to parse request data!");
                warning(format!("Connection closed: {}", client.peer_addr()?));
                return Ok(());
            }
        };
        let opt: Operation = match json["opt"].as_u64() {
            Some(s) => Operation::from_u64(s),
            None => {
                error("Failed to parse operation!");
                warning(format!("Connection closed: {}", client.peer_addr()?));
                return Ok(());
            }
        };

        // Operation
        match opt {
            Operation::Status => {
                let ver = json["ver"].as_str().unwrap_or("NULL");
                if ver != VERSION {
                    warning(format!("Client version is {ver}, may not work properly."));
                }
                client
                    .write_all(format!("{{\"status\":0,\"ver\":{VERSION}}}\n").as_bytes())
                    .await?;
            }
            Operation::Request => {
                client
                    .write_all(serde_json::to_string(&conf).unwrap().as_bytes())
                    .await?;
                info(format!("Sended configuration to: {}", client.peer_addr()?))
            }
            Operation::Sync => save_config(json["config"].to_string().as_str(), true).await,
            Operation::Combined => macros(json["macro"].clone(), &conf).await?,
            Operation::Independent => independent(json["input"].clone(), &conf).await?,
        }
    }
}

async fn load_config() -> Option<Config> {
    let conf: Config = match fs::read_to_string(CONF_PATH) {
        Ok(ok) => serde_json::from_str(&ok).unwrap(),
        Err(_) => return None,
    };

    Some(conf)
}

async fn save_config(str: &str, sync: bool) {
    match fs::write(CONF_PATH, str) {
        Ok(_) => match sync {
            true => {
                info("Configuration synchronized, need to restart.");
                std::process::exit(0);
            }
            false => info("Configuration saved."),
        },
        Err(_) => match sync {
            true => {
                error("Failed to synchronize configuration!");
                std::process::exit(0);
            }
            false => error("Failed to save configuration!"),
        },
    }
}

async fn macros(value: Value, conf: &Config) -> Result<()> {
    let name = value["name"].as_str().unwrap_or("");
    let list = match value["steps"].as_array() {
        Some(s) => s,
        None => {
            error("Failed to parse steps!");
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
    let step = match value["step"].as_u64() {
        Some(s) => Step::from_u64(s),
        None => {
            error("Failed to parse step!");
            return Ok(());
        }
    };
    let t = match value["type"].as_u64() {
        Some(s) => InputType::from_u64(s),
        None => {
            error("Failed to parse input type!");
            return Ok(());
        }
    };
    execute(step, t, conf).await.unwrap();

    Ok(())
}

fn simulate_key_event<F>(step: Step, event_type_fn: F, conf: &Config)
where
    F: Fn(rdev::Key) -> EventType,
{
    let key = match step {
        Step::Open => conf.open.clone().to_key(),
        Step::Up => conf.up.clone().to_key(),
        Step::Down => conf.down.clone().to_key(),
        Step::Left => conf.left.clone().to_key(),
        Step::Right => conf.right.clone().to_key(),
    };
    simulate(&event_type_fn(key)).unwrap();
}

async fn execute(step: Step, t: InputType, conf: &Config) -> Result<()> {
    match t {
        InputType::Click => {
            simulate_key_event(step.clone(), EventType::KeyPress, conf);
            print(step.clone());
            let _ = io::stdout().flush();
            sleep(Duration::from_millis(conf.delay)).await;
            simulate_key_event(step, EventType::KeyRelease, conf);
        }
        InputType::Press => {
            simulate_key_event(step, EventType::KeyPress, conf);
        }
        InputType::Release => {
            simulate_key_event(step, EventType::KeyRelease, conf);
        }
        InputType::Begin => {
            print("Free input: ");
        }
        InputType::End => {
            println!();
        }
    }
    sleep(Duration::from_millis(conf.delay)).await;

    Ok(())
}
