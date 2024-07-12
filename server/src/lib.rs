use std::io::Write;
use std::{fs, io};
use serde_json::Value;
use tokio::io::{AsyncReadExt, AsyncWriteExt, Result};
use tokio::net::{TcpListener, TcpStream};
use tokio::time::{sleep, Duration};
use rdev::{simulate, EventType, Key};
use local_ipaddress;
use data::*;

pub mod data;

const CONF_PATH: &str = "./config.json";

pub async fn run() -> Result<()> {
    // Load configuration
    let conf: Config = match load_config().await{
        Some(s) => {println!("Configuration loaded."); s},
        None => {
            println!("File error: Failed to load configuration, loading default configuration!");
            save_config(serde_json::to_string(&Config::default()).unwrap().as_str(), false).await;
            Config::default()
        },
    };

    // Listen port
    let listener = match TcpListener::bind(format!("{}:{}", local_ipaddress::get().unwrap(), conf.port)).await {
        Ok(ok) => ok,
        Err(err) => {
            println!("Error: {}", err); 
            println!("Using temporary network configuration"); 
            TcpListener::bind(format!("{}:{}", local_ipaddress::get().unwrap(), Config::default().port)).await?
        }
    };
    println!("Listening: {}", listener.local_addr().unwrap().to_string());
    loop {
        let (client, _address) = listener.accept().await?;
        tokio::spawn(handle_connection(client, conf.clone()));
    }
}

