use std::fmt;

pub enum Operation {
    Status = 0,
    Macro = 1,
    Input = 2,
    Request = 3,
    Sync = 4,
    Auth = 5,
}

impl Operation {
    pub fn from_u64(v: u64) -> Self {
        match v {
            1 => Self::Macro,
            2 => Self::Input,
            3 => Self::Request,
            4 => Self::Sync,
            5 => Self::Auth,
            _ => Self::Status,
        }
    }
}

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