use std::fmt;

use serde::{de, Deserialize, Deserializer};

use crate::{error::I18NError, key::KeyFromString};

#[derive(Debug, Clone, Deserialize)]
pub struct CombinedMacro {
    pub name: String,
    pub steps: Vec<Step>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct IndependentInput {
    pub r#type: InputType,
    pub step: Step,
}

#[derive(Debug, Clone, Deserialize)]
pub struct ConfigData(pub crate::data::Config);

impl std::ops::Deref for ConfigData {
    type Target = crate::data::Config;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl std::ops::DerefMut for ConfigData {
    fn deref_mut(&mut self) -> &mut Self::Target {
        &mut self.0
    }
}

impl AsRef<crate::data::Config> for ConfigData {
    fn as_ref(&self) -> &crate::data::Config {
        &self.0
    }
}

#[derive(Debug, Clone, Copy)]
pub enum InputType {
    Click = 0,
    Press = 1,
    Release = 2,
    Begin = 3,
    End = 4,
}

impl InputType {
    pub fn from_u32(v: u32) -> Option<Self> {
        let ty = match v {
            0 => Self::Click,
            1 => Self::Press,
            2 => Self::Release,
            3 => Self::Begin,
            4 => Self::End,
            _ => return None,
        };
        Some(ty)
    }
}

impl<'de> Deserialize<'de> for InputType {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        let v = u32::deserialize(deserializer)?;
        Self::from_u32(v).ok_or(de::Error::custom(format!("Invalid input_type: {}", v)))
    }
}

#[derive(Debug, Clone, Copy)]
pub enum Step {
    Open = 0,
    Up = 1,
    Down = 2,
    Left = 3,
    Right = 4,
}

impl Step {
    pub fn from_u32(v: u32) -> Option<Self> {
        let step = match v {
            0 => Self::Open,
            1 => Self::Up,
            2 => Self::Down,
            3 => Self::Left,
            4 => Self::Right,
            _ => return None,
        };
        Some(step)
    }
}

impl<'de> Deserialize<'de> for Step {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>,
    {
        let v = u32::deserialize(deserializer)?;
        Self::from_u32(v).ok_or(de::Error::custom(format!("Invalid step: {}", v)))
    }
}

impl fmt::Display for Step {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match self {
            Self::Open => write!(f, "open"),
            Self::Up => write!(f, "↑"),
            Self::Down => write!(f, "↓"),
            Self::Left => write!(f, "←"),
            Self::Right => write!(f, "→"),
        }
    }
}

pub enum InputData {
    Keyboard(rdev::Key),
    MouseButton(rdev::Button),
    WheelUp,
    WheelDown,
}

impl KeyFromString for InputData {
    fn from_str(s: &str) -> Option<Self> {
        if let Some(key) = rdev::Key::from_str(s) {
            return Some(Self::Keyboard(key));
        }
        if let Some(button) = rdev::Button::from_str(s) {
            return Some(Self::MouseButton(button));
        }
        match s {
            "wheel_up" => Some(Self::WheelUp),
            "wheel_down" => Some(Self::WheelDown),
            _ => None,
        }
    }
}

#[repr(i32)]
#[derive(Clone, Copy)]
pub enum Status {
    Success = 0,
    VersionMismatch = 1,
    Unauthorized = 2,
}

impl fmt::Display for Status {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match self {
            Self::Success => write!(f, "0"),
            Self::VersionMismatch => write!(f, "1"),
            Self::Unauthorized => write!(f, "2"),
        }
    }
}

/// Struct for requests from the client.
///
/// Since serde does not yet support tagged enumerations in numeric form,
/// we can only deserialize it manually.
#[derive(Debug, Clone)]
pub enum Request {
    // 0
    Status {
        ver: String,
    },
    // 1
    Combined {
        r#macro: CombinedMacro,
        token: String,
    },
    // 2
    Independent {
        input: IndependentInput,
        token: String,
    },
    // 3
    Config {
        token: String,
    },
    // 4
    Sync {
        config: ConfigData,
        token: String,
    },
    // 5
    Auth {
        sid: String,
    },
}

impl<'de> Deserialize<'de> for Request {
    fn deserialize<D>(deserializer: D) -> std::result::Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        #[derive(Deserialize)]
        struct Status {
            ver: String,
        }
        #[derive(Deserialize)]
        struct Combined {
            r#macro: CombinedMacro,
            #[serde(default)]
            token: String,
        }
        #[derive(Deserialize)]
        struct Independent {
            input: IndependentInput,
            #[serde(default)]
            token: String,
        }
        #[derive(Deserialize)]
        struct Config {
            #[serde(default)]
            token: String,
        }
        #[derive(Deserialize)]
        struct Sync {
            config: ConfigData,
            #[serde(default)]
            token: String,
        }
        #[derive(Deserialize)]
        struct Auth {
            sid: String,
        }

        let map: serde_json::Value = Deserialize::deserialize(deserializer)?;

        let opt = map
            .get("opt")
            .ok_or_else(|| de::Error::missing_field("opt"))?
            .as_u64()
            .ok_or_else(|| de::Error::custom("opt must be a number"))?;

        let req = match opt {
            0 => {
                let tmp: Status = serde_json::from_value(map).map_err(de::Error::custom)?;
                Request::Status { ver: tmp.ver }
            }
            1 => {
                let tmp: Combined = serde_json::from_value(map).map_err(de::Error::custom)?;
                Request::Combined {
                    r#macro: tmp.r#macro,
                    token: tmp.token,
                }
            }
            2 => {
                let tmp: Independent = serde_json::from_value(map).map_err(de::Error::custom)?;
                Request::Independent {
                    input: tmp.input,
                    token: tmp.token,
                }
            }
            3 => {
                let tmp: Config = serde_json::from_value(map).map_err(de::Error::custom)?;
                Request::Config { token: tmp.token }
            }
            4 => {
                let tmp: Sync = serde_json::from_value(map).map_err(de::Error::custom)?;
                Request::Sync {
                    config: tmp.config,
                    token: tmp.token,
                }
            }
            5 => {
                let tmp: Auth = serde_json::from_value(map).map_err(de::Error::custom)?;
                Request::Auth { sid: tmp.sid }
            }
            _ => return Err(de::Error::custom(format!("Unsupported opt: {}", opt))),
        };

        Ok(req)
    }
}

impl Request {
    pub fn from_data(data: &str) -> Result<Self, I18NError> {
        serde_json::from_str(data).map_err(I18NError::Json)
    }
}
