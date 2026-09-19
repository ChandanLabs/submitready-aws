export const CHECK_IDS = [
  'required-fields',
  'required-documents',
  'name-match',
  'dob-match',
  'income-match',
  'certificate-validity',
  'file-constraints',
  'eligibility-policy'
];

const REQUIRED_FIELDS = ['name', 'dateOfBirth', 'annualIncome', 'domicileState', 'course'];
const REQUIRED_DOCS = ['identityProof', 'incomeCertificate', 'domicileCertificate'];

function normalizeName(value = '') {
  return String(value).trim().toLowerCase().replace(/[^a-z0-9 ]/g, '').replace(/\s+/g, ' ');
}

function parseIsoDate(value) {
  const d = new Date(`${value}T00:00:00Z`);
  return Number.isNaN(d.getTime()) ? null : d;
}

function extractFact(text, label) {
  const source = String(text || '');
  const rx = new RegExp(`${label}\\s*[:\\-]\\s*([^\\n\\r]+)`, 'i');
  const match = source.match(rx);
  return match ? match[1].trim() : null;
}

function extractIncome(text) {
  const source = String(text || '');
  const labelled = extractFact(source, 'Annual Income');
  const value = labelled ?? source.match(/(?:INR|Rs\.?|₹)\s*([0-9][0-9,]*)/i)?.[1];
  if (!value) return null;
  const n = Number(String(value).replace(/[^0-9.-]/g, ''));
  return Number.isFinite(n) ? n : null;
}

function extractDate(text, label) {
  const raw = extractFact(text, label);
  if (!raw) return null;
  const match = raw.match(/\d{4}-\d{2}-\d{2}/);
  return match ? match[0] : null;
}

function result(id, title, passed, message, evidence = null, severity = 'error') {
  return { id, title, passed, severity: passed ? 'success' : severity, message, evidence };
}

export function validateApplication(packet, options = {}) {
  const now = parseIsoDate(options.today ?? new Date().toISOString().slice(0, 10)) ?? new Date();
  const application = packet.application ?? {};
  const documents = packet.documents ?? {};
  const results = [];

  const missingFields = REQUIRED_FIELDS.filter((field) => application[field] === undefined || application[field] === null || String(application[field]).trim() === '');
  results.push(
    result(
      'required-fields',
      'Required application fields',
      missingFields.length === 0,
      missingFields.length === 0 ? 'All required fields are present.' : `Missing: ${missingFields.join(', ')}.`
    )
  );

  const missingDocs = REQUIRED_DOCS.filter((doc) => !documents[doc]);
  results.push(
    result(
      'required-documents',
      'Required documents',
      missingDocs.length === 0,
      missingDocs.length === 0 ? 'All required documents are attached.' : `Missing: ${missingDocs.join(', ')}.`
    )
  );

  const identityText = documents.identityProof?.text ?? '';
  const identityName = extractFact(identityText, 'Name');
  const namePassed = !!application.name && !!identityName && normalizeName(application.name) === normalizeName(identityName);
  results.push(
    result(
      'name-match',
      'Applicant name matches identity proof',
      namePassed,
      namePassed ? 'Name matches the evidence.' : `Application: "${application.name ?? 'missing'}"; evidence: "${identityName ?? 'not found'}".`,
      identityName ? { field: application.name, evidence: identityName } : null
    )
  );

  const dobText = identityText;
  const evidenceDob = extractDate(dobText, 'Date of Birth');
  const dobPassed = !!application.dateOfBirth && !!evidenceDob && application.dateOfBirth === evidenceDob;
  results.push(
    result(
      'dob-match',
      'Date of birth matches identity proof',
      dobPassed,
      dobPassed ? 'Date of birth matches the evidence.' : `Application: "${application.dateOfBirth ?? 'missing'}"; evidence: "${evidenceDob ?? 'not found'}".`,
      evidenceDob ? { field: application.dateOfBirth, evidence: evidenceDob } : null
    )
  );

  const incomeText = documents.incomeCertificate?.text ?? '';
  const evidenceIncome = extractIncome(incomeText);
  const applicationIncome = Number(application.annualIncome);
  const incomePassed = Number.isFinite(applicationIncome) && evidenceIncome !== null && applicationIncome === evidenceIncome;
  results.push(
    result(
      'income-match',
      'Annual income matches income certificate',
      incomePassed,
      incomePassed ? 'Income matches the evidence.' : `Application: ₹${Number.isFinite(applicationIncome) ? applicationIncome.toLocaleString('en-IN') : 'missing'}; evidence: ${evidenceIncome === null ? 'not found' : `₹${evidenceIncome.toLocaleString('en-IN')}`}.`,
      evidenceIncome !== null ? { field: applicationIncome, evidence: evidenceIncome } : null
    )
  );

  const domicileText = documents.domicileCertificate?.text ?? '';
  const issuedOn = extractDate(domicileText, 'Issued On') ?? application.domicileCertificateIssuedOn;
  const validUntil = extractDate(domicileText, 'Valid Until') ?? application.domicileCertificateValidUntil;
  const validUntilDate = parseIsoDate(validUntil);
  const validityPassed = !!validUntilDate && validUntilDate >= now;
  results.push(
    result(
      'certificate-validity',
      'Domicile certificate is valid',
      validityPassed,
      validityPassed ? `Valid through ${validUntil}.` : `Certificate validity ended on ${validUntil ?? 'an unknown date'}.`,
      { issuedOn, validUntil, checkedOn: now.toISOString().slice(0, 10) },
      'warning'
    )
  );

  const allDocs = Object.entries(documents).filter(([, doc]) => doc);
  const badFiles = allDocs.filter(([, doc]) => {
    const typeOk = ['application/pdf', 'text/plain'].includes(doc.contentType ?? '');
    const sizeOk = Number(doc.sizeBytes ?? 0) <= 5_000_000;
    return !typeOk || !sizeOk;
  });
  results.push(
    result(
      'file-constraints',
      'File type and size constraints',
      badFiles.length === 0,
      badFiles.length === 0 ? 'All attached files meet the demo constraints (PDF/TXT, ≤5 MB).' : `Invalid attachments: ${badFiles.map(([k]) => k).join(', ')}.`,
      badFiles.length ? badFiles.map(([key, doc]) => ({ key, contentType: doc.contentType, sizeBytes: doc.sizeBytes })) : null
    )
  );

  // This is intentionally a synthetic demonstration policy, not an official scholarship rule.
  const domicilePassed = application.domicileState === 'Andhra Pradesh';
  results.push(
    result(
      'eligibility-policy',
      'Demo scholarship policy check',
      domicilePassed,
      domicilePassed ? 'Policy permits this application for the demo scheme.' : `Policy denies domicile "${application.domicileState ?? 'missing'}" for the demo scheme.`,
      { rule: 'domicileState == "Andhra Pradesh"', policySource: 'policies/eligibility.cedar' },
      'error'
    )
  );

  const passedCount = results.filter((r) => r.passed).length;
  return {
    ready: results.every((r) => r.passed),
    passedCount,
    totalChecks: results.length,
    results,
    generatedAt: new Date().toISOString()
  };
}
