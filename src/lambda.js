import { validateApplication } from './validator.js';

export const handler = async (event) => {
  try {
    const payload =
      typeof event.body === 'string'
        ? JSON.parse(event.body)
        : (event.body ?? event.packet ?? event);

    const report = validateApplication(payload);

    return {
      statusCode: 200,
      headers: {
        'content-type': 'application/json',
        'access-control-allow-origin': '*'
      },
      body: JSON.stringify(report)
    };
  } catch (error) {
    return {
      statusCode: 400,
      headers: {
        'content-type': 'application/json',
        'access-control-allow-origin': '*'
      },
      body: JSON.stringify({ error: error.message })
    };
  }
};