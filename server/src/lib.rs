use std::fs;
use serde_json::{json, Value};
use tokio::io::{AsyncReadExt, AsyncWriteExt, Result};
use tokio::net::{TcpListener, TcpStream};
use tokio::time::{sleep, Duration};
use rdev::{simulate, EventType, Key};
use config::*;

pub mod config;

const CONF_PATH: &str = "./config.json";

pub async fn run() -> Result<()> {
    // Load configuration
    let conf: Config = load_config().await?;

    // Listen port
    let listener = TcpListener::bind(format!("127.0.0.1:{}", conf.port)).await?;
    println!("Listening port: {}", conf.port);
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

        // Parsing json
        let json = json!(std::str::from_utf8(&buffer[..size]).unwrap());
        let opt: Operation = Operation::from_u64(json["operation"].as_u64().unwrap());
        match opt {
            Operation::Status => client.write_all("ready".as_bytes()).await?,
            Operation::Request => {client.write_all(serde_json::to_string(&conf).unwrap().as_bytes()).await?; println!("Sending configuration to: {}", client.peer_addr()?)},
            Operation::Sync => save_config(json["configuration"].as_str().unwrap()).await,
            Operation::Combined => macros(json["macro"].clone(), &conf).await?,
            Operation::Independent => independent(json["input"].clone(), &conf).await?
        }
    }
}

async fn load_config() -> Result<Config> {
    let conf: Config = match fs::read_to_string(CONF_PATH) {
        Ok(ok) => serde_json::from_str(&ok).unwrap(),
        Err(_) => {println!("Error loading configuration, loading default configuration!"); Config::default()}
    };

    Ok(conf)
}

async fn save_config(str: &str) {
    fs::write(CONF_PATH, str).unwrap();
    println!("Configuration synchronize, need to restart.");
    std::process::exit(0);
}

async fn macros(value: Value, conf: &Config) -> Result<()>{
    // Press ctrl
    print!("{}: ", value["name"].as_str().unwrap());
    execute(Step::Ctrl, InputType::Press, conf).await.unwrap();
    
    // Click steps
    let list = value["steps"].as_array().unwrap();
    for i in list {
        execute(Step::from_u64(i.as_u64().unwrap()), InputType::Click, conf).await.unwrap();
    }

    // Release ctrl
    print!("\n");
    execute(Step::Ctrl, InputType::Release, conf).await.unwrap();

    Ok(())
}

async fn independent(value: Value, conf: &Config) -> Result<()>{
    let step = Step::from_u64(value["step"].as_u64().unwrap());
    let t = InputType::from_u64(value["type"].as_u64().unwrap());
    execute(step, t, conf).await.unwrap();

    Ok(())
}

async fn execute(step: Step, t: InputType, conf: &Config) -> Result<()>{
    match t {
        InputType::Click => {
            match step {
                Step::Ctrl => {simulate(&EventType::KeyPress(str_to_key(conf.open.as_str()))).unwrap(); print!("{}", step)},
                Step::Up => {simulate(&EventType::KeyPress(str_to_key(conf.up.as_str()))).unwrap(); print!("{}", step)},
                Step::Down => {simulate(&EventType::KeyPress(str_to_key(conf.down.as_str()))).unwrap(); print!("{}", step)},
                Step::Left => {simulate(&EventType::KeyPress(str_to_key(conf.left.as_str()))).unwrap(); print!("{}", step)},
                Step::Right => {simulate(&EventType::KeyPress(str_to_key(conf.right.as_str()))).unwrap(); print!("{}", step)}
            }
            sleep(Duration::from_millis(conf.delay)).await;
            match step {
                Step::Ctrl => simulate(&EventType::KeyRelease(str_to_key(conf.open.as_str()))).unwrap(),
                Step::Up => simulate(&EventType::KeyRelease(str_to_key(conf.up.as_str()))).unwrap(),
                Step::Down => simulate(&EventType::KeyRelease(str_to_key(conf.down.as_str()))).unwrap(),
                Step::Left => simulate(&EventType::KeyRelease(str_to_key(conf.left.as_str()))).unwrap(),
                Step::Right => simulate(&EventType::KeyRelease(str_to_key(conf.right.as_str()))).unwrap()
            }
        }
        InputType::Press => {
            match step {
                Step::Ctrl => simulate(&EventType::KeyPress(str_to_key(conf.open.as_str()))).unwrap(),
                Step::Up => simulate(&EventType::KeyPress(str_to_key(conf.up.as_str()))).unwrap(),
                Step::Down => simulate(&EventType::KeyPress(str_to_key(conf.down.as_str()))).unwrap(),
                Step::Left => simulate(&EventType::KeyPress(str_to_key(conf.left.as_str()))).unwrap(),
                Step::Right => simulate(&EventType::KeyPress(str_to_key(conf.right.as_str()))).unwrap()
            }
        }
        InputType::Release => {
            match step {
                Step::Ctrl => simulate(&EventType::KeyRelease(str_to_key(conf.open.as_str()))).unwrap(),
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