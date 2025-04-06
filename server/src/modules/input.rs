use std::io::Write;
use std::io;

use tokio::io::Result;
use tokio::time::{sleep, Duration};
use serde_json::Value;
use rdev::{simulate, EventType};

use crate::data::*;
use crate::tool::*;
use rust_i18n::t;

/// 处理宏命令
pub async fn macros(value: Value, conf: &Config) -> Result<()> {
    let name = value["name"].as_str().unwrap_or("");
    let list = match value["steps"].as_array() {
        Some(s) => s,
        None => {
            error(t!("err_parse_step_failed"));
            return Ok(());
        }
    };

    // 按下打开键
    print(format!("{name}: "));
    if conf.openType == "hold" {
        execute(Step::Open, InputType::Press, conf).await.unwrap();
    } else if conf.openType == "long_press" {
        execute(Step::Open, InputType::Press, conf).await.unwrap();
        sleep(Duration::from_millis(400)).await;
        execute(Step::Open, InputType::Release, conf).await.unwrap();
    } else if conf.openType == "double_tap" {
        execute(Step::Open, InputType::Click, conf).await.unwrap();
        execute(Step::Open, InputType::Click, conf).await.unwrap();
    } else {
        execute(Step::Open, InputType::Click, conf).await.unwrap();
    }

    // 点击步骤
    for i in list {
        execute(Step::from_u64(i.as_u64().unwrap()), InputType::Click, conf)
            .await
            .unwrap();
    }

    // 释放打开键
    if conf.openType == "hold" {
        execute(Step::Open, InputType::Release, conf).await.unwrap();
    }
    println!();

    Ok(())
}

/// 处理独立输入
pub async fn independent(value: Value, conf: &Config) -> Result<()> {
    let step = match value["step"].as_u64() {
        Some(s) => Step::from_u64(s),
        None => {
            error(t!("err_parse_step_failed"));
            return Ok(());
        }
    };
    let t = match value["type"].as_u64() {
        Some(s) => InputType::from_u64(s),
        None => {
            error(t!("err_parse_input_failed"));
            return Ok(());
        }
    };
    execute(step, t, conf).await.unwrap();

    Ok(())
}

/// 模拟按键事件
fn simulate_key_event(step: Step, event_type: u32, conf: &Config) {
    let data = match step {
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
            KeyType::WheelUp => simulate(&EventType::Wheel {
                delta_x: 0,
                delta_y: 1,
            })
            .unwrap(),
            KeyType::WheelDown => simulate(&EventType::Wheel {
                delta_x: 0,
                delta_y: -1,
            })
            .unwrap(),
        }
    } else {
        match data.key_type {
            KeyType::Keyboard => simulate(&EventType::KeyRelease(data.keyboard)).unwrap(),
            KeyType::MouseButton => simulate(&EventType::ButtonRelease(data.mouse_button)).unwrap(),
            KeyType::WheelUp => (),
            KeyType::WheelDown => (),
        }
    }
}

/// 执行输入操作
pub async fn execute(step: Step, t: InputType, conf: &Config) -> Result<()> {
    match t {
        InputType::Click => {
            simulate_key_event(step.clone(), 0, conf);
            print(step.clone());
            let _ = io::stdout().flush();
            sleep(Duration::from_millis(conf.delay)).await;
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
