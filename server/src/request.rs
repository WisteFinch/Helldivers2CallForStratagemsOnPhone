use serde::{de, Deserialize};

use crate::error::I18NError;

#[derive(Debug, Clone, Deserialize)]
pub struct CombinedMacro {
    pub name: String,
    pub steps: Vec<u32>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct IndependentInput {
    pub r#type: u32,
    pub step: u32,
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
