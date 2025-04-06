use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Clone)]
pub struct Auth {
    pub sid: String,
    pub time: u64,
} 