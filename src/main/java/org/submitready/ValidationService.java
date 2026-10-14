package org.submitready;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.NumberFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ValidationService {
    private static final List<String> REQUIRED_FIELDS = List.of(
            "name", "dateOfBirth", "annualIncome", "domicileState", "course");
    private static final List<String> REQUIRED_DOCUMENTS = List.of(
            "identityProof", "incomeCertificate", "domicileCertificate");

    public Map<String, Object> validate(JsonNode packet) {
        return validate(packet, LocalDate.now(ZoneOffset.UTC));
    }

    Map<String, Object> validate(JsonNode packet, LocalDate today) {
        JsonNode application = child(packet, "application");
        JsonNode documents = child(packet, "documents");
        List<Map<String, Object>> results = new ArrayList<>();

        List<String> missingFields = REQUIRED_FIELDS.stream()
                .filter(field -> isBlank(child(application, field)))
                .toList();
        results.add(result("required-fields", "Required application fields", missingFields.isEmpty(),
                missingFields.isEmpty() ? "All required fields are present."
                        : "Missing: " + String.join(", ", missingFields) + "."));

        List<String> missingDocuments = REQUIRED_DOCUMENTS.stream()
                .filter(document -> !isTruthy(child(documents, document)))
                .toList();
        results.add(result("required-documents", "Required documents", missingDocuments.isEmpty(),
                missingDocuments.isEmpty() ? "All required documents are attached."
                        : "Missing: " + String.join(", ", missingDocuments) + "."));

        JsonNode identityDocument = child(documents, "identityProof");
        String identityText = text(child(identityDocument, "text"));
        String evidenceName = extractFact(identityText, "Name");
        String applicantName = text(child(application, "name"));
        boolean namePassed = !applicantName.isBlank() && evidenceName != null
                && normalizeName(applicantName).equals(normalizeName(evidenceName));
        results.add(result("name-match", "Applicant name matches identity proof", namePassed,
                namePassed ? "Name matches the evidence."
                        : "Application: \"" + display(applicantName) + "\"; evidence: \""
                        + display(evidenceName) + "\".",
                evidenceName == null ? null : Map.of("field", applicantName, "evidence", evidenceName), "error"));

        String evidenceDob = extractDate(identityText, "Date of Birth");
        String applicantDob = text(child(application, "dateOfBirth"));
        boolean dobPassed = !applicantDob.isBlank() && evidenceDob != null && applicantDob.equals(evidenceDob);
        results.add(result("dob-match", "Date of birth matches identity proof", dobPassed,
                dobPassed ? "Date of birth matches the evidence."
                        : "Application: \"" + display(applicantDob) + "\"; evidence: \""
                        + display(evidenceDob) + "\".",
                evidenceDob == null ? null : Map.of("field", applicantDob, "evidence", evidenceDob), "error"));

        String incomeText = text(child(child(documents, "incomeCertificate"), "text"));
        Double evidenceIncome = extractIncome(incomeText);
        Double applicantIncome = number(child(application, "annualIncome"));
        boolean incomePassed = applicantIncome != null && evidenceIncome != null
                && applicantIncome.doubleValue() == evidenceIncome.doubleValue();
        results.add(result("income-match", "Annual income matches income certificate", incomePassed,
                incomePassed ? "Income matches the evidence."
                        : "Application: ₹" + displayIncome(applicantIncome) + "; evidence: "
                        + (evidenceIncome == null ? "not found" : "₹" + displayIncome(evidenceIncome)) + ".",
                evidenceIncome == null ? null : Map.of("field", applicantIncome, "evidence", evidenceIncome), "error"));

        String domicileText = text(child(child(documents, "domicileCertificate"), "text"));
        String issuedOn = firstNonNull(extractDate(domicileText, "Issued On"),
                textOrNull(child(application, "domicileCertificateIssuedOn")));
        String validUntil = firstNonNull(extractDate(domicileText, "Valid Until"),
                textOrNull(child(application, "domicileCertificateValidUntil")));
        LocalDate validUntilDate = parseDate(validUntil);
        boolean validityPassed = validUntilDate != null && !validUntilDate.isBefore(today);
        Map<String, Object> certificateEvidence = new LinkedHashMap<>();
        certificateEvidence.put("issuedOn", issuedOn);
        certificateEvidence.put("validUntil", validUntil);
        certificateEvidence.put("checkedOn", today.toString());
        results.add(result("certificate-validity", "Domicile certificate is valid", validityPassed,
                validityPassed ? "Valid through " + validUntil + "."
                        : "Certificate validity ended on " + display(validUntil) + ".",
            certificateEvidence, "warning"));

        List<Map<String, Object>> invalidFiles = new ArrayList<>();
        if (documents.isObject()) {
            documents.fields().forEachRemaining(entry -> {
                JsonNode document = entry.getValue();
                if (!isTruthy(document)) return;
                String contentType = text(child(document, "contentType"));
                double sizeBytes = numberOrDefault(child(document, "sizeBytes"), 0);
                if (!(contentType.equals("application/pdf") || contentType.equals("text/plain"))
                    || !(sizeBytes <= 5_000_000)) {
                    Map<String, Object> invalidFile = new LinkedHashMap<>();
                    invalidFile.put("key", entry.getKey());
                    invalidFile.put("contentType", contentType.isEmpty() ? null : contentType);
                        JsonNode originalSize = child(document, "sizeBytes");
                        invalidFile.put("sizeBytes", originalSize.isMissingNode() || originalSize.isNull()
                            ? null : originalSize);
                    invalidFiles.add(invalidFile);
                }
            });
        }
        results.add(result("file-constraints", "File type and size constraints", invalidFiles.isEmpty(),
                invalidFiles.isEmpty() ? "All attached files meet the demo constraints (PDF/TXT, ≤5 MB)."
                        : "Invalid attachments: " + invalidFiles.stream()
                        .map(file -> String.valueOf(file.get("key"))).reduce((a, b) -> a + ", " + b).orElse("") + ".",
                invalidFiles.isEmpty() ? null : invalidFiles, "error"));

        String domicileState = text(child(application, "domicileState"));
        boolean domicilePassed = domicileState.equals("Andhra Pradesh");
        results.add(result("eligibility-policy", "Demo scholarship policy check", domicilePassed,
                domicilePassed ? "Policy permits this application for the demo scheme."
                        : "Policy denies domicile \"" + display(domicileState) + "\" for the demo scheme.",
            Map.of("rule", "domicileState == \"Andhra Pradesh\"", "policySource", "Java demo rule"),
                "error"));

        long passedCount = results.stream().filter(result -> (boolean) result.get("passed")).count();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("ready", passedCount == results.size());
        report.put("passedCount", passedCount);
        report.put("totalChecks", results.size());
        report.put("results", results);
        report.put("generatedAt", Instant.now().toString());
        return report;
    }

    private static Map<String, Object> result(String id, String title, boolean passed, String message) {
        return result(id, title, passed, message, null, "error");
    }

    private static Map<String, Object> result(String id, String title, boolean passed, String message,
                                               Object evidence, String severity) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("title", title);
        result.put("passed", passed);
        result.put("severity", passed ? "success" : severity);
        result.put("message", message);
        result.put("evidence", evidence);
        return result;
    }

    private static JsonNode child(JsonNode node, String key) {
        return node == null ? com.fasterxml.jackson.databind.node.MissingNode.getInstance() : node.path(key);
    }

    private static boolean isBlank(JsonNode node) {
        return node.isMissingNode() || node.isNull() || text(node).isBlank();
    }

    private static boolean isTruthy(JsonNode node) {
        return !node.isMissingNode() && !node.isNull()
                && !(node.isBoolean() && !node.booleanValue())
                && !(node.isNumber() && node.doubleValue() == 0)
                && !(node.isTextual() && node.textValue().isEmpty());
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return "";
        return node.isValueNode() ? node.asText() : node.toString();
    }

    private static String textOrNull(JsonNode node) {
        String value = text(node).trim();
        return value.isEmpty() ? null : value;
    }

    private static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ");
    }

    private static String extractFact(String source, String label) {
        Matcher matcher = Pattern.compile(Pattern.quote(label) + "\\s*[:\\-]\\s*([^\\n\\r]+)",
                Pattern.CASE_INSENSITIVE).matcher(source);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static Double extractIncome(String source) {
        String labelled = extractFact(source, "Annual Income");
        String value = labelled;
        if (value == null) {
            Matcher matcher = Pattern.compile("(?:INR|Rs\\.?|₹)\\s*([0-9][0-9,]*)", Pattern.CASE_INSENSITIVE)
                    .matcher(source);
            if (matcher.find()) value = matcher.group(1);
        }
        return value == null ? null : number(value.replaceAll("[^0-9.-]", ""));
    }

    private static String extractDate(String source, String label) {
        String raw = extractFact(source, label);
        if (raw == null) return null;
        Matcher matcher = Pattern.compile("\\d{4}-\\d{2}-\\d{2}").matcher(raw);
        return matcher.find() ? matcher.group() : null;
    }

    private static LocalDate parseDate(String value) {
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static Double number(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        return number(text(node));
    }

    private static Double number(String value) {
        try {
            double parsed = Double.parseDouble(value.trim());
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static double numberOrDefault(JsonNode node, double fallback) {
        if (node.isMissingNode() || node.isNull()) return fallback;
        Double value = number(node);
        return value == null ? Double.NaN : value;
    }

    private static String display(String value) {
        return value == null || value.isBlank() ? "not found" : value;
    }

    private static String displayIncome(Double value) {
        return value == null ? "missing"
            : NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-IN")).format(value);
    }

    private static String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

}