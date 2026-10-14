# SubmitReady

SubmitReady checks scholarship application packets before submission and returns an evidence report for each check. The application uses Java 21 and Spring Boot; its browser UI and API are served by the same process.

The included domicile rule and application fixtures are synthetic demo data, not official scholarship requirements. This demo does not persist submissions. Do not submit real applicant data.

## Run locally

Requirements: JDK 21 or newer and Maven 3.9 or newer.

```bash
mvn test
mvn spring-boot:run
```

Open `http://localhost:8080`. Set `PORT` to override the local port.

## API

- `POST /api/validate` accepts an application packet as JSON and returns the eight-check report.
- `GET /api/demo/broken` and `GET /api/demo/fixed` return the sample packets.
- `GET /api/health` is the deployment health check.

## Deploy on Render free tier

The repository includes a Docker build and Render Blueprint. In Render, create a new Blueprint instance, connect this GitHub repository, and select the branch to deploy. Render builds and tests the container from `render.yaml`; the resulting web service is public over HTTPS. The free instance may spin down while idle and take a short time to wake on its next request.

The service is stateless and stores no submitted packet or report. Add authentication, durable storage, and a privacy review before using real applicant data.

## Demo

Open the home page to enter applicant details and document evidence. Select PDF or TXT files to check file type and size; the files stay in your browser. Paste the relevant document text into the evidence fields so SubmitReady can compare names, dates, and income. Use **Load ready sample** for an 8/8 result or **Load mismatch sample** to show fixable issues. Run `mvn test` for the integration tests.

The domicile rule is still a synthetic demo rule, not a real scholarship policy. This public demo is not approved for real applicant documents or personal data.
