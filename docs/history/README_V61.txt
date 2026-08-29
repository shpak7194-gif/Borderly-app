Borderly v61 / app 1.1.1

- Bundled visa database: v17.
- Remote visa download: up to three attempts for transient network errors.
- Connect timeout: 15 seconds; read timeout: 30 seconds.
- Integrity, taxonomy, matrix and SHA-256 checks are unchanged.
- Failed downloads preserve the last verified database and are logged with
  the BorderlyVisaUpdate tag.
