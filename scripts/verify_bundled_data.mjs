import fs from "node:fs";

const assets = "app/src/main/assets";
const read = (name) => JSON.parse(fs.readFileSync(`${assets}/${name}`, "utf8"));

const allowedVisaStatuses = new Set([
  "freedom",
  "visa free",
  "eta",
  "visa on arrival",
  "e-visa",
  "visa required",
  "entry restricted",
  "special permit",
  "mixed requirements",
  "no data",
]);
const allowedFormalityTypes = new Set([
  "arrival_card",
  "pre_travel_registration",
  "health_declaration",
  "customs_declaration",
  "tourism_registration",
  "other_entry_formality",
]);
const allowedVisaTypeCodes = new Set([
  "freedom",
  "visa_free",
  "eta",
  "visa_on_arrival",
  "e_visa",
  "visa_required",
  "entry_restricted",
  "special_permit",
  "mixed_requirements",
  "no_data",
]);

const visa = read("visa_requirements.json");
if (visa.schemaVersion !== 1 || !Number.isInteger(visa.dataVersion) || visa.dataVersion < 1) {
  throw new Error("Bundled visa database has an unsupported release contract");
}
if (visa.destinationCount !== 248) {
  throw new Error("Bundled visa database must contain 248 destinations");
}

const sourceRegistry = new Map(
  (visa.sources ?? []).map((source) => [source.id, source]),
);
const provenanceMapping = visa.provenance?.destinationSourceIds ?? {};
if (visa.dataVersion >= 13) {
  if (visa.provenance?.schemaVersion !== 1) {
    throw new Error("Bundled visa database has an unsupported provenance contract");
  }
  if (sourceRegistry.size === 0) {
    throw new Error("Bundled visa database has no source registry");
  }
  for (const [sourceId, source] of sourceRegistry) {
    if (
      !sourceId ||
      !["dataset", "derived"].includes(source?.type) ||
      !source?.name ||
      !source?.url?.startsWith("https://")
    ) {
      throw new Error(`Invalid source registry entry: ${sourceId}`);
    }
  }
}