async fn handle_connection(mut client: TcpStream, conf: Config) -> Result<()> {
    println!("Established connection with: {}", client.peer_addr()?);
    let mut buffer = vec![0; 4096];
    loop {
        let size = client.read(&mut buffer).await?;
        if size == 0 {
            println!("Connection closed: {}", client.peer_addr()?);
            return Ok(());
        }

        // println!("{}",std::str::from_utf8(&buffer[..size]).unwrap());

        // Parsing json
        let json: Value = match serde_json::from_str(std::str::from_utf8(&buffer[..size]).unwrap()) {
            Ok(ok) => ok,
            Err(_) => {
                println!("Request error: Failed to parse request data!"); 
                continue
            },
        };
        let opt: Operation = match json["operation"].as_u64() {
            Some(s) => Operation::from_u64(s),
            None => {
                println!("Request error: Failed to parse operation!"); 
                continue
            },
        };

        // Operation
        match opt {
            Operation::Status => {
                client.write_all("ready\n".as_bytes()).await?; 
                // println!("Sended status to: {}", client.peer_addr()?)
            },
            Operation::Request => {
                client.write_all(serde_json::to_string(&conf).unwrap().as_bytes()).await?; 
                println!("Sended configuration to: {}", client.peer_addr()?)
            },
            Operation::Sync => save_config(json["configuration"].to_string().as_str(), true).await,
            Operation::Combined => macros(json["macro"].clone(), &conf).await?,
            Operation::Independent => independent(json["input"].clone(), &conf).await?
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
        Ok(_) => {
            match sync {
                true => {
                    println!("Configuration synchronized, need to restart.");
                    std::process::exit(0);
                },
                false => println!("Configuration saved."),
            }
        },
        Err(_) => match sync {
            true => {
                println!("File error: Failed to synchronize configuration!");
                std::process::exit(0);
            },
            false => println!("File error: Failed to save configuration!"),
        },
    }
}

async fn macros(value: Value, conf: &Config) -> Result<()>{
    let name = match value["name"].as_str() {
        Some(s) => s,
        None => "",
    };
    let list = match value["steps"].as_array() {
        Some(s) => s,
        None => {
            println!("Request error: Failed to parse steps!"); 
            return Ok(())
        },
    };

    // Press ctrl
    print!("{}: ", name);
    execute(Step::Open, InputType::Press, conf).await.unwrap();
    
    // Click steps
    for i in list {
        execute(Step::from_u64(i.as_u64().unwrap()), InputType::Click, conf).await.unwrap();
    }

    // Release ctrl
    execute(Step::Open, InputType::Release, conf).await.unwrap();

    Ok(())
}

async fn independent(value: Value, conf: &Config) -> Result<()>{
    let step = match value["step"].as_u64() {
        Some(s) => Step::from_u64(s),
        None => {
            println!("Request error: Failed to parse step!"); 
            return Ok(())
        },
    };
    let t = match value["type"].as_u64() {
        Some(s) => InputType::from_u64(s),
        None => {
            println!("Request error: Failed to parse input type!"); 
            return Ok(())
        },
    };
    execute(step, t, conf).await.unwrap();

    Ok(())
}

async fn execute(step: Step, t: InputType, conf: &Config) -> Result<()>{
    match t {
        InputType::Click => {
            match step {
                Step::Open => simulate(&EventType::KeyPress(str_to_key(conf.open.as_str()))).unwrap(),
                Step::Up => simulate(&EventType::KeyPress(str_to_key(conf.up.as_str()))).unwrap(),
                Step::Down => simulate(&EventType::KeyPress(str_to_key(conf.down.as_str()))).unwrap(),
                Step::Left => simulate(&EventType::KeyPress(str_to_key(conf.left.as_str()))).unwrap(),
                Step::Right => simulate(&EventType::KeyPress(str_to_key(conf.right.as_str()))).unwrap()
            }
            print!("{}", step);
            let _ = io::stdout().flush();
            sleep(Duration::from_millis(conf.delay)).await;
            match step {
                Step::Open => simulate(&EventType::KeyRelease(str_to_key(conf.open.as_str()))).unwrap(),
                Step::Up => simulate(&EventType::KeyRelease(str_to_key(conf.up.as_str()))).unwrap(),
                Step::Down => simulate(&EventType::KeyRelease(str_to_key(conf.down.as_str()))).unwrap(),
                Step::Left => simulate(&EventType::KeyRelease(str_to_key(conf.left.as_str()))).unwrap(),
                Step::Right => simulate(&EventType::KeyRelease(str_to_key(conf.right.as_str()))).unwrap()
            }
        }
        InputType::Press => {
            match step {
                Step::Open => simulate(&EventType::KeyPress(str_to_key(conf.open.as_str()))).unwrap(),
                Step::Up => simulate(&EventType::KeyPress(str_to_key(conf.up.as_str()))).unwrap(),
                Step::Down => simulate(&EventType::KeyPress(str_to_key(conf.down.as_str()))).unwrap(),
                Step::Left => simulate(&EventType::KeyPress(str_to_key(conf.left.as_str()))).unwrap(),
                Step::Right => simulate(&EventType::KeyPress(str_to_key(conf.right.as_str()))).unwrap()
            }
        }
        InputType::Release => {
            match step {
                Step::Open => simulate(&EventType::KeyRelease(str_to_key(conf.open.as_str()))).unwrap(),
                Step::Up => simulate(&EventType::KeyRelease(str_to_key(conf.up.as_str()))).unwrap(),
                Step::Down => simulate(&EventType::KeyRelease(str_to_key(conf.down.as_str()))).unwrap(),
                Step::Left => simulate(&EventType::KeyRelease(str_to_key(conf.left.as_str()))).unwrap(),
                Step::Right => simulate(&EventType::KeyRelease(str_to_key(conf.right.as_str()))).unwrap()
            }
            print!("\n");
        }
    }
    sleep(Duration::from_millis(conf.delay)).await;

    Ok(())
}

fn str_to_key(str: &str) -> Key {
    match str {
        "alt" => Key::Alt,
        "alt_gr" => Key::AltGr,
        "backspace" => Key::Backspace,
        "caps_lock" => Key::CapsLock,
        "ctrl_left" => Key::ControlLeft,
        "ctrl_right" => Key::ControlRight,
        "delete" => Key::Delete,
        "down" => Key::DownArrow,
        "end" => Key::End,
        "esc" => Key::Escape,
        "f1" => Key::F1,
        "f10" => Key::F10,
        "f11" => Key::F11,
        "f12" => Key::F12,
        "f2" => Key::F2,
        "f3" => Key::F3,
        "f4" => Key::F4,
        "f5" => Key::F5,
        "f6" => Key::F6,
        "f7" => Key::F7,
        "f8" => Key::F8,
        "f9" => Key::F9,
        "home" => Key::Home,
        "left" => Key::LeftArrow,
        "page_down" => Key::PageDown,
        "page_up" => Key::PageUp,
        "enter" => Key::Return,
        "right" => Key::RightArrow,
        "shift_left" => Key::ShiftLeft,
        "shift_right" => Key::ShiftRight,
        "space" => Key::Space,
        "tab" => Key::Tab,
        "up" => Key::UpArrow,
        "print_screen" => Key::PrintScreen,
        "scroll_lock" => Key::ScrollLock,
        "pause" => Key::Pause,
        "num_lock" => Key::NumLock,
        "`" => Key::BackQuote,
        "1" => Key::Num1,
        "2" => Key::Num2,
        "3" => Key::Num3,
        "4" => Key::Num4,
        "5" => Key::Num5,
        "6" => Key::Num6,
        "7" => Key::Num7,
        "8" => Key::Num8,
        "9" => Key::Num9,
        "0" => Key::Num0,
        "-" => Key::Minus,
        "=" => Key::Equal,
        "q" => Key::KeyQ,
        "w" => Key::KeyW,
        "e" => Key::KeyE,
        "r" => Key::KeyR,
        "t" => Key::KeyT,
        "y" => Key::KeyY,
        "u" => Key::KeyU,
        "i" => Key::KeyI,
        "o" => Key::KeyO,
        "p" => Key::KeyP,
        "[" => Key::LeftBracket,
        "]" => Key::RightBracket,
        "a" => Key::KeyA,
        "s" => Key::KeyS,
        "d" => Key::KeyD,
        "f" => Key::KeyF,
        "g" => Key::KeyG,
        "h" => Key::KeyH,
        "j" => Key::KeyJ,
        "k" => Key::KeyK,
        "l" => Key::KeyL,
        ";" => Key::SemiColon,
        "'" => Key::Quote,
        "\\" => Key::BackSlash,
        "z" => Key::KeyZ,
        "x" => Key::KeyX,
        "c" => Key::KeyC,
        "v" => Key::KeyV,
        "b" => Key::KeyB,
        "n" => Key::KeyN,
        "m" => Key::KeyM,
        "," => Key::Comma,
        "." => Key::Dot,
        "/" => Key::Slash,
        "insert" => Key::Insert,
        "kp_enter" => Key::KpReturn,
        "kp-" => Key::KpMinus,
        "kp+" => Key::KpPlus,
        "kp*" => Key::KpMultiply,
        "kp/" => Key::KpDivide,
        "kp0" => Key::Kp0,
        "kp1" => Key::Kp1,
        "kp2" => Key::Kp2,
        "kp3" => Key::Kp3,
        "kp4" => Key::Kp4,
        "kp5" => Key::Kp5,
        "kp6" => Key::Kp6,
        "kp7" => Key::Kp7,
        "kp8" => Key::Kp8,
        "kp9" => Key::Kp9,
        "kp_delete" => Key::KpDelete,
        "fn" => Key::Function,
        _ => Key::Unknown(0)
    }
}