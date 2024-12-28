use rust_i18n::t;

/// Error type with i18n strings.
#[derive(Debug, thiserror::Error)]
pub enum I18NError {
    #[error("{i18n}: {0}", i18n = t!("err_parse_ip"))]
    ParseAddr(#[from] std::net::AddrParseError),
    #[error("{i18n}: {0}", i18n = t!("err_json"))]
    Json(#[from] serde_json::Error),
    #[error("{i18n}: {0}", i18n = t!("err_invalid_utf8"))]
    InvalidUtf8(#[from] std::str::Utf8Error),

    // non-i18n errors
    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),
    #[error("QR code error: {0}")]
    QRCode(#[from] fast_qr::qr::QRCodeError),

    #[error("{i18n}: {0}", i18n = t!("err_bind_addr"))]
    BindAddr(std::io::Error),
    #[error("{i18n}: {0}", i18n = t!("err_bad_request"))]
    BadRequest(serde_json::Error),
}
