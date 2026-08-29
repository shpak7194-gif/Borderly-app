# Borderly data attribution

Borderly visa data v17 uses only commercial-compatible active sources:

- `imorte/passport-index-data` for the 199-passport open matrix and stay
  lengths: https://github.com/imorte/passport-index-data
- Official protected sources and reviewed territorial policies stored in the
  Borderly data repository.

For the 199-country core matrix, v17 pins the exact Passport Index snapshot
and validates every non-official rule against it. The country card opens that
snapshot for dataset-backed rules; complete rule-specific official sources
still take priority where configured.

The country card distinguishes a rule-specific official link from a general
dataset or a derived territorial rule. A dataset URL documents provenance; it
is not presented as individual confirmation by a government authority.

No active application or release file contains the former non-commercial
comparison layer. All 25 previously pending non-core territories now use
dedicated official matrices. The one explicitly unresolved pair remains
`Нет подтверждённых данных`; it does not receive a guessed visa category and
never scores in the passport ranking.