const passportRows = Object.entries(visa.passports ?? {});
if (passportRows.length !== 199) {
  throw new Error(`Bundled visa database must contain 199 passports, got ${passportRows.length}`);
}
const destinationUniverse = new Set(passportRows.map(([passportId]) => passportId));
for (const [passportId, row] of passportRows) {
  const rules = Object.entries(row ?? {});
  if (rules.length !== 247 || row[passportId] !== undefined) {
    throw new Error(`${passportId}: expected exactly 247 non-self rules`);
  }
  for (const [destinationId, rule] of rules) {
    destinationUniverse.add(destinationId);
    if (!allowedVisaStatuses.has(rule?.status)) {
      throw new Error(`${passportId}->${destinationId}: unsupported status ${rule?.status}`);
    }
    if (
      rule?.days !== undefined &&
      (!Number.isInteger(rule.days) || rule.days < 1 || rule.days > 3660)
    ) {
      throw new Error(`${passportId}->${destinationId}: invalid stay length`);
    }
    const explicitSource = rule?.source && rule?.sourceUrl;
    const destinationSource = sourceRegistry.get(provenanceMapping[destinationId]);
    if (visa.dataVersion >= 13) {
      if (!explicitSource && !destinationSource) {
        throw new Error(`${passportId}->${destinationId}: missing provenance`);
      }
      if (explicitSource) {
        if (
          !rule.sourceUrl.startsWith("https://") ||
          !["official", "corroborated"].includes(rule.sourceType)
        ) {
          throw new Error(`${passportId}->${destinationId}: invalid rule source`);
        }
      }
    }
  }
}
if (destinationUniverse.size !== 248) {
  throw new Error(`Bundled destination universe is incomplete: ${destinationUniverse.size}`);
}
if (
  visa.dataVersion >= 13 &&
  Object.keys(provenanceMapping).length !== destinationUniverse.size
) {
  throw new Error("Bundled provenance mapping must cover all destinations");
}
if (visa.dataVersion >= 14) {
  const snapshot = visa.sourceSnapshots?.["passport-index-data"];
  const passportIndexSource = sourceRegistry.get("passport-index-data");
  if (
    !snapshot ||
    snapshot.schemaVersion !== 1 ||
    !/^releases\/passport_index_source_v[1-9][0-9]*\.json$/.test(snapshot.file ?? "") ||
    !/^[a-f0-9]{64}$/.test(snapshot.sha256 ?? "") ||
    snapshot.passportCount !== 199 ||
    snapshot.ruleCount !== 39402
  ) {
    throw new Error("Bundled v14 database has no valid Passport Index snapshot contract");
  }
  if (
    passportIndexSource?.snapshotSha256 !== snapshot.sha256 ||
    !passportIndexSource?.snapshotUrl?.startsWith("https://")
  ) {
    throw new Error("Bundled v14 source registry does not expose the exact snapshot");
  }
  if (visa.quality?.arrivalCardsAffectVisaStatus !== false) {
    throw new Error("Bundled v14 database must isolate arrival cards from visa status");
  }
  const geToRu = visa.passports?.["268"]?.["643"];
  const geToGb = visa.passports?.["268"]?.["826"];
  if (geToRu?.status !== "visa free" || geToRu?.days !== 90) {
    throw new Error("Bundled v14 regression: GE->RU must be visa free for 90 days");
  }
  if (geToGb?.status !== "visa required") {
    throw new Error("Bundled v14 regression: GE->GB must require a visa");
  }
}
if (visa.dataVersion >= 15) {
  if (visa.quality?.commercialSourcesOnly !== true) {
    throw new Error("Bundled v15+ database must enforce commercial-safe active sources");
  }
  if (sourceRegistry.size !== 2) {
    throw new Error(`Bundled v15+ database must expose exactly two active sources`);
  }
  const forbidden = /(?:cc\s*by[-\s]*nc|noncommercial|kaggle-extended)/i;
  if (forbidden.test(JSON.stringify(visa.sources))) {
    throw new Error("Bundled v15+ database contains a forbidden non-commercial source");
  }
  let noDataRules = 0;
  for (const row of Object.values(visa.passports ?? {})) {
    for (const rule of Object.values(row ?? {})) {
      if (rule?.status === "no data") noDataRules += 1;
    }
  }
  if (visa.dataVersion === 15 && noDataRules !== 4781) {
    throw new Error(`Bundled v15 expected 4781 no-data rules, found ${noDataRules}`);
  }
  if (visa.dataVersion >= 17) {
    if (
      visa.quality?.territoryAuditVersion !== 2 ||
      visa.quality?.officialTerritoryMatrices !== 25 ||
      visa.quality?.pendingTerritoryAudits !== 0
    ) {
      throw new Error("Bundled v17 database has no completed territory audit contract");
    }
    if (noDataRules !== 1) {
      throw new Error(`Bundled v17 expected 1 explicitly unresolved rule, found ${noDataRules}`);
    }
  }
}

const requirements = read("entry_requirements.json");
if (requirements.schemaVersion !== 1 || requirements.version < 1) {
  throw new Error("Bundled entry requirements have an unsupported release contract");
}
for (const requirement of requirements.requirements ?? []) {
  if (!allowedFormalityTypes.has(requirement.type)) {
    throw new Error(`${requirement.id}: unsupported non-visa formality`);
  }
  for (const forbidden of ["status", "visaType", "visaStatus", "days", "stayDays", "mapCategory"]) {
    if (Object.hasOwn(requirement, forbidden)) {
      throw new Error(`${requirement.id}: formality attempts to redefine a visa category`);
    }
  }
  if (!(requirement.visaTypes ?? []).every((type) => allowedVisaTypeCodes.has(type))) {
    throw new Error(`${requirement.id}: unsupported applicable visa type`);
  }
}

const guides = read("entry_guides.json");
if (guides.schemaVersion !== 1 || guides.version < 1 || (guides.guides ?? []).length < 4) {
  throw new Error("Bundled entry guides have an unsupported release contract");
}
for (const guide of guides.guides ?? []) {
  if (!(guide.visaTypes ?? []).every((type) => allowedVisaTypeCodes.has(type))) {
    throw new Error(`${guide.passportIso}->${guide.destinationIso}: unsupported guide visa type`);
  }
}

console.log(
  `Bundled data valid: visa v${visa.dataVersion}, ${passportRows.length}×${visa.destinationCount}, ` +
    `${requirements.requirements?.length ?? 0} formalities, ${guides.guides?.length ?? 0} guides.`
);
