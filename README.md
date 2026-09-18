# SubmitReady

**SubmitReady** is a pre-submission quality gate for scholarship application packets.

The MVP focuses on one job: **find defects before the applicant submits**.

## What works now

- Required-field checks
- Required-document checks
- Applicant name consistency check
- Date-of-birth consistency check
- Income consistency check
- Certificate validity check
- File type/size checks
- A policy check backed by a Cedar policy file
- Actionable pass/fix evidence report
- Broken → fixed rerun demo
- Local HTTP server and AWS Lambda entrypoint
- SAM template for Ship It deployment

> The included domicile policy is deliberately synthetic for the hackathon demo. It is not an official scholarship rule.

## Run locally

```bash
npm test
npm start
```

Open `http://localhost:3000`.

You can also validate the fixtures directly:

```bash
npm run validate:demo
npm run validate:fixed
```

## AWS path

The current deployment skeleton uses **AWS SAM + Lambda + HTTP API**. For the full Ship It build, extend the adapter with:

- S3 for document storage
- Textract for document extraction
- DynamoDB for application/report state
- Step Functions for the multi-document verification workflow
- CloudWatch for audit/operational visibility
- Cognito if authenticated applicant/admin roles are added

The core validation logic remains deterministic. AI is optional and should only explain a machine-detected defect, not decide eligibility.

## Demo story

1. Load the broken packet.
2. Run preflight.
3. Show the exact name and income mismatches.
4. Load the fixed packet.
5. Run again.
6. End on `8/8 — READY TO SUBMIT`.

## Hackathon positioning

The hackathon asks for a real problem, visible real-world impact, meaningful AWS usage, working execution, and a three-minute recorded demo. The project is intentionally narrow so the complete workflow can be shown rather than described.
